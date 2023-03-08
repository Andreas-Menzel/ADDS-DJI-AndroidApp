package com.andreasmenzel.adds_dji.Managers;

import android.os.Handler;
import android.util.Log;

// Traffic System Communication Events
import com.andreasmenzel.adds_dji.Events.DJIManager.CreatedManagers;
import com.andreasmenzel.adds_dji.Events.InformationHolder.BlackboxDatasetChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnectivityChange;
import com.andreasmenzel.adds_dji.Events.ToastMessage;

import com.andreasmenzel.adds_dji.InformationHolder.AircraftHealth;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;
import com.andreasmenzel.adds_dji.InformationHolder.BlackboxDataset;
import com.andreasmenzel.adds_dji.InformationHolder.FlightData;
import com.andreasmenzel.adds_dji.MApplication;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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
            FlightData flightData = djiManager.getFlightData();

            // Collect data
            blackboxDataset.setAircraftLocation(aircraftLocation);
            blackboxDataset.setAircraftPower(aircraftPower);
            blackboxDataset.setAircraftHealth(aircraftHealth);
            blackboxDataset.setFlightMission(flightData);

            recordedDatasetsCounter++;

            bus.post(new BlackboxDatasetChanged());

            Log.d("MY_DEBUG", aircraftLocation.getDatasetAsJsonObject().toString());

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
