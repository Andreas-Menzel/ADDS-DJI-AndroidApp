package com.andreasmenzel.adds_dji.Manager;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

// Events
import com.andreasmenzel.adds_dji.Events.DJIManager.UIUpdated;
import com.andreasmenzel.adds_dji.Events.InformationHolder.AircraftLocationChanged;
import com.andreasmenzel.adds_dji.Events.InformationHolder.AircraftPowerChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnected;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnectivityChange;
import com.andreasmenzel.adds_dji.Events.ProductModelChanged;

// Information Holder
import com.andreasmenzel.adds_dji.InformationHolder.AircraftHealth;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;

import com.andreasmenzel.adds_dji.InformationHolder.FlightMission;
import com.andreasmenzel.adds_dji.MainActivity;

// High-Level Operation Modes
import com.andreasmenzel.adds_dji.OperationModes.CancelLanding;
import com.andreasmenzel.adds_dji.OperationModes.CancelTakeOff;
import com.andreasmenzel.adds_dji.OperationModes.OperationMode;
import com.andreasmenzel.adds_dji.OperationModes.Landing;
import com.andreasmenzel.adds_dji.OperationModes.None;
import com.andreasmenzel.adds_dji.OperationModes.StopVirtualStick;
import com.andreasmenzel.adds_dji.OperationModes.TakeOff;
import com.andreasmenzel.adds_dji.OperationModes.TurnOffMotors;
import com.andreasmenzel.adds_dji.OperationModes.TurnOnMotors;
import com.andreasmenzel.adds_dji.OperationModes.StartVirtualStick;
import com.andreasmenzel.adds_dji.OperationModes.UseVirtualStick;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import dji.common.battery.BatteryState;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GoHomeAssessment;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Manager for the DJI product. This is the higher-level interface between the app and the DJI
 * product. Updates the information holders and provides methods to control the drone via high-level
 * operation modes.
 */
public class DJIManager {

    private static final String TAG = MainActivity.class.getName();

    private static final EventBus bus = EventBus.getDefault();

    private static final Handler controlDroneHandler = new Handler();

    private static String modelName = null;

    private static AircraftLocation aircraftLocation;
    private static AircraftPower aircraftPower;
    private static AircraftHealth aircraftHealth;
    private static FlightMission flightMission;

    private static OperationMode operationMode = new None();

    private static final FlightControlData virtualStickFlightControlData = new FlightControlData(0, 0, 0, 0);

    private static VideoFeeder.VideoDataListener videoDataListener = null;
    private static DJICodecManager codecManager = null;


    /**
     * Initializes the DJIManager: Registers to the event bus and gets the information holders.
     */
    public DJIManager() {
        bus.register(this);

        aircraftLocation = new AircraftLocation();
        aircraftPower = new AircraftPower();
        aircraftHealth = new AircraftHealth();
        flightMission = new FlightMission();


        //codecManager = new DJICodecManager(this, surface, width, height);

        // The callback for receiving the raw H264 video data for camera live view
        videoDataListener = (videoBuffer, size) -> {
            if(codecManager != null) {
                codecManager.sendDataToDecoder(videoBuffer, size);
            }
        };

        if(getProductInstance() != null && getProductInstance().isConnected()) {
            setupVideoDataListener();
        }
    }

    /**
     * Unregisters from the event bus.
     */
    @Override
    protected void finalize() {
        bus.unregister(this);
    }


    /**
     * This is executed when a DJI product was connected. Sets up the callback methods to retrieve
     * information about the drone.
     */
    @Subscribe
    public void productConnected(ProductConnected event) {
        // TODO: necessary? Isn't this already called in productChanged?
        setupCallbacks();
        setupVideoDataListener();
    }

    /**
     * This is executed when the DJI product was changed. Sets up the callback methods to retrieve
     * information about the drone IF a product is now connected.
     */
    @Subscribe
    public void productChanged(ProductChanged event) {
        if(getProductInstance() != null && getProductInstance().isConnected()) {
            setupCallbacks();
            setupVideoDataListener();
        }
    }

    private void setupVideoDataListener() {
        BaseProduct product = getProductInstance();

        if(product != null) {
            if(product.getModel() != null && !product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoDataListener);
            }
        }
    }

    /**
     * Starts the setup callback methods for the battery state and the flight controller state.
     */
    private void setupCallbacks() {
        setupBatteryStateCallback();
        setupFlightControllerStateCallback();
    }


    /**
     * Gets the instance of the specific product connected.
     */
    public static synchronized BaseProduct getProductInstance() {
        //return null;
        return DJISDKManager.getInstance().getProduct();
    }

    /**
     * Checks if an aircraft is currently connected (could also be a handheld device).
     *
     * @return true if an aircraft is connected, false otherwise.
     */
    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    /**
     * Gets the instance of the specific aircraft connected (if one is connected).
     *
     * @return The aircraft instance if one is connected, null otherwise.
     */
    public static synchronized Aircraft getAircraftInstance() {
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


    /**
     * Sets up the battery state callbacks. Makes sure that the SDK starts the
     * processUpdatedBatteryState(...) method when the battery state changes.
     */
    private void setupBatteryStateCallback() {
        BaseProduct product = getProductInstance();

        if(product != null) {
            Battery battery = getProductInstance().getBattery();

            try {
                battery.setStateCallback((@NonNull BatteryState batteryState) -> {
                    processUpdatedBatteryState(batteryState);
                });
            } catch (Exception ignored) {
                // TODO: Error - retry?
            }
        } else {
            // Could not setup
            // TODO: Error - retry?
        }

    }

    /**
     * This method is called whenever the battery state of the connected product changed. Executes
     * the appropriate method(s) that update the information holder(s).
     *
     * @param batteryState The new battery state.
     */
    private void processUpdatedBatteryState(@NonNull BatteryState batteryState) {
        // TODO: Make sure only running once

        processUpdatedBatteryStateUpdateAircraftPower(batteryState);
    }

    /**
     * Updates the AircraftPower from an updated flight battery state.
     *
     * @param batteryState The new battery state.
     */
    private void processUpdatedBatteryStateUpdateAircraftPower(@NonNull BatteryState batteryState) {
        aircraftPower.updateFromBatteryState(
                batteryState.getChargeRemaining(),
                batteryState.getChargeRemainingInPercent()
        );

        bus.post(new AircraftPowerChanged());
    }


    /**
     * Sets up the flight controller state callbacks. Makes sure that the SDK starts the
     * processUpdatedFlightControllerState(...) method when the battery state changes.
     */
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

    /**
     * This method is called whenever the flight controller state of the connected product changed.
     * Executes the appropriate methods that update the information holders.
     *
     * @param flightControllerState The new flight controller state.
     */
    private void processUpdatedFlightControllerState(@NonNull FlightControllerState flightControllerState) {
        // TODO: Make sure only running once

        processUpdatedFlightControllerStateUpdateAircraftLocation(flightControllerState);
        processUpdatedFlightControllerStateUpdateAircraftPower(flightControllerState);
    }

    /**
     * Updates the AircraftLocation from an updated flight controller state.
     *
     * @param flightControllerState The new flight controller state.
     */
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

                flightControllerState.getVelocityX(),
                flightControllerState.getVelocityY(),
                flightControllerState.getVelocityZ(),

                aircraftAttitude.pitch,
                aircraftAttitude.yaw,
                aircraftAttitude.roll
        );

        bus.post(new AircraftLocationChanged());
    }

    /**
     * Updates the AircraftPower from an updated flight controller state.
     *
     * @param flightControllerState The new flight controller state.
     */
    private void processUpdatedFlightControllerStateUpdateAircraftPower(@NonNull FlightControllerState flightControllerState) {
        GoHomeAssessment goHomeAssessment = flightControllerState.getGoHomeAssessment();

        aircraftPower.updateFromFlightControllerState(
                goHomeAssessment.getRemainingFlightTime(),
                goHomeAssessment.getMaxRadiusAircraftCanFlyAndGoHome()
        );

        bus.post(new AircraftPowerChanged());
    }


    /**
     * Changes the high-level operation mode to TakeOff().
     */
    public void takeOff() {
        changeOperationMode(new TakeOff());
    }

    /**
     * Changes the high-level operation mode to Landing().
     */
    public void land() {
        changeOperationMode(new Landing());
    }

    /**
     * Changes the high-level operation mode to TakeOff(), then Landing().
     */
    public void takeOffLand() {
        changeOperationMode(new TakeOff(new Landing()));
    }

    public void setVirtualSticks(float pitch, float yaw, float roll, float verticalThrottle) {
        if(pitch < -1) pitch = -1;
        else if(pitch > 1) pitch = 1;

        if(yaw < -1) yaw = -1;
        else if(yaw > 1) yaw = 1;

        if(roll < -1) roll = -1;
        else if(roll > 1) roll = 1;

        if(verticalThrottle < -1) verticalThrottle = -1;
        else if(verticalThrottle > 1) verticalThrottle = 1;

        // Yes, that's correct. This is supposed to be switched
        // TODO: Why DJI?!?
        virtualStickFlightControlData.setPitch(roll);
        virtualStickFlightControlData.setYaw(yaw);
        virtualStickFlightControlData.setRoll(pitch);
        virtualStickFlightControlData.setVerticalThrottle(verticalThrottle);
    }

    // TODO: None mode
    public void cancel() {
        changeOperationMode(new None());
    }

    /**
     * Changes the high-level operation mode to StartVirtualStick(). The high-level operation mode
     * then automatically changes to UseVirtualStick() and the virtualStickFlightControlData is
     * sent to the SDK.
     */
    public void virtualStick() {
        changeOperationMode(new StartVirtualStick());
    }

    // TODO: THIS IS A DEMO
    public void virtualStickAddLeft() {
        float newValue = virtualStickFlightControlData.getRoll() - (float)0.1;
        if(newValue < -1) newValue = -1;

        virtualStickFlightControlData.setRoll(newValue);
    }
    // TODO: THIS IS A DEMO
    public void virtualStickAddRight() {
        float newValue = virtualStickFlightControlData.getRoll() + (float)0.1;
        if(newValue > 1) newValue = 1;

        virtualStickFlightControlData.setRoll(newValue);
    }


    /**
     * Changes the high-level operation mode. This also performs some checks and potential cleanup
     * depending on the mode currently active.
     *
     * @param newOperationMode The new high-level operation mode.
     */
    // TODO: Check the sub-modes
    public static void changeOperationMode(OperationMode newOperationMode) {
        OperationMode.States currentSubmode = operationMode.getState();

        if(currentSubmode == OperationMode.States.finished) {
            operationMode = newOperationMode;
        } else {
            if(operationMode instanceof TakeOff) {
                operationMode = new CancelTakeOff(newOperationMode);
            } else if(operationMode instanceof Landing) {
                operationMode = new CancelLanding(newOperationMode);
            } else if(operationMode instanceof CancelTakeOff) {
                // Cannot be interrupted. Create new CancelTakeOff object.
                operationMode = new CancelTakeOff(newOperationMode);
            } else if(operationMode instanceof CancelLanding) {
                // Cannot be interrupted. Create new CancelLanding object.
                operationMode = new CancelLanding(newOperationMode);
            } else if(operationMode instanceof TurnOnMotors) {
                // Cannot be interrupted. Create new TurnOnMotors object.
                operationMode = new TurnOnMotors(newOperationMode);
            } else if(operationMode instanceof TurnOffMotors) {
                // Cannot be interrupted. Create new TurnOffMotors object.
                operationMode = new TurnOffMotors(newOperationMode);
            }  else if(operationMode instanceof StartVirtualStick || operationMode instanceof UseVirtualStick) {
                resetVirtualStickFlightControlData();
                operationMode = new StopVirtualStick(newOperationMode);
            } else {
                // This mode does not have a restriction. Change mode immediately.
                operationMode = newOperationMode;
            }
        }
    }


    /**
     * Resets the virtual stick flight control data. This is done so that the drone doesn't act
     * unpredictable when starting the virtual stick mode and not updating the flight control data.
     */
    private static void resetVirtualStickFlightControlData() {
        virtualStickFlightControlData.setPitch(0);
        virtualStickFlightControlData.setYaw(0);
        virtualStickFlightControlData.setRoll(0);
        virtualStickFlightControlData.setVerticalThrottle(0);
    }


    /**
     * Starts the drone control loop.
     */
    public void controlDrone() {
        controlDrone(true);
    }

    /**
     * Controls the drone depending on the high-level operation mode currently active. This method
     * has to be executed periodically, at least a few times per second.
     *
     * @param active If true, continue the drone control loop. If false, stop the drone control
     *               loop.
     */
    public void controlDrone(boolean active) {
        controlDroneHandler.removeCallbacksAndMessages(null);
        if(!active) {
            // stop drone control
            return;
        }

        if(operationMode.getState() != OperationMode.States.failed) {
            operationMode.perform(bus);
        } else {
            // changeHighLevelOperationMode(new None()); ?
        }

        bus.post(new UIUpdated()); // TODO: update / make better
        controlDroneHandler.postDelayed(this::controlDrone, 50);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GETTERS AND SETTERS                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the AircraftLocation.
     *
     * @return The AircraftLocation.
     */
    public AircraftLocation getAircraftLocation() {
        return aircraftLocation;
    }

    /**
     * Returns the AircraftPower.
     *
     * @return The AircraftPower.
     */
    public AircraftPower getAircraftPower() {
        return aircraftPower;
    }

    /**
     * Returns the AircraftHealth.
     *
     * @return The AircraftHealth.
     */
    public AircraftHealth getAircraftHealth() {
        return aircraftHealth;
    }

    /**
     * Returns the FlightMission.
     *
     * @return The FlightMission.
     */
    public FlightMission getFlightMission() {
        return flightMission;
    }

    /**
     * Returns the high-level operation mode.
     *
     * @return The high-level operation mode.
     */
    public OperationMode getHighLevelOperationMode() {
        return operationMode;
    }

    /**
     * Returns the virtual stick flight control data.
     *
     * @return The virtual stick flight control data.
     */
    public static FlightControlData getVirtualStickFlightControlData() {
        return virtualStickFlightControlData;
    }

}
