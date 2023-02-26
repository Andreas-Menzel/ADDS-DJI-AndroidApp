package com.andreasmenzel.adds_dji.Manager;

import android.os.Handler;

import androidx.annotation.NonNull;

// Traffic System Communication Events
import com.andreasmenzel.adds_dji.Events.DJIManager.CreatedManagers;
import com.andreasmenzel.adds_dji.Events.InformationHolder.BlackboxDatasetChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnectivityChange;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
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

import com.andreasmenzel.adds_dji.InformationHolder.AircraftHealth;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;
import com.andreasmenzel.adds_dji.InformationHolder.BlackboxDataset;
import com.andreasmenzel.adds_dji.InformationHolder.FlightMission;
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
 * The Blackbox Manager.
 */
public class BlackboxManager {

    private static final EventBus bus = EventBus.getDefault();

    private final Handler dataRecordingHandler = new Handler();
    private boolean dataRecordingActive = false;
    private int dataRecordingDelay = 1000;

    private BlackboxDataset blackboxDataset = new BlackboxDataset();

    /*
     * Manager
     */
    private DJIManager djiManager;

    private int recordedDatasetsCounter = 0;


    /**
     * Initializes the Blackbox Manager: Registers to the event bus and gets the djiManager.
     */
    public BlackboxManager() {
        bus.register(this);

        djiManager = MApplication.getDjiManager();
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


    @Subscribe
    public void createdManagers(CreatedManagers event) {
        djiManager = MApplication.getDjiManager();
    }


    /**
     *
     */
    @Subscribe
    public void productConnectivityChanged(ProductConnectivityChange event) {
        if(djiManager.getModelName() != null) {
            // Start recording data
            dataRecordingActive = true;
            dataRecordingHandler.removeCallbacksAndMessages(null);
            recordData();

            bus.post(new ToastMessage("Blackbox data recording enabled."));
        } else {
            // Stop recording data
            dataRecordingActive = false;
            dataRecordingHandler.removeCallbacksAndMessages(null);

            bus.post(new ToastMessage("Blackbox data recording disabled."));
        }
    }


    private void recordData() {
        if(dataRecordingActive) {
            // Get data
            AircraftLocation aircraftLocation = djiManager.getAircraftLocation();
            AircraftPower aircraftPower = djiManager.getAircraftPower();
            AircraftHealth aircraftHealth = djiManager.getAircraftHealth();
            FlightMission flightMission = djiManager.getFlightMission();

            // Collect data
            blackboxDataset.setAircraftLocation(aircraftLocation);
            blackboxDataset.setAircraftPower(aircraftPower);
            blackboxDataset.setAircraftHealth(aircraftHealth);
            blackboxDataset.setFlightMission(flightMission);

            recordedDatasetsCounter++;

            bus.post(new BlackboxDatasetChanged());

            dataRecordingHandler.postDelayed(this::recordData, dataRecordingDelay);
        }
    }


    public BlackboxDataset getBlackboxDataset() {
        return blackboxDataset;
    }

    public int getRecordedDatasetsCounter() {
        return recordedDatasetsCounter;
    }

}
