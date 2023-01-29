package com.andreasmenzel.adds_dji.Manager;

import android.os.Handler;

import androidx.annotation.NonNull;

// Traffic System Communication Events
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication.Communication;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication.GotTellResponse;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication.GotAskResponse;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication.InvalidTellResponse;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication.RequestFailed;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication.RequestSucceeded;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication.TellFailed;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication.AskFailed;

// Traffic System Connectivity Events - TODO: update and improve
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Connectivity.TrafficSystemConnected;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Connectivity.TrafficSystemConnectionCheckInProgeress;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Connectivity.TrafficSystemNotConnected;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Connectivity.TrafficSystemNowConnected;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Connectivity.TrafficSystemNowDisconnected;

import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;
import com.andreasmenzel.adds_dji.MApplication;
import com.andreasmenzel.adds_dji.MainActivity;

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

public class TrafficSystemManager {

    private final EventBus bus = EventBus.getDefault();

    /*
     * Manager
     */
    private DJIManager djiManager;

    /*
     * Information Holder
     */
    private AircraftLocation aircraftLocation;
    private AircraftPower aircraftPower;

    /*
     * Base information for Traffic System connection
     */
    private final OkHttpClient client = new OkHttpClient();
    private final String trafficSystemUrl = "http://adds-demo.an-men.de/";

    /*
     * Periodically check the connection to the Traffic System
     * (every <checkConnectionUntilConnectedDelay> seconds) until a connection can be established.
     */
    private final Handler connectivityCheckHandler = new Handler();
    private int connectivityCheckDelay = 1000;

    /*
     *
     */
    private LinkedList<String> tellsToSend = new LinkedList<>();
    private final ReentrantLock processingTellsToSend = new ReentrantLock();

    /*
     * Automatically send TELLs in specified intervals. Periodically check the connection
     * (/how_are_you) and server preferences, resend data on error and send data requested from
     * Traffic System.
     */
    private boolean autoCommunicationActive = false;

    // Auto communication: here_i_am
    private final Handler autoCommunicationHereIAmHandler = new Handler();
    private int autoCommunicationHereIAmDelay = 2000;

    // Auto communication: my_health
    private final Handler autoCommunicationMyHealthHandler = new Handler();
    private int autoCommunicationMyHealthDelay = 10_000;

    /*
     * Count the number of failed requests (TELL or ASK). "/how_are_you" requests are not considered.
     * Consider the device disconnected when the requests failed multiple times consecutively.
     */
    private int requestsFailedCounter = 0;
    private final int requestsFailedCounterMax = 3;

    /*
     * The Traffic System version. This is null if the Traffic System cannot be reached.
     */
    private String trafficSystemVersion = null;


    public TrafficSystemManager() {
        bus.register(this);

        djiManager = MApplication.getDjiManager();

        checkConnection();
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        bus.unregister(this);
    }


    public void checkConnection() {
        bus.post(new TrafficSystemConnectionCheckInProgeress());

        Request request = new Request.Builder()
                .url(trafficSystemUrl + "how_are_you")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if(trafficSystemVersion == null) {
                    // Was already not connected
                    bus.post(new TrafficSystemNotConnected());
                } else {
                    // Was connected earlier
                    trafficSystemVersion = null;
                    bus.post(new TrafficSystemNowDisconnected());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String myResponse = response.body().string();

                    try {
                        JSONObject json = new JSONObject(myResponse);
                        String version = json.getString("version");

                        if(version.equals(trafficSystemVersion)) {
                            bus.post(new TrafficSystemConnected());
                        } else {
                            trafficSystemVersion = version;
                            bus.post(new TrafficSystemNowConnected());
                        }
                    } catch (JSONException e) {
                        // TODO: invalid response
                        if(trafficSystemVersion == null) {
                            // Was already not connected
                            bus.post(new TrafficSystemNotConnected());
                        } else {
                            // Was connected earlier
                            trafficSystemVersion = null;
                            bus.post(new TrafficSystemNowDisconnected());
                        }
                    }
                } else {
                    if(trafficSystemVersion == null) {
                        // Was already not connected
                        bus.post(new TrafficSystemNotConnected());
                    } else {
                        // Was connected earlier
                        trafficSystemVersion = null;
                        bus.post(new TrafficSystemNowDisconnected());
                    }
                }
            }
        });
    }


    // example: requestGroup = tell && requestType = here_i_am
    private void sendAsynchronousRequest(String requestGroup, String requestType, String requestData) {
        Request request = new Request.Builder()
                .url(trafficSystemUrl + requestGroup + "/" + requestType + "?" + requestData)
                .build();

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


    @Subscribe
    public void nowConnected(TrafficSystemNowConnected event) {
        bus.post(new ToastMessage("Now conneced: startAutoCommunicationTell()"));
        startAutoCommunicationTell();
    }

    @Subscribe
    public void nowDisconnected(TrafficSystemNowDisconnected event) {
        trafficSystemVersion = null; // Just in case it is missing somewhere else.
        bus.post(new ToastMessage("Now disconneced: stopAutoCommunicationTell()"));
        stopAutoCommunicationTell();
    }

    @Subscribe
    public void notConnected(TrafficSystemNotConnected event) {
        if(event instanceof TrafficSystemNowDisconnected) {
            checkConnection();
        } else {
            connectivityCheckHandler.postDelayed(this::checkConnection, connectivityCheckDelay);
        }
    }


    @Subscribe
    public void requestFailed(RequestFailed event) {
        requestsFailedCounter++;
        if(requestsFailedCounter > requestsFailedCounterMax) {
            requestsFailedCounter = 0;

            trafficSystemVersion = null;
            bus.post(new TrafficSystemNowDisconnected());
        }
    }

    @Subscribe
    public void requestSucceeded(RequestSucceeded event) {
        requestsFailedCounter = 0;
    }


    /**********************************************************************************************/
    /********************************* AUTO COMMUNICATION: TELL  **********************************/
    /**********************************************************************************************/
    /*
     * Starts the automatic communication for all TELLs. This will send periodic TELL updates to the
     * Traffic System.
     */
    public void startAutoCommunicationTell() {
        autoCommunicationActive = true;

        addTellToSend("here_i_am");
        addTellToSend("my_health");
    }
    /*
     * Stop the automatic communication for TELLs.
     */
    public void stopAutoCommunicationTell() {
        autoCommunicationActive = false;

        autoCommunicationHereIAmHandler.removeCallbacksAndMessages(null);
        autoCommunicationMyHealthHandler.removeCallbacksAndMessages(null);
    }


    /*
     * Add a new TELL to the buffer list.
     */
    private void addTellToSend(String tell) {
        if(tell.equals("here_i_am")) {
            autoCommunicationHereIAmHandler.removeCallbacksAndMessages(null);
        } else if(tell.equals("my_health")) {
            autoCommunicationMyHealthHandler.removeCallbacksAndMessages(null);
        }

        processingTellsToSend.lock();

        try {
            // Only add if is new
            for(String tmp_tell : tellsToSend) {
                if(tmp_tell.equals(tell)) {
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

    /*
     * "Send" all TELLs currently in the buffer list.
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

    /*
     * Gather necessary information and send TELL request.
     */
    private void sendTell(@NonNull String tell) {
        String requestData = "";

        if(tell.equals("here_i_am")) {
            AircraftLocation aircraftLocation = djiManager.getAircraftLocation();

            requestData += "&drone_id=" + "testdrone1"; // TODO: get correct drone id
            requestData += "&gps_signal_level=" + aircraftLocation.getGpsSignalLevel();
            requestData += "&gps_satellites_connected=" + aircraftLocation.getGpsSatellitesConnected();
            requestData += "&gps_valid=" + aircraftLocation.getGpsValid();
            requestData += "&gps_lat=" + aircraftLocation.getGpsLat();
            requestData += "&gps_lon=" + aircraftLocation.getGpsLon();
            requestData += "&altitude=" + aircraftLocation.getAltitude();
            requestData += "&velocity_x=" + aircraftLocation.getVelocityX();
            requestData += "&velocity_Y=" + aircraftLocation.getVelocityY();
            requestData += "&velocity_Z=" + aircraftLocation.getVelocityZ();
            requestData += "&pitch=" + aircraftLocation.getPitch();
            requestData += "&yaw=" + aircraftLocation.getYaw();
            requestData += "&roll=" + aircraftLocation.getRoll();
        } if(tell.equals("my_health")) {
            AircraftPower aircraftPower = djiManager.getAircraftPower();

            requestData += "&drone_id=" + "dummy_drone"; // TODO: get correct drone id
            requestData += "&health=" + "ok"; // TODO: get correct value
            requestData += "&battery_remaining=" + aircraftPower.getBatteryRemaining();
            requestData += "&battery_remaining_percent=" + aircraftPower.getBatteryRemainingPercent();
            requestData += "&remaining_flight_time=" + aircraftPower.getRemainingFlightTime();
            requestData += "&remaining_flight_radius=" + aircraftPower.getRemainingFlightRadius();
        }

        // Remove first '&' character and send request
        if(requestData.length() > 0 && requestData.charAt(0) == '&') requestData = requestData.substring(1);
        sendAsynchronousRequest("tell", tell, requestData);
    }


    /*
     * Handle response from a TELL request.
     */
    @Subscribe()
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
                // TODO
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
            if(event.getTell().equals("here_i_am")) {
                autoCommunicationHereIAmHandler.postDelayed(() -> {
                    addTellToSend("here_i_am");
                }, autoCommunicationHereIAmDelay);
            } else if(event.getTell().equals("my_health")) {
                autoCommunicationMyHealthHandler.postDelayed(() -> {
                    addTellToSend("my_health");
                }, autoCommunicationMyHealthDelay);
            }
        }
    }

    /*
     * Handle failed TELL request.
     */
    @Subscribe
    public void tellFailed(TellFailed event) {
        // TODO
        // Check which tell was sent.
        // Log error.
        // Retry?

        // This currently ignores that the TELL failed.
        if(autoCommunicationActive) {
            if(event.getTell().equals("here_i_am")) {
                autoCommunicationHereIAmHandler.postDelayed(() -> {
                    addTellToSend("here_i_am");
                }, autoCommunicationHereIAmDelay);
            } else if(event.getTell().equals("my_health")) {
                autoCommunicationMyHealthHandler.postDelayed(() -> {
                    addTellToSend("my_health");
                }, autoCommunicationMyHealthDelay);
            }
        }
    }
    /**************************** END - automatic communication: tell *****************************/

    /**********************************************************************************************/
    /******************************* AUTOMATIC COMMUNICATION: ASK  ********************************/
    /**********************************************************************************************/
    // TODO
    /***************************** END - automatic communication: ask *****************************/





    /*
     * Getter methods
     */
    public String getTrafficSystemVersion() {
        return trafficSystemVersion;
    }

}
