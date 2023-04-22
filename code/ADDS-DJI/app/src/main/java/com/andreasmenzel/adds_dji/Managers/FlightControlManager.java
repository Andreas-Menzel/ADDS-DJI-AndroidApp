package com.andreasmenzel.adds_dji.Managers;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

// Traffic System Communication Events
import com.andreasmenzel.adds_dji.Datasets.Corridor;
import com.andreasmenzel.adds_dji.Datasets.Intersection;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.Communication;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.GotTellResponse;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.GotAskResponse;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.InvalidAskResponse;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.InvalidTellResponse;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.RequestFailed;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.RequestSucceeded;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.TellFailed;
import com.andreasmenzel.adds_dji.Events.FlightControl.Communication.AskFailed;

import com.andreasmenzel.adds_dji.Events.FlightControl.Connectivity.Connected;
import com.andreasmenzel.adds_dji.Events.FlightControl.Connectivity.ConnectionCheckInProgress;
import com.andreasmenzel.adds_dji.Events.FlightControl.Connectivity.NotConnected;
import com.andreasmenzel.adds_dji.Events.FlightControl.Connectivity.NowConnected;
import com.andreasmenzel.adds_dji.Events.FlightControl.Connectivity.NowDisconnected;

import com.andreasmenzel.adds_dji.InformationHolder.InformationHolder;
import com.andreasmenzel.adds_dji.InformationHolder.MissionData;
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
public class FlightControlManager {

    private static final EventBus bus = EventBus.getDefault();

    private final DJIManager djiManager;
    private final MissionManager missionManager;

    /*
     * Base information for Traffic Control connection
     */
    private final OkHttpClient client = new OkHttpClient();
    private final String flightControlUrl = "http://adds-demo.an-men.de:2000/";

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

    private final LinkedList<String> asksToSend = new LinkedList<>();
    private final ReentrantLock processingAsksToSend = new ReentrantLock();

    /*
     * Automatically send TELLs or ASKs in specified intervals. Periodically check the connection
     * (/how_are_you) and server preferences, resend data on error and send data requested from
     * Traffic Control.
     */
    private boolean autoCommunicationTellActive = false;
    private boolean autoCommunicationAskActive = false;

    // Auto communication: tell/aircraft_location
    private final Handler autoCommunicationHandlerAircraftLocation = new Handler();
    private final int autoCommunicationDelayAircraftLocation = 1000;

    // Auto communication: tell/aircraft_power
    private final Handler autoCommunicationHandlerAircraftPower = new Handler();
    private final int autoCommunicationDelayAircraftPower = 10000;

    // Auto communication: tell/flight_data
    private final Handler autoCommunicationHandlerFlightData = new Handler();
    private final int autoCommunicationDelayFlightData = 3000;

    // Auto communication: tell/mission_data
    private final Handler autoCommunicationHandlerMissionData = new Handler();
    private final int autoCommunicationDelayMissionData = 1000; // Note: Nothing will be sent if nothing has changed

    // Auto communication: ask/infrastructure (controls intersection_list and corridor_list)
    private final Handler autoCommunicationHandlerInfrastructure = new Handler();
    private final int autoCommunicationDelayInfrastructure = 3000;

    // Auto communication: ask/request_clearance
    private final Handler autoCommunicationHandlerRequestClearance = new Handler();
    private final int autoCommunicationDelayRequestClearance = 2000; // Note: Nothing will be sent if no clearance is required

    // Auto communication: ask/request_flightpath
    private final Handler autoCommunicationHandlerRequestFlightpath = new Handler();
    private final int autoCommunicationDelayRequestFlightpath = 3000; // Note: Nothing will be sent if no flightpath is required

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
    private String flightControlVersion = null;


    /**
     * Initializes the Traffic Control Manager: Registers to the event bus, gets the djiManager and
     * starts checking the connection to the Traffic Control.
     */
    public FlightControlManager() {
        bus.register(this);

        djiManager = MApplication.getDjiManager();
        missionManager = MApplication.getMissionManager();

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
                .url(flightControlUrl + "how_are_you")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(flightControlVersion == null) {
                    // Was already not connected
                    bus.post(new NotConnected());
                } else {
                    // Was connected earlier
                    flightControlVersion = null;
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

                        if(version.equals(flightControlVersion)) {
                            bus.post(new Connected());
                        } else {
                            flightControlVersion = version;
                            bus.post(new NowConnected());
                        }
                    } catch (JSONException e) {
                        // TODO: exception handling for invalid response
                        if(flightControlVersion == null) {
                            // Was already not connected
                            bus.post(new NotConnected());
                        } else {
                            // Was connected earlier
                            flightControlVersion = null;
                            bus.post(new NowDisconnected());
                        }
                    }
                } else {
                    if(flightControlVersion == null) {
                        // Was already not connected
                        bus.post(new NotConnected());
                    } else {
                        // Was connected earlier
                        flightControlVersion = null;
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
     *                     &quot;tell&quot; or &quot;infrastructure&quot; in &quot;ask&quot;.
     * @param payload      The payload as a JSON-string.
     */
    private void sendAsynchronousRequest(String requestGroup, String requestType, String payload) {
        Request request = new Request.Builder()
                .url(flightControlUrl + requestGroup + "/" + requestType + "?payload=" + payload)
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
     * Executes the startAutoCommunication{Tell|Ask}() methods. This method is executed whenever a
     * connection to the Traffic Control could be established.
     */
    @Subscribe
    public void nowConnected(NowConnected event) {
        startAutoCommunicationTell();
        startAutoCommunicationAsk();
    }

    /**
     * Executes the stopAutoCommunication{Tell|Ask}() methods. This method is executed whenever the
     * connection to the Traffic Control was lost.
     */
    @Subscribe
    public void nowDisconnected(NowDisconnected event) {
        flightControlVersion = null; // Just in case it is missing somewhere else.
        stopAutoCommunicationTell();
        stopAutoCommunicationAsk();
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

            flightControlVersion = null;
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
        autoCommunicationTellActive = true;

        addTellToSend("aircraft_location");
        addTellToSend("aircraft_power");
        addTellToSend("flight_data");
        addTellToSend("mission_data");
    }
    /**
     * Stops the automatic communication for TELLs.
     */
    public void stopAutoCommunicationTell() {
        autoCommunicationTellActive = false;

        autoCommunicationHandlerAircraftLocation.removeCallbacksAndMessages(null);
        autoCommunicationHandlerAircraftPower.removeCallbacksAndMessages(null);
        autoCommunicationHandlerFlightData.removeCallbacksAndMessages(null);
        autoCommunicationHandlerMissionData.removeCallbacksAndMessages(null);
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
        } else if(tell.equals("mission_data")) {
            autoCommunicationHandlerMissionData.removeCallbacksAndMessages(null);
        }

        // Do nothing and retry in 1 second when drone is not active
        if(!MApplication.getDroneActive()) {
            if(tell.equals("aircraft_location")) {
                autoCommunicationHandlerAircraftLocation.postDelayed(() -> {
                    addTellToSend("aircraft_location");
                }, 1000);
            } else if(tell.equals("aircraft_power")) {
                autoCommunicationHandlerAircraftPower.postDelayed(() -> {
                    addTellToSend("aircraft_power");
                }, 1000);
            } else if(tell.equals("flight_data")) {
                autoCommunicationHandlerFlightData.postDelayed(() -> {
                    addTellToSend("flight_data");
                }, 1000);
            } else if(tell.equals("mission_data")) {
                autoCommunicationHandlerMissionData.postDelayed(() -> {
                    addTellToSend("mission_data");
                }, 1000);
            }

            return;
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
            payload.put("drone_id", MApplication.getDroneId());
            payload.put("data_type", requestType);
            payload.put("time_sent", System.currentTimeMillis() / 1000.0);

            InformationHolder informationHolder = null;
            if(requestType.equals("aircraft_location")) {
                informationHolder = djiManager.getAircraftLocation();
            } else if(requestType.equals("aircraft_power")) {
                informationHolder = djiManager.getAircraftPower();
            } else if(requestType.equals("flight_data")) {
                informationHolder = djiManager.getFlightData();
            } else if(requestType.equals("mission_data")) {
                informationHolder = missionManager.getMissionData();
            }

            if(informationHolder != null) {
                if(informationHolder.getAndSetDataUpdatedSinceLastFlightControlUpdate()) {
                    payload.put("data", informationHolder.getDatasetAsSmallJsonObject());
                    sendAsynchronousRequest("tell", requestType, payload.toString());
                } else {
                    // Nothing changed since last update. Check again later.
                    if(requestType.equals("aircraft_location")) {
                        autoCommunicationHandlerAircraftLocation.postDelayed(() -> {
                            addTellToSend("aircraft_location");
                        }, 1000);
                    } else if(requestType.equals("aircraft_power")) {
                        autoCommunicationHandlerAircraftPower.postDelayed(() -> {
                            addTellToSend("aircraft_power");
                        }, 1000);
                    } else if(requestType.equals("flight_data")) {
                        autoCommunicationHandlerFlightData.postDelayed(() -> {
                            addTellToSend("flight_data");
                        }, 1000);
                    } else if(requestType.equals("mission_data")) {
                        autoCommunicationHandlerMissionData.postDelayed(() -> {
                            addTellToSend("mission_data");
                        }, 1000);
                    }
                }
            } else {
                // TODO: error handling
            }
        } catch (JSONException e) {
            // TODO: error handling
        }
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

        if(autoCommunicationTellActive) {
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
            } else if(event.getTell().equals("mission_data")) {
                autoCommunicationHandlerMissionData.postDelayed(() -> {
                    addTellToSend("mission_data");
                }, autoCommunicationDelayMissionData);
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
        if(autoCommunicationTellActive) {
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
            } else if(event.getTell().equals("mission_data")) {
                autoCommunicationHandlerMissionData.postDelayed(() -> {
                    addTellToSend("mission_data");
                }, autoCommunicationDelayMissionData);
            }
        }
    }
    /**************************** END - automatic communication: tell *****************************/

    /**********************************************************************************************/
    /******************************* AUTOMATIC COMMUNICATION: ASK  ********************************/
    /**********************************************************************************************/
    /**
     * Starts the automatic communication for all ASKs. This will send periodic ASK requests to the
     * Traffic System.
     */
    public void startAutoCommunicationAsk() {
        autoCommunicationAskActive = true;

        addAskToSend("infrastructure");
        addAskToSend("request_clearance");
        addAskToSend("request_flightpath");
    }
    /**
     * Stops the automatic communication for ASKs.
     */
    public void stopAutoCommunicationAsk() {
        autoCommunicationAskActive = false;

        autoCommunicationHandlerInfrastructure.removeCallbacksAndMessages(null);
        autoCommunicationHandlerRequestClearance.removeCallbacksAndMessages(null);
        autoCommunicationHandlerRequestFlightpath.removeCallbacksAndMessages(null);
    }


    /**
     * Adds a new ASK to the buffer list.
     */
    private void addAskToSend(String ask) {
        if(ask.equals("infrastructure")) {
            autoCommunicationHandlerInfrastructure.removeCallbacksAndMessages(null);
        } else if(ask.equals("request_clearance")) {
            autoCommunicationHandlerRequestClearance.removeCallbacksAndMessages(null);
        } else if(ask.equals("request_flightpath")) {
            autoCommunicationHandlerRequestFlightpath.removeCallbacksAndMessages(null);
        }

        // Do nothing and retry in 1 second when drone is not active
        if(!MApplication.getDroneActive()) {
            if(ask.equals("infrastructure")) {
                autoCommunicationHandlerInfrastructure.postDelayed(() -> {
                    addAskToSend("infrastructure");
                }, 1000);
            } else if(ask.equals("request_clearance")) {
                autoCommunicationHandlerRequestClearance.postDelayed(() -> {
                    addAskToSend("request_clearance");
                }, 1000);
            } else if(ask.equals("request_flightpath")) {
                autoCommunicationHandlerRequestFlightpath.postDelayed(() -> {
                    addAskToSend("request_flightpath");
                }, 1000);
            }

            return;
        }

        processingAsksToSend.lock();

        try {
            // Only add if is new
            for(String tmp_ask : asksToSend) {
                if(tmp_ask.equals(ask)) {
                    processingAsksToSend.unlock();
                    return;
                }
            }

            asksToSend.add(ask);

            processingAsksToSend.unlock();

            processAsksToSend();
        } finally {
            if(processingAsksToSend.isLocked()) {
                processingAsksToSend.unlock();
            }
        }
    }

    /**
     * Executes the sendAsk() method for all ASKs currently in the buffer list.
     */
    private void processAsksToSend() {
        processingAsksToSend.lock();

        try {
            while(!asksToSend.isEmpty()) {
                sendAsk(asksToSend.removeFirst());
            }
        } finally {
            processingAsksToSend.unlock();
        }
    }

    /**
     * Gathers the necessary information and sends the ASK request.
     */
    private void sendAsk(@NonNull String requestType) {
        if(requestType.equals("infrastructure")) {
            JSONObject payloadIntersectionList = new JSONObject();
            JSONObject payloadCorridorList = new JSONObject();

            try {
                payloadIntersectionList.put("intersection_id", "%");
                payloadIntersectionList.put("data_type", "intersection_list");

                payloadCorridorList.put("corridor_id", "%");
                payloadCorridorList.put("data_type", "corridor_list");
            } catch (JSONException e) {
                // TODO: error handling
            }

            sendAsynchronousRequest("ask", "intersection_list", payloadIntersectionList.toString());
            sendAsynchronousRequest("ask", "corridor_list", payloadCorridorList.toString());
        } else if(requestType.equals("request_clearance")) {
            LinkedList<Corridor> corridorsPending = missionManager.getMissionData().getCorridorsPending();

            if(!corridorsPending.isEmpty()) {

                Corridor corridor = corridorsPending.getFirst();
                String thisCorAId = corridor.getIntersectionAId();
                String thisCorBId = corridor.getIntersectionBId();

                String destIntersectionId = "";

                if(corridorsPending.size() >= 2) {
                    Corridor nextCor = corridorsPending.get(1);
                    String nextCorAId = nextCor.getIntersectionAId();
                    String nextCorBId = nextCor.getIntersectionBId();

                    if(thisCorAId.equals(nextCorAId) || thisCorAId.equals(nextCorBId)) {
                        destIntersectionId = thisCorAId;
                    } else if(thisCorBId.equals(nextCorAId) || thisCorBId.equals(nextCorBId)) {
                        destIntersectionId = thisCorBId;
                    }
                } else {
                    destIntersectionId = missionManager.getMissionData().getLastMissionIntersection().getId();
                }

                JSONObject payload = new JSONObject();
                try {
                    payload.put("drone_id", MApplication.getDroneId());
                    payload.put("data_type", requestType);
                    //payload.put("time_sent", System.currentTimeMillis() / 1000.0);

                    JSONObject payloadData = new JSONObject();
                    payloadData.put("corridor", corridor.getId());
                    payloadData.put("dest_intersection", destIntersectionId);
                    payload.put("data", payloadData);
                } catch (JSONException e) {
                    // TODO: error handling
                }

                sendAsynchronousRequest("ask", "request_clearance", payload.toString());
            } else {
                // Don't send. Try again later.
                autoCommunicationHandlerRequestClearance.postDelayed(() -> {
                    addAskToSend("request_clearance");
                }, 1000);
            }
        } else if(requestType.equals("request_flightpath")) {
            // Only send, if there is currently no mission
            boolean send = true;

            MissionData missionData = MApplication.getMissionManager().getMissionData();
            if(!missionData.getCorridorsPending().isEmpty()) send = false;
            if(!missionData.getCorridorsApproved().isEmpty()) send = false;
            if(!missionData.getCorridorsUploaded().isEmpty()) send = false;
            if(!missionData.getCorridorsFinished().isEmpty()) send = false;

            Intersection destinationIntersection = missionData.getDestinationIntersection();
            String destinationIntersectionId = null;
            if(destinationIntersection != null) {
                destinationIntersectionId = missionData.getDestinationIntersection().getId();
            } else {
                send = false;
            }

            if(send) {
                JSONObject payload = new JSONObject();

                try {
                    payload.put("drone_id", MApplication.getDroneId());
                    payload.put("data_type", requestType);
                    //payload.put("time_sent", System.currentTimeMillis() / 1000.0);

                    JSONObject payloadData = new JSONObject();

                    payloadData.put("dest_intersection", destinationIntersectionId);
                    payload.put("data", payloadData);
                } catch (JSONException e) {
                    // TODO: Error Handling
                }

                sendAsynchronousRequest("ask", "request_flightpath", payload.toString());
            } else {
                // Don't send. Try again later.
                autoCommunicationHandlerRequestFlightpath.postDelayed(() -> {
                    addAskToSend("request_flightpath");
                }, 1000);
            }
        }
    }


    /**
     * Handles the response from a ASK request.
     */
    @Subscribe
    public void gotAskResponse(@NonNull GotAskResponse event) {
        try {
            JSONObject json = new JSONObject(event.getResponse());

            // Extract values from response
            boolean executed = json.getBoolean("executed");
            JSONArray errors = json.getJSONArray("errors");
            JSONArray warnings = json.getJSONArray("warnings");
            JSONArray requestingValues = null;
            JSONObject responseData = null;

            if(json.has("requesting_values")) {
                requestingValues = json.getJSONArray("requesting_values");
            }
            if(json.has("response_data")) {
                responseData = json.getJSONObject("response_data");
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

            if(event.getAsk().equals("intersection_list") || event.getAsk().equals("corridor_list")) {
                InfrastructureManager infrastructureManager = MApplication.getInfrastructureManager();
                // For the first response the InfrastructureManager may not have been initialized yet
                if(infrastructureManager != null) {
                    if(event.getAsk().equals("intersection_list")) {
                        MApplication.getInfrastructureManager().updateIntersectionList(responseData);
                    } else if(event.getAsk().equals("corridor_list")) {
                        MApplication.getInfrastructureManager().updateCorridorList(responseData);
                    }
                }
            } else if(event.getAsk().equals("request_clearance")) {
                String clearedCorridorId = responseData.getString("corridor");
                String clearedDestIntersectionId = responseData.getString("dest_intersection");
                boolean clearedStatus = responseData.getBoolean("cleared");

                if(clearedStatus) {
                    MissionData missionData = MApplication.getMissionManager().getMissionData();
                    LinkedList<Corridor> corridorsPending = missionData.getCorridorsPending();
                    LinkedList<Corridor> corridorsApproved = missionData.getCorridorsApproved();

                    // TODO: Also check flying direction (dest_intersection)
                    if(corridorsPending.getFirst().getId().equals(clearedCorridorId)) {
                        corridorsApproved.addLast(corridorsPending.removeFirst());
                        missionData.dataUpdated();
                        bus.post(new ToastMessage("Got clearance for corridor \"" + clearedCorridorId + "\"."));
                    }
                } else {
                    // TODO: Maybe handle this? Show on screen? -> Not necessary
                }
            } else if(event.getAsk().equals("request_flightpath")) {
                if(responseData != null) {
                    InfrastructureManager infrastructureManager = MApplication.getInfrastructureManager();

                    // Clear current mission (Should be empty - just in case)
                    MissionData missionData = MApplication.getMissionManager().getMissionData();
                    missionData.getCorridorsPending().clear();
                    missionData.getCorridorsApproved().clear();
                    missionData.getCorridorsUploaded().clear();
                    missionData.getCorridorsFinished().clear();
                    missionData.setLastMissionIntersection(null);
                    missionData.setLastUploadedIntersection(null);
                    missionData.setStartIntersection(null);

                    String startIntersectionId = responseData.getString("start_intersection");
                    Intersection startIntersection = infrastructureManager.getIntersection(startIntersectionId);
                    missionManager.setStartIntersection(startIntersection);

                    JSONArray flightpath = responseData.getJSONArray("flightpath");
                    for(int i = 0; i < flightpath.length(); ++i) {
                        Corridor cor = infrastructureManager.getCorridor(flightpath.getString(i));
                        if(cor != null) {
                            missionManager.getMissionData().getCorridorsPending().addLast(cor);
                        } else {
                            // TODO: Error handling
                        }
                    }

                    missionManager.startMission();
                } else {
                    // TODO: Maybe handle this? Show on screen?
                }
            }

        } catch (JSONException e) {
            // Invalid response
            bus.post(new InvalidAskResponse(event.getAsk()));
        }

        if(autoCommunicationAskActive) {
            if(event.getAsk().equals("intersection_list")) {
                // Assume that the response for corridor_list comes more or less at the same time
                autoCommunicationHandlerInfrastructure.postDelayed(() -> {
                    addAskToSend("infrastructure");
                }, autoCommunicationDelayInfrastructure);
            } else if(event.getAsk().equals("request_clearance")) {
                autoCommunicationHandlerRequestClearance.postDelayed(() -> {
                    addAskToSend("request_clearance");
                }, autoCommunicationDelayRequestClearance);
            } else if(event.getAsk().equals("request_flightpath")) {
                autoCommunicationHandlerRequestFlightpath.postDelayed(() -> {
                    addAskToSend("request_flightpath");
                }, autoCommunicationDelayRequestFlightpath);
            }
        }
    }

    /**
     * Handles a failed ASK request.
     */
    @Subscribe
    public void askFailed(AskFailed event) {
        // TODO: error handling
        // Check which ask was sent.
        // Log error.
        // Retry?

        // This currently ignores that the ask failed.
        if(autoCommunicationAskActive) {
            if(event.getAsk().equals("intersection_list")) {
                // Assume that the response for corridor_list comes more or less at the same time
                autoCommunicationHandlerInfrastructure.postDelayed(() -> {
                    addAskToSend("infrastructure");
                }, autoCommunicationDelayInfrastructure);
            } else if(event.getAsk().equals("request_clearance")) {
                autoCommunicationHandlerRequestClearance.postDelayed(() -> {
                    addAskToSend("request_clearance");
                }, autoCommunicationDelayRequestClearance);
            } else if(event.getAsk().equals("request_flightpath")) {
                autoCommunicationHandlerRequestFlightpath.postDelayed(() -> {
                    addAskToSend("request_flightpath");
                }, autoCommunicationDelayRequestFlightpath);
            }
        }
    }
    /***************************** END - automatic communication: ask *****************************/


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GETTERS AND SETTERS                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets the FlightControl URL.
     *
     * @return The flightControlUrl.
     */
    public String getFlightControlUrl() {
        return flightControlUrl;
    }

    /**
     * Gets the Flight Control version.
     *
     * @return The Flight Control version.
     */
    public String getFlightControlVersion() {
        return flightControlVersion;
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
