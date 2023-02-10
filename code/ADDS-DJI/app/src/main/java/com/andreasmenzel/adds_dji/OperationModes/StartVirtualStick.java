package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.finished;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.inProgress;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Manager.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode starts the DJI Virtual Stick mode. Manual control is no longer available
 * through the DJI remote.
 *
 * The available states in this mode are: "start" -> "attempting" -> "inProgress" ->
 * "finished" | "failed".
 */
public class StartVirtualStick extends OperationMode {

    /**
     * Sets the initial state to "start" and the next Operation Mode to "UseVirtualStick".
     */
    public StartVirtualStick() {
        super(new UseVirtualStick());
        state = States.start;
    }

    /**
     * Sets the initial state to "start" and sets the next Operation Mode.
     *
     * @param nextOperationMode The next Operation Mode.
     */
    public StartVirtualStick(OperationMode nextOperationMode) {
        super(nextOperationMode);
        state = States.start;
    }


    /**
     * Tells the drone to start the virtual stick mode so the app can simulate a pilot.
     *
     * @param bus The event bus used to send events.
     */
    @Override
    public void perform(@NonNull EventBus bus)  {
        Aircraft aircraft;
        FlightController flightController;

        switch(state) {
            case start:
                aircraft = DJIManager.getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(!flightController.isVirtualStickControlModeAvailable()) {
                            setState(attempting);

                            flightController.setVirtualStickModeEnabled(true, djiError -> {
                                if(djiError == null) {
                                    flightController.setVirtualStickAdvancedModeEnabled(true);
                                    flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

                                    setState(inProgress);
                                } else {
                                    bus.post(new ToastMessage("StartVirtualStick / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    attemptFailed();
                                }
                            });
                        } else {
                            setState(finished);
                        }
                    } else {
                        bus.post(new ToastMessage("StartVirtualStick / start failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("StartVirtualStick / start failed: aircraft is null!"));
                    attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case inProgress:
                aircraft = DJIManager.getAircraftInstance();

                if(aircraft != null) {
                    flightController = aircraft.getFlightController();

                    if(flightController != null) {
                        if(flightController.isVirtualStickControlModeAvailable()) {
                            setState(finished);
                        }
                    } else {
                        bus.post(new ToastMessage("StartVirtualStick / inProgress failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("StartVirtualStick / inProgress failed: aircraft is null!"));
                    attemptFailed();
                }

                break;
            case finished:
                DJIManager.changeOperationMode(nextOperationMode);
                break;
        }
    }


    @NonNull
    public String toString() {
        return "StartVirtualStick";
    }

}
