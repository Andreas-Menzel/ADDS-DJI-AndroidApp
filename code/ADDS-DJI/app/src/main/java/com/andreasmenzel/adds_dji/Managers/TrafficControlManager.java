package com.andreasmenzel.adds_dji.Managers;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

// Traffic System Communication Events
import com.andreasmenzel.adds_dji.Events.TrafficControl.Communication.Communication;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Communication.GotTellResponse;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Communication.GotAskResponse;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Communication.InvalidTellResponse;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Communication.RequestFailed;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Communication.RequestSucceeded;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Communication.TellFailed;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Communication.AskFailed;

import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.Connected;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.ConnectionCheckInProgress;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.NotConnected;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.NowConnected;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.NowDisconnected;

import com.andreasmenzel.adds_dji.InformationHolder.InformationHolder;
import com.andreasmenzel.adds_dji.MApplication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The Traffic Control Manager. This provides a high-level interface to send data to and request
 * data from Traffic Control. This manager handles connection failures, tries resending failed
 * requests and automatically sends data that the Traffic Control requests.
 */
public class TrafficControlManager {

    private static final EventBus bus = EventBus.getDefault();

    private final DJIManager djiManager;

    /*
     * Base information for Traffic Control connection
     */
    private final OkHttpClient client = new OkHttpClient();
    private final String trafficControlUrl = "http://adds-demo.an-men.de/";

    /*
     * Periodically check the connection to the Traffic Control
     * (every <checkConnectionUntilConnectedDelay> seconds) until a connection can be established.
     */
    private final Handler connectivityCheckHandler = new Handler();
    private final int connectivityCheckDelay = 1000;

    /*
     *
     */
    private final LinkedList<String> tellsToSend = new LinkedList<>();
    private final ReentrantLock processingTellsToSend = new ReentrantLock();

    /*
     * Automatically send TELLs in specified intervals. Periodically check the connection
     * (/how_are_you) and server preferences, resend data on error and send data requested from
     * Traffic Control.
     */
    private boolean autoCommunicationActive = false;

    // Auto communication: aircraft_location
    private final Handler autoCommunicationHandlerAircraftLocation = new Handler();
    private final int autoCommunicationDelayAircraftLocation = 1000;

    // Auto communication: aircraft_power
    private final Handler autoCommunicationHandlerAircraftPower = new Handler();
    private final int autoCommunicationDelayAircraftPower = 10000;

    // Auto communication: flight_data
    private final Handler autoCommunicationHandlerFlightData = new Handler();
    private final int autoCommunicationDelayFlightData = 3000;

    /*
     * Count the number of failed requests (TELL or ASK). "/how_are_you" requests are not considered.
     * Consider the device disconnected when the requests failed multiple times consecutively.
     */
    private int requestsFailedCounter = 0;
    private final int requestsFailedCounterMax = 3;
    private int totalRequestsFailed = 0;
    private int totalRequestsSucceeded = 0;

    /*
     * The Traffic System version. This is null if the Traffic System cannot be reached.
     */
    private String trafficControlVersion = null;


    /**
     * Initializes the Traffic Control Manager: Registers to the event bus, gets the djiManager and
     * starts checking the connection to the Traffic Control.
     */
    public TrafficControlManager() {
        bus.register(this);

        djiManager = MApplication.getDjiManager();

        checkConnection();
    }

    /**
     * Unregisters from the event bus.
     *
     * @throws Throwable if a Throwable was thrown.
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        bus.unregister(this);
    }


    /**
     * Checks the connection to Traffic Control by sending a /how_are_you and checking for the
     * version field in the (JSON) response.
     */
    public void checkConnection() {
        bus.post(new ConnectionCheckInProgress());

        Request request = new Request.Builder()
                .url(trafficControlUrl + "how_are_you")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(trafficControlVersion == null) {
                    // Was already not connected
                    bus.post(new NotConnected());
                } else {
                    // Was connected earlier
                    trafficControlVersion = null;
                    bus.post(new NowDisconnected());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String myResponse = response.body().string();

                    try {
                        JSONObject json = new JSONObject(myResponse);
                        String version = json.getString("version");

                        if(version.equals(trafficControlVersion)) {
                            bus.post(new Connected());
                        } else {
                            trafficControlVersion = version;
                            bus.post(new NowConnected());
                        }
                    } catch (JSONException e) {
                        // TODO: exception handling for invalid response
                        if(trafficControlVersion == null) {
                            // Was already not connected
                            bus.post(new NotConnected());
                        } else {
                            // Was connected earlier
                            trafficControlVersion = null;
                            bus.post(new NowDisconnected());
                        }
                    }
                } else {
                    if(trafficControlVersion == null) {
                        // Was already not connected
                        bus.post(new NotConnected());
                    } else {
                        // Was connected earlier
                        trafficControlVersion = null;
                        bus.post(new NowDisconnected());
                    }
                }
            }
        });
    }


    /**
     * Sends an asynchronous request to the Traffic Control.
     *
     * @param requestGroup The request group, e.g. &quot;tell&quot; or &quot;ask&quot;.
     * @param requestType  The request type, e.g. &quot;aircraft_location&quot; in request group
     *                     &quot;tell&quot;.
     * @param payload      The payload as a JSON-string.
     */
    private void sendAsynchronousRequest(String requestGroup, String requestType, String payload) {
        Request request = new Request.Builder()
                .url(trafficControlUrl + requestGroup + "/" + requestType + "?payload=" + payload)
                .build();
        // TODO: Encrypt data and send via POST

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Communication event = new Communication();
                if(requestGroup.equals("tell")) {
                    event = new TellFailed(requestType);
                } else if(requestGroup.equals("ask")) {
                    event = new AskFailed(requestType);
                }
                bus.post(event);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String myResponse = response.body().string();

                    Communication event = new Communication();
                    if(requestGroup.equals("tell")) {
                        event = new GotTellResponse(requestType, myResponse);
                    } else if(requestGroup.equals("ask")) {
                        event = new GotAskResponse(requestType, myResponse);
                    }
                    bus.post(event);
                } else {
                    Communication event = new Communication();
                    if(requestGroup.equals("tell")) {
                        event = new TellFailed(requestType);
                    } else if(requestGroup.equals("ask")) {
                        event = new AskFailed(requestType);
                    }
                    bus.post(event);
                }
            }
        });
    }


    /**
     * Executes the startAutoCommunicationTell() method. This method is executed whenever a
     * connection to the Traffic Control could be established.
     */
    @Subscribe
    public void nowConnected(NowConnected event) {
        startAutoCommunicationTell();
    }

    /**
     * Executes the stopAutoCommunicationTell() method. This method is executed whenever the
     * connection to the Traffic Control was lost.
     */
    @Subscribe
    public void nowDisconnected(NowDisconnected event) {
        trafficControlVersion = null; // Just in case it is missing somewhere else.
        stopAutoCommunicationTell();
    }

    /**
     * Periodically checks the connection to Traffic Control. This method is executed whenever
     * the NotConnected event was sent.
     */
    @Subscribe
    public void notConnected(NotConnected event) {
        if(event instanceof NowDisconnected) {
            checkConnection();
        } else {
            connectivityCheckHandler.postDelayed(this::checkConnection, connectivityCheckDelay);
        }
    }


    /**
     * Increments the totalRequestsFailed counter and creates the NowDisconnected() event if
     * multiple requests previously failed. This method is called whenever the RequestFailed event
     * was sent.
     */
    @Subscribe
    public void requestFailed(RequestFailed event) {
        totalRequestsFailed++;

        requestsFailedCounter++;
        if(requestsFailedCounter > requestsFailedCounterMax) {
            requestsFailedCounter = 0;

            trafficControlVersion = null;
            bus.post(new NowDisconnected());
        }
    }

    /**
     * Increments the totalRequestsSucceeded counter and resets the requestsFailed counter. This
     * method is executed whenever the RequestSucceeded event was sent.
     */
    @Subscribe
    public void requestSucceeded(RequestSucceeded event) {
        totalRequestsSucceeded++;

        requestsFailedCounter = 0;
    }


    /**********************************************************************************************/
    /********************************* AUTO COMMUNICATION: TELL  **********************************/
    /**********************************************************************************************/
    /**
     * Starts the automatic communication for all TELLs. This will send periodic TELL updates to the
     * Traffic System.
     */
    public void startAutoCommunicationTell() {
        autoCommunicationActive = true;

        addTellToSend("aircraft_location");
        addTellToSend("aircraft_power");
        addTellToSend("flight_data");
    }
    /**
     * Stops the automatic communication for TELLs.
     */
    public void stopAutoCommunicationTell() {
        autoCommunicationActive = false;

        autoCommunicationHandlerAircraftLocation.removeCallbacksAndMessages(null);
        autoCommunicationHandlerAircraftPower.removeCallbacksAndMessages(null);
        autoCommunicationHandlerFlightData.removeCallbacksAndMessages(null);
    }


    /**
     * Adds a new TELL to the buffer list.
     */
    private void addTellToSend(String tell) {
        if(tell.equals("aircraft_location")) {
            autoCommunicationHandlerAircraftLocation.removeCallbacksAndMessages(null);
        } else if(tell.equals("aircraft_power")) {
            autoCommunicationHandlerAircraftPower.removeCallbacksAndMessages(null);
        } else if(tell.equals("flight_data")) {
            autoCommunicationHandlerFlightData.removeCallbacksAndMessages(null);
        }

        processingTellsToSend.lock();

        try {
            // Only add if is new
            for(String tmp_tell : tellsToSend) {
                if(tmp_tell.equals(tell)) {
                    processingTellsToSend.unlock();
                    return;
                }
            }

            tellsToSend.add(tell);

            processingTellsToSend.unlock();

            processTellsToSend();
        } finally {
            if(processingTellsToSend.isLocked()) {
                processingTellsToSend.unlock();
            }
        }
    }

    /**
     * Executes the sendTell() method for all TELLs currently in the buffer list.
     */
    private void processTellsToSend() {
        processingTellsToSend.lock();

        try {
            while(!tellsToSend.isEmpty()) {
                sendTell(tellsToSend.removeFirst());
            }
        } finally {
            processingTellsToSend.unlock();
        }
    }

    /**
     * Gathers the necessary information and sends the TELL request.
     */
    private void sendTell(@NonNull String requestType) {
        JSONObject payload = new JSONObject();

        try {
            payload.put("drone_id", "demo_drone"); // TODO: set correct drone id
            payload.put("data_type", requestType);

            InformationHolder informationHolder = null;
            if(requestType.equals("aircraft_location")) {
                informationHolder = djiManager.getAircraftLocation();
            } else if(requestType.equals("aircraft_power")) {
                informationHolder = djiManager.getAircraftPower();
            } else if(requestType.equals("flight_data")) {
                informationHolder = djiManager.getFlightData();
            }

            if(informationHolder != null) {
                Log.d("MY_DEBUG", informationHolder.getDatasetAsJsonString());
                Log.d("MY_DEBUG", informationHolder.getDatasetAsSmallJsonString());

                payload.put("data", informationHolder.getDatasetAsJsonObject()); // TODO-LATER: SmallJsonObject (?)
            } else {
                // TODO: error handling
            }
        } catch (JSONException e) {
            // TODO: error handling
        }

        sendAsynchronousRequest("tell", requestType, payload.toString());
    }


    /**
     * Handles the response from a TELL request.
     */
    @Subscribe
    public void gotTellResponse(@NonNull GotTellResponse event) {
        try {
            JSONObject json = new JSONObject(event.getResponse());

            // Extract values from response
            boolean executed = json.getBoolean("executed");
            JSONArray errors = json.getJSONArray("errors");
            JSONArray warnings = json.getJSONArray("warnings");
            JSONArray requestingValues = null;
            if(json.has("requesting_values")) {
                requestingValues = json.getJSONArray("requesting_values");
            }

            if(!executed) {
                // TODO: error handling
            }

            // TODO: Handle errors and warnings

            // Add requested values to tellsToSend
            if(requestingValues != null) {
                for(int i = 0; i < requestingValues.length(); i++) {
                    addTellToSend(requestingValues.getString(i));
                }
            }
        } catch (JSONException e) {
            // Invalid response
            bus.post(new InvalidTellResponse(event.getTell()));
        }

        if(autoCommunicationActive) {
            if(event.getTell().equals("aircraft_location")) {
                autoCommunicationHandlerAircraftLocation.postDelayed(() -> {
                    addTellToSend("aircraft_location");
                }, autoCommunicationDelayAircraftLocation);
            } else if(event.getTell().equals("aircraft_power")) {
                autoCommunicationHandlerAircraftPower.postDelayed(() -> {
                    addTellToSend("aircraft_power");
                }, autoCommunicationDelayAircraftPower);
            } else if(event.getTell().equals("flight_data")) {
                autoCommunicationHandlerFlightData.postDelayed(() -> {
                    addTellToSend("flight_data");
                }, autoCommunicationDelayFlightData);
            }
        }
    }

    /**
     * Handles a failed TELL request.
     */
    @Subscribe
    public void tellFailed(TellFailed event) {
        // TODO: error handling
        // Check which tell was sent.
        // Log error.
        // Retry?

        // This currently ignores that the TELL failed.
        if(autoCommunicationActive) {
            if(event.getTell().equals("aircraft_location")) {
                autoCommunicationHandlerAircraftLocation.postDelayed(() -> {
                    addTellToSend("aircraft_location");
                }, autoCommunicationDelayAircraftLocation);
            } else if(event.getTell().equals("aircraft_power")) {
                autoCommunicationHandlerAircraftPower.postDelayed(() -> {
                    addTellToSend("aircraft_power");
                }, autoCommunicationDelayAircraftPower);
            } else if(event.getTell().equals("flight_data")) {
                autoCommunicationHandlerFlightData.postDelayed(() -> {
                    addTellToSend("flight_data");
                }, autoCommunicationDelayFlightData);
            }
        }
    }
    /**************************** END - automatic communication: tell *****************************/

    /**********************************************************************************************/
    /******************************* AUTOMATIC COMMUNICATION: ASK  ********************************/
    /**********************************************************************************************/
    // TODO
    /***************************** END - automatic communication: ask *****************************/


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GETTERS AND SETTERS                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets the Traffic Control version.
     *
     * @return The Traffic Control version.
     */
    public String getTrafficControlVersion() {
        return trafficControlVersion;
    }

    /**
     * Gets the total requests failed counter.
     *
     * @return The total number of requests failed.
     */
    public int getTotalRequestsFailed() {
        return totalRequestsFailed;
    }

    /**
     * Gets the total requests succeeded counter.
     *
     * @return The total number of requests succeeded.
     */
    public int getTotalRequestsSucceeded() {
        return totalRequestsSucceeded;
    }

}
