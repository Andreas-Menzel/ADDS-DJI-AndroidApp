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
import com.andreasmenzel.adds_dji.Events.ToastMessage;

// Information Holder
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;

import com.andreasmenzel.adds_dji.MainActivity;

// High-Level Operation Modes
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.CancelLanding;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.CancelTakeOff;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.HighLevelOperationMode;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.Hovering;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.Landing;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.OnGround;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.TakeOff;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.TurnOffMotors;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.TurnOnMotors;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.StartVirtualStick;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.UseVirtualStick;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import dji.common.battery.BatteryState;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GoHomeAssessment;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
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

    private static HighLevelOperationMode highLevelOperationMode = new OnGround();

    private static final FlightControlData virtualStickFlightControlData = new FlightControlData(0, 0, 0, 0);


    /**
     * Initializes the DJIManager: Registers to the event bus and gets the information holders.
     */
    public DJIManager() {
        bus.register(this);

        aircraftLocation = new AircraftLocation();
        aircraftPower = new AircraftPower();
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
        setupCallbacks();
    }

    /**
     * This is executed when the DJI product was changed. Sets up the callback methods to retrieve
     * information about the drone IF a product is now connected.
     */
    @Subscribe
    public void productChanged(ProductChanged event) {
        if(getProductInstance().isConnected()) {
            setupCallbacks();
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
    private static synchronized BaseProduct getProductInstance() {
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
        changeHighLevelOperationMode(new TakeOff());
    }

    /**
     * Changes the high-level operation mode to Landing().
     */
    public void land() {
        changeHighLevelOperationMode(new Landing());
    }

    /**
     * Changes the high-level operation mode to TakeOff(), then Landing().
     */
    public void takeOffLand() {
        changeHighLevelOperationMode(new TakeOff(new Landing()));
    }

    // TODO: None mode
    public void cancel() {
        changeHighLevelOperationMode(null); // None Mode
    }

    /**
     * Changes the high-level operation mode to StartVirtualStick(). The high-level operation mode
     * then automatically changes to UseVirtualStick() and the virtualStickFlightControlData is
     * sent to the SDK.
     */
    public void virtualStick() {
        changeHighLevelOperationMode(new StartVirtualStick());
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
     * @param newHighLevelOperationMode The new high-level operation mode.
     */
    // TODO: Check the sub-modes
    public void changeHighLevelOperationMode(HighLevelOperationMode newHighLevelOperationMode) {
        // TODO: Check mode finished?
        if(highLevelOperationMode instanceof OnGround) {
            highLevelOperationMode = newHighLevelOperationMode;
        } if(highLevelOperationMode instanceof Hovering) {
            highLevelOperationMode = newHighLevelOperationMode;
        } else if(highLevelOperationMode instanceof TakeOff) {
            highLevelOperationMode = new CancelTakeOff(newHighLevelOperationMode);
        } else if(highLevelOperationMode instanceof Landing) {
            highLevelOperationMode = new CancelLanding(newHighLevelOperationMode);
        } else if(highLevelOperationMode instanceof CancelTakeOff) {
            // Cannot be interrupted. Create new CancelTakeOff object.
            highLevelOperationMode = new CancelTakeOff(newHighLevelOperationMode);
        } else if(highLevelOperationMode instanceof CancelLanding) {
            // Cannot be interrupted. Create new CancelLanding object.
            highLevelOperationMode = new CancelLanding(newHighLevelOperationMode);
        } else if(highLevelOperationMode instanceof TurnOnMotors) {
            // Cannot be interrupted. Create new TurnOnMotors object.
            highLevelOperationMode = new TurnOnMotors(newHighLevelOperationMode);
        } else if(highLevelOperationMode instanceof TurnOffMotors) {
            // Cannot be interrupted. Create new TurnOffMotors object.
            highLevelOperationMode = new TurnOffMotors(newHighLevelOperationMode);
        }  else if(highLevelOperationMode instanceof StartVirtualStick) {
            // Cannot be interrupted. Create new TurnOffMotors object.
            highLevelOperationMode = newHighLevelOperationMode;
        } else {
            // Currently selected mode not recognized
            highLevelOperationMode = newHighLevelOperationMode;
        }
    }


    // TODO: Necessary?
    // TODO: Comment
    private void setNextHighLevelOperationModeFromFinished(HighLevelOperationMode nextHighLevelOperationMode) {
        if(nextHighLevelOperationMode != null) {
            highLevelOperationMode = nextHighLevelOperationMode;
        }
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

        if(highLevelOperationMode instanceof OnGround) {
            // OnGround
        } else if(highLevelOperationMode instanceof Hovering) {
            // Hovering
        } else if(highLevelOperationMode instanceof TakeOff) {
            controlDroneTakeOff((TakeOff) highLevelOperationMode);
        } else if(highLevelOperationMode instanceof Landing) {
            controlDroneLanding((Landing) highLevelOperationMode);
        } else if(highLevelOperationMode instanceof CancelTakeOff) {
            controlDroneCancelTakeOff((CancelTakeOff) highLevelOperationMode);
        } else if(highLevelOperationMode instanceof CancelLanding) {
            controlDroneCancelLanding((CancelLanding) highLevelOperationMode);
        } else if(highLevelOperationMode instanceof TurnOnMotors) {
            controlDroneTurnOnMotors((TurnOnMotors) highLevelOperationMode);
        } else if(highLevelOperationMode instanceof TurnOffMotors) {
            controlDroneTurnOffMotors((TurnOffMotors) highLevelOperationMode);
        } else if(highLevelOperationMode instanceof StartVirtualStick) {
            controlDroneStartVirtualStick((StartVirtualStick) highLevelOperationMode);
        } else if(highLevelOperationMode instanceof UseVirtualStick) {
            controlDroneUseVirtualStick((UseVirtualStick) highLevelOperationMode);
        }

        bus.post(new UIUpdated()); // TODO: update / make better
        controlDroneHandler.postDelayed(this::controlDrone, 50);
    }

    // TODO: TakeOff when motors are already on?

    /**
     * Tells the drone to take off. This method is executed when the high-level operation mode is
     * TakeOff().
     *
     * @param operationMode The high-level operation mode.
     */
    private void controlDroneTakeOff(@NonNull TakeOff operationMode)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(operationMode.getMode()) {
            case start:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(!flightController.getState().isFlying()) {
                            // Check if motors are already on. Must be turned off first in order to
                            // start the AUTO_TAKEOFF mode.
                            if(flightController.getState().areMotorsOn()) {
                                highLevelOperationMode = new TurnOffMotors(highLevelOperationMode);
                                break; // Should be in start mode when retrying
                            }

                            operationMode.setMode(HighLevelOperationMode.Modes.attempting);

                            flightController.startTakeoff(djiError -> {
                                if(djiError == null) {
                                    operationMode.setMode(HighLevelOperationMode.Modes.inProgress);
                                } else {
                                    bus.post(new ToastMessage("TakeOff / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    operationMode.attemptFailed();
                                }
                            });
                        } else {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("TakeOff / start failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("TakeOff / start failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.getState().isFlying()
                                && !flightController.getState().getFlightMode()._equals(FlightMode.AUTO_TAKEOFF.value())) {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                        // The TakeOff still counts as succeeded when the user terminates the
                        // AUTO_TAKEOFF mode as long as the drone is flying.
                    } else {
                        bus.post(new ToastMessage("TakeOff / inProgress failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("TakeOff / inProgress failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case finished:
                setNextHighLevelOperationModeFromFinished(operationMode.getNextHightLevelOperationMode());
                break;
        }
    }

    /**
     * Tells the drone to land. This method is executed when the high-level operation mode is
     * Landing().
     *
     * @param operationMode The high-level operation mode.
     */
    private void controlDroneLanding(@NonNull Landing operationMode)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(operationMode.getMode()) {
            case start:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.getState().isFlying()) {
                            operationMode.setMode(HighLevelOperationMode.Modes.attempting);

                            flightController.startLanding(djiError -> {
                                if(djiError == null) {
                                    operationMode.setMode(HighLevelOperationMode.Modes.inProgress);
                                } else {
                                    bus.post(new ToastMessage("Landing / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    operationMode.attemptFailed();
                                }
                            });
                        } else {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("Landing / start failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("Landing / start failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(!flightController.getState().isFlying()
                                && !flightController.getState().getFlightMode()._equals(FlightMode.AUTO_LANDING.value())) {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        } else if(flightController.getState().isFlying()
                                && (!flightController.getState().getFlightMode()._equals(FlightMode.AUTO_LANDING.value())
                                && !flightController.getState().getFlightMode()._equals(FlightMode.CONFIRM_LANDING.value()))) {
                            // The drone is flying but not in AUTO_LANDING or CONFIRM_LANDING mode.
                            // This is executed if the landing procedure was terminated (e.g. by the user).

                            // TODO: Detect when this mode was interrupted by the user
                            //operationMode.setMode(HighLevelOperationMode.Modes.failed);
                        }
                    } else {
                        bus.post(new ToastMessage("Landing / inProgress failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("Landing / inProgress failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case finished:
                setNextHighLevelOperationModeFromFinished(operationMode.getNextHightLevelOperationMode());
                break;
        }
    }

    /**
     * Tells the drone to cancel the take off. This method is executed when the high-level operation
     * mode is CancelTakeOff().
     *
     * @param operationMode The high-level operation mode.
     */
    // TODO: Doesn't work(?)
    private void controlDroneCancelTakeOff(@NonNull CancelTakeOff operationMode)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(operationMode.getMode()) {
            case start:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.getState().getFlightMode()._equals(FlightMode.AUTO_TAKEOFF.value())) {
                            operationMode.setMode(HighLevelOperationMode.Modes.attempting);

                            flightController.cancelTakeoff(djiError -> {
                                if(djiError == null) {
                                    operationMode.setMode(HighLevelOperationMode.Modes.inProgress);
                                    bus.post(new ToastMessage("CancelTakeOff in progress"));
                                } else {
                                    bus.post(new ToastMessage("CancelTakeOff / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    operationMode.attemptFailed();
                                }
                            });
                        } else {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("CancelTakeOff / start failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("CancelTakeOff / start failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(!flightController.getState().getFlightMode()._equals(FlightMode.AUTO_TAKEOFF.value())) {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                        // The TakeOff still counts as succeeded when the user terminates the
                        // AUTO_TAKEOFF mode as long as the drone is flying.
                    } else {
                        bus.post(new ToastMessage("CancelTakeOff / inProgress failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("CancelTakeOff / inProgress failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case finished:
                setNextHighLevelOperationModeFromFinished(operationMode.getNextHightLevelOperationMode());
                break;
        }
    }

    /**
     * Tells the drone to cancel the landing. This method is executed when the high-level operation
     * mode is CancelLanding().
     *
     * @param operationMode The high-level operation mode.
     */
    private void controlDroneCancelLanding(@NonNull CancelLanding operationMode)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(operationMode.getMode()) {
            case start:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.getState().getFlightMode()._equals(FlightMode.AUTO_LANDING.value())
                            || flightController.getState().getFlightMode()._equals(FlightMode.CONFIRM_LANDING.value())) {
                            operationMode.setMode(HighLevelOperationMode.Modes.attempting);

                            flightController.cancelLanding(djiError -> {
                                if(djiError == null) {
                                    operationMode.setMode(HighLevelOperationMode.Modes.inProgress);
                                } else {
                                    bus.post(new ToastMessage("CancelLanding / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    operationMode.attemptFailed();
                                }
                            });
                        } else {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("CancelLanding / start failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("CancelLanding / start failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(!flightController.getState().getFlightMode()._equals(FlightMode.AUTO_LANDING.value())
                                && !flightController.getState().getFlightMode()._equals(FlightMode.CONFIRM_LANDING.value())) {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("CancelLanding / inProgress failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("CancelLanding / inProgress failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case finished:
                setNextHighLevelOperationModeFromFinished(operationMode.getNextHightLevelOperationMode());
                break;
        }
    }

    /**
     * Tells the drone to turn on the motors. This method is executed when the high-level operation
     * mode is TurnOnMotors().
     *
     * @param operationMode The high-level operation mode.
     */
    private void controlDroneTurnOnMotors(@NonNull TurnOnMotors operationMode)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(operationMode.getMode()) {
            case start:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(!flightController.getState().areMotorsOn()) {
                            operationMode.setMode(HighLevelOperationMode.Modes.attempting);

                            flightController.turnOnMotors(djiError -> {
                                if(djiError == null) {
                                    operationMode.setMode(HighLevelOperationMode.Modes.inProgress);
                                } else {
                                    bus.post(new ToastMessage("TurnOnMotors / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    operationMode.attemptFailed();
                                }
                            });
                        } else {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("TurnOnMotors / start failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("TurnOnMotors / start failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.getState().areMotorsOn()) {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("TurnOnMotors / inProgress failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("TurnOnMotors / inProgress failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case finished:
                setNextHighLevelOperationModeFromFinished(operationMode.getNextHightLevelOperationMode());
                break;
        }
    }

    /**
     * Tells the drone to turn off the motors. This method is executed when the high-level operation
     * mode is TurnOffMotors().
     *
     * @param operationMode The high-level operation mode.
     */
    private void controlDroneTurnOffMotors(@NonNull TurnOffMotors operationMode)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(operationMode.getMode()) {
            case start:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.getState().areMotorsOn()) {
                            operationMode.setMode(HighLevelOperationMode.Modes.attempting);

                            flightController.turnOffMotors(djiError -> {
                                if(djiError == null) {
                                    operationMode.setMode(HighLevelOperationMode.Modes.inProgress);
                                } else {
                                    bus.post(new ToastMessage("TurnOffMotors / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    operationMode.attemptFailed();
                                }
                            });
                        } else {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("TurnOffMotors / start failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("TurnOffMotors / start failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(!flightController.getState().areMotorsOn()) {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("TurnOffMotors / inProgress failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("TurnOffMotors / inProgress failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case finished:
                setNextHighLevelOperationModeFromFinished(operationMode.getNextHightLevelOperationMode());
                break;
        }
    }

    /**
     * Tells the drone to start the virtual stick mode so the app can simulate a pilot. This method
     * is executed when the high-level operation mode is StartVirtualStick().
     *
     * @param operationMode The high-level operation mode.
     */
    private void controlDroneStartVirtualStick(@NonNull StartVirtualStick operationMode)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(operationMode.getMode()) {
            case start:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(!flightController.isVirtualStickControlModeAvailable()) {
                            operationMode.setMode(HighLevelOperationMode.Modes.attempting);

                            flightController.setVirtualStickModeEnabled(true, djiError -> {
                                if(djiError == null) {
                                    flightController.setVirtualStickAdvancedModeEnabled(true);
                                    flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

                                    operationMode.setMode(HighLevelOperationMode.Modes.inProgress);
                                } else {
                                    bus.post(new ToastMessage("StartVirtualStick / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    operationMode.attemptFailed();
                                }
                            });
                        } else {
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("StartVirtualStick / start failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("StartVirtualStick / start failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.isVirtualStickControlModeAvailable()) { // TODO: Is this necessary?
                            operationMode.setMode(HighLevelOperationMode.Modes.finished);
                        }
                    } else {
                        bus.post(new ToastMessage("StartVirtualStick / inProgress failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("StartVirtualStick / inProgress failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case finished:
                setNextHighLevelOperationModeFromFinished(operationMode.getNextHightLevelOperationMode());
                break;
        }
    }

    /**
     * Tells the drone to use the virtual stick mode by sending FlightControlData. This method is
     * executed when the high-level operation mode is UseVirtualStick().
     *
     * @param operationMode The high-level operation mode.
     */
    private void controlDroneUseVirtualStick(@NonNull UseVirtualStick operationMode)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(operationMode.getMode()) {
            case start:
                aircraft = getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.isVirtualStickControlModeAvailable()) {
                            operationMode.setMode(HighLevelOperationMode.Modes.attempting);

                            flightController.sendVirtualStickFlightControlData(virtualStickFlightControlData, djiError -> {
                                if(djiError == null) {
                                    operationMode.setMode(HighLevelOperationMode.Modes.finished);
                                } else {
                                    bus.post(new ToastMessage("UseVirtualStick / start failed: send data failed! ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    operationMode.attemptFailed();
                                }
                            });
                        } else {
                            // Virtual Stick is disabled. Restart.
                            highLevelOperationMode = new StartVirtualStick(highLevelOperationMode);
                        }
                    } else {
                        bus.post(new ToastMessage("UseVirtualStick / start failed: flightController is null!"));
                        operationMode.attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("UseVirtualStick / start failed: aircraft is null!"));
                    operationMode.attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                // Not necessary / used here.
                break;
            case finished:
                // Finished sending command. Now send again.
                highLevelOperationMode.setMode(HighLevelOperationMode.Modes.start);
                break;
        }
    }


    /*
     *  Getter & Setter methods
     */

    /**
     * Gets the AircraftLocation.
     * @return The AircraftLocation.
     */
    public AircraftLocation getAircraftLocation() {
        return aircraftLocation;
    }
    /**
     * Gets the AircraftPower.
     * @return The AircraftPower.
     */
    public AircraftPower getAircraftPower() {
        return aircraftPower;
    }
    /**
     * Gets the high-level operation mode.
     * @return The high-level operation mode.
     */
    public HighLevelOperationMode getHighLevelOperationMode() {
        return highLevelOperationMode;
    }

}
