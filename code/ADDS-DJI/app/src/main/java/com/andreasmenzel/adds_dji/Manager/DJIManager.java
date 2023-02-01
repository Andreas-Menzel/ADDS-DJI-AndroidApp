package com.andreasmenzel.adds_dji.Manager;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.DJIManager.UIUpdated;
import com.andreasmenzel.adds_dji.Events.InformationHolder.AircraftLocationChanged;
import com.andreasmenzel.adds_dji.Events.InformationHolder.AircraftPowerChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnected;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnectivityChange;
import com.andreasmenzel.adds_dji.Events.ProductModelChanged;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;
import com.andreasmenzel.adds_dji.MainActivity;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.CancelLanding;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.CancelTakeOff;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.HighLevelOperationMode;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.Hovering;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.Landing;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.OnGround;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.TakeOff;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.TurnOffMotors;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.TurnOnMotors;

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

public class DJIManager {

    private static final String TAG = MainActivity.class.getName();

    private final EventBus bus = EventBus.getDefault();

    private final Handler controlDroneHandler = new Handler();

    private static String modelName = null;


    private static AircraftLocation aircraftLocation;
    private static AircraftPower aircraftPower;


    private static HighLevelOperationMode highLevelOperationMode = new OnGround();


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
        bus.post(new ToastMessage("productConnected(): Registered Callbacks"));
    }

    @Subscribe
    public void productChanged(ProductChanged event) {
        setupCallbacks();
        bus.post(new ToastMessage("productChanged(): Registered Callbacks"));
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
                // TODO: Error - retry?
            }
        } else {
            // Could not setup
            // TODO: Error - retry?
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

        bus.post(new AircraftPowerChanged());
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

                flightControllerState.getVelocityX(),
                flightControllerState.getVelocityY(),
                flightControllerState.getVelocityZ(),

                aircraftAttitude.pitch,
                aircraftAttitude.yaw,
                aircraftAttitude.roll
        );

        bus.post(new AircraftLocationChanged());
    }

    private void processUpdatedFlightControllerStateUpdateAircraftPower(@NonNull FlightControllerState flightControllerState) {
        GoHomeAssessment goHomeAssessment = flightControllerState.getGoHomeAssessment();

        aircraftPower.updateFromFlightControllerState(
                goHomeAssessment.getRemainingFlightTime(),
                goHomeAssessment.getMaxRadiusAircraftCanFlyAndGoHome()
        );

        bus.post(new AircraftPowerChanged());
    }


    public void takeOff() {
        changeHighLevelOperationMode(new TakeOff());
    }

    public void land() {
        changeHighLevelOperationMode(new Landing());
    }

    public void cancel() {
        changeHighLevelOperationMode(null); // None Mode
    }


    public void virtualStickModeState(boolean enable) {
        Aircraft aircraft = getAircraftInstance();
        if(aircraft != null) {
            FlightController flightController = aircraft.getFlightController();

            flightController.setVirtualStickModeEnabled(enable, djiError -> {
                if(djiError != null) {
                    bus.post(new ToastMessage(djiError.getDescription()));
                } else {
                    flightController.setVirtualStickAdvancedModeEnabled(true);
                    flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
                }
            });
            if(enable) {
                bus.post(new ToastMessage("VirtualStickMode enabled"));
            } else {
                bus.post(new ToastMessage("VirtualStickMode disabled"));
            }
        } else {
            bus.post(new ToastMessage("aircraft is none."));
        }
    }

    public void virtualStickRoll(float val) {
        Aircraft aircraft = getAircraftInstance();
        if(aircraft != null) {
            FlightController flightController = aircraft.getFlightController();

            boolean vsAvailable = flightController.isVirtualStickControlModeAvailable();
            if(vsAvailable) {
                flightController.sendVirtualStickFlightControlData(new FlightControlData(val, 0, 0, 0), djiError -> {
                    if(djiError != null) {
                        //bus.post(new ToastMessage(djiError.getDescription()));
                    } else {
                        //bus.post("StickRoll success");
                    }
                    //bus.post(new ToastMessage("Was there an error?"));
                });
            } else {
                //bus.post(new ToastMessage("VirtualStick not available."));
            }
        } else {
            //bus.post(new ToastMessage("aircraft is none."));
        }
    }


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
        } else {
            // Currently selected mode not recognized
            highLevelOperationMode = newHighLevelOperationMode;
        }
    }

    private void setNextHighLevelOperationModeFromFinished(HighLevelOperationMode nextHighLevelOperationMode) {
        if(nextHighLevelOperationMode != null) {
            highLevelOperationMode = nextHighLevelOperationMode;
        }
    }

    /*
     * This method has to be executed periodically at least a few times per second. This method
     * controls the drone.
     */
    public void controlDrone() {
        controlDroneHandler.removeCallbacksAndMessages(null);

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
        }

        bus.post(new UIUpdated()); // TODO: update / make better
        controlDroneHandler.postDelayed(this::controlDrone, 50);
    }

    // TODO: TakeOff when motors are already on?
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


    /*
     *  Getter & Setter methods
     */
    public AircraftLocation getAircraftLocation() {
        return aircraftLocation;
    }
    public AircraftPower getAircraftPower() {
        return aircraftPower;
    }
    public HighLevelOperationMode getHighLevelOperationMode() {
        return highLevelOperationMode;
    }

}
