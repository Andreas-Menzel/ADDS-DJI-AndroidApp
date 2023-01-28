package com.andreasmenzel.adds_dji.Manager;

import android.util.Log;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnected;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnectivityChange;
import com.andreasmenzel.adds_dji.Events.ProductModelChanged;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;
import com.andreasmenzel.adds_dji.MainActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import dji.common.battery.BatteryState;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GoHomeAssessment;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class DJIManager {

    private static final String TAG = MainActivity.class.getName();

    private final EventBus bus = EventBus.getDefault();

    private static String modelName = null;



    private static AircraftLocation aircraftLocation;
    private static AircraftPower aircraftPower;


    public DJIManager() {
        bus.register(this);

        aircraftLocation = new AircraftLocation();
        aircraftPower = new AircraftPower();
    }

    @Override
    protected void finalize() throws Throwable {
        bus.unregister(this);
    }


    @Subscribe
    public void productConnected(ProductConnected event) {
        setupCallbacks();
    }

    @Subscribe
    public void productChanged(ProductChanged event) {
        setupCallbacks();
    }

    private void setupCallbacks() {
        setupBatteryStateCallback();
        setupFlightControllerStateCallback();
    }


    /**
     * Gets instance of the specific product connected.
     */
    private static synchronized BaseProduct getProductInstance() {
        return DJISDKManager.getInstance().getProduct();
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    private static synchronized Aircraft getAircraftInstance() {
        Aircraft result = null;
        if (isAircraftConnected()) {
            result = (Aircraft) getProductInstance();
        }
        return result;
    }


    // TODO: MODIFY
    @Subscribe
    public void productConnectivityChanged(ProductConnectivityChange event) {
        updateModel();

        // Not connected? Set all variables to default
    }

    private void updateModel() {
        BaseProduct product = getProductInstance();


        if(product != null && product.getModel() != null) {
            modelName = product.getModel().getDisplayName();
        } else {
            modelName = null;
        }

        bus.post(new ProductModelChanged());
    }
    public String getModelName() {
        return modelName;
    }
    // END: TODO: MODIFY


    private void setupBatteryStateCallback() {
        BaseProduct product = getProductInstance();

        if(product != null) {
            Battery battery = getProductInstance().getBattery();

            try {
                battery.setStateCallback((@NonNull BatteryState batteryState) -> {
                    processUpdatedBatteryState(batteryState);
                });
            } catch (Exception ignored) {

            }
        } else {
            // Could not setup
        }

    }

    private void processUpdatedBatteryState(@NonNull BatteryState batteryState) {
        // TODO: Make sure only running once

        processUpdatedBatteryStateUpdateAircraftPower(batteryState);
    }

    private void processUpdatedBatteryStateUpdateAircraftPower(@NonNull BatteryState batteryState) {
        aircraftPower.updateFromBatteryState(
                batteryState.getChargeRemaining(),
                batteryState.getChargeRemainingInPercent()
        );
    }


    private void setupFlightControllerStateCallback() {
        BaseProduct product = getProductInstance();

        if(product != null) {
            FlightController flightController = ((Aircraft) product).getFlightController();

            if(flightController != null) {
                flightController.setStateCallback((@NonNull FlightControllerState flightControllerState) -> {
                    processUpdatedFlightControllerState(flightControllerState);
                });
            } else {
                // Could not setup
                Log.d(TAG, "flightController is none.");
            }
        } else {
            // Could not setup
            Log.d(TAG, "product is none.");
        }
    }

    private void processUpdatedFlightControllerState(@NonNull FlightControllerState flightControllerState) {
        // TODO: Make sure only running once

        processUpdatedFlightControllerStateUpdateAircraftLocation(flightControllerState);
        processUpdatedFlightControllerStateUpdateAircraftPower(flightControllerState);
    }

    private void processUpdatedFlightControllerStateUpdateAircraftLocation(@NonNull FlightControllerState flightControllerState) {
        LocationCoordinate3D djiAircraftLocation = flightControllerState.getAircraftLocation();
        Attitude aircraftAttitude = flightControllerState.getAttitude();

        int gpsSignalLevel = -1;
        switch (flightControllerState.getGPSSignalLevel()) {
            case NONE:
                // No GPS signal
                gpsSignalLevel = 0;
            case LEVEL_0:
                // Almost no GPS signal
                gpsSignalLevel = 1;
                break;
            case LEVEL_1:
                // Very weak GPS signal
                gpsSignalLevel = 2;
                break;
            case LEVEL_2:
                // Weak GPS signal
                gpsSignalLevel = 3;
                break;
            case LEVEL_3:
                // Good GPS signal
                gpsSignalLevel = 4;
                break;
            case LEVEL_4:
                // Very good GPS signal
                gpsSignalLevel = 5;
                break;
            case LEVEL_5:
                // Very strong GPS signal
                gpsSignalLevel = 6;
                break;
        }

        boolean gpsValid = gpsSignalLevel >= 4;

        aircraftLocation.updateData(
                gpsSignalLevel,
                flightControllerState.getSatelliteCount(),

                gpsValid,
                djiAircraftLocation.getLatitude(),
                djiAircraftLocation.getLongitude(),

                djiAircraftLocation.getAltitude(),

                aircraftAttitude.pitch,
                aircraftAttitude.yaw,
                aircraftAttitude.roll
        );
    }

    private void processUpdatedFlightControllerStateUpdateAircraftPower(@NonNull FlightControllerState flightControllerState) {
        GoHomeAssessment goHomeAssessment = flightControllerState.getGoHomeAssessment();

        aircraftPower.updateFromFlightControllerState(
                goHomeAssessment.getRemainingFlightTime(),
                goHomeAssessment.getMaxRadiusAircraftCanFlyAndGoHome()
        );
    }


    /*
     *  Getter & Setter methods
     */
    public AircraftLocation getAircraftLocation() {
        return aircraftLocation;
    }

}
