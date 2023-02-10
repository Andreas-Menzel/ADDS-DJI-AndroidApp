package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.finished;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.inProgress;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.start;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Manager.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.common.flightcontroller.FlightMode;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode starts the takeoff procedure.
 *
 * The available states in this mode are: "start" -> "attempting" -> "inProgress" ->
 * "finished" | "failed".
 */
public class TakeOff extends OperationMode {

    /**
     * Sets the initial state to "start" and the next Operation Mode to "Hovering".
     */
    public TakeOff() {
        super(new Hovering());
        state = start;
    }

    /**
     * Sets the initial state to "start" and sets the next Operation Mode.
     *
     * @param nextOperationMode The next Operation Mode.
     */
    public TakeOff(OperationMode nextOperationMode) {
        super(nextOperationMode);
        state = start;
    }


    /**
     * Tells the drone to take off.
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
                        if(!flightController.getState().isFlying()) {
                            // Check if motors are already on. Must be turned off first in order to
                            // start the AUTO_TAKEOFF mode.
                            if(flightController.getState().areMotorsOn()) {
                                DJIManager.changeOperationMode(new TurnOffMotors(this));
                                break; // Should be in start mode when retrying
                            }

                            setState(attempting);

                            flightController.startTakeoff(djiError -> {
                                if(djiError == null) {
                                    setState(inProgress);
                                } else {
                                    bus.post(new ToastMessage("TakeOff / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    attemptFailed();
                                }
                            });
                        } else {
                            setState(finished);
                        }
                    } else {
                        bus.post(new ToastMessage("TakeOff / start failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("TakeOff / start failed: aircraft is null!"));
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
                        if(flightController.getState().isFlying()
                                && !flightController.getState().getFlightMode()._equals(FlightMode.AUTO_TAKEOFF.value())) {
                            setState(finished);
                        }
                        // The TakeOff still counts as succeeded when the user terminates the
                        // AUTO_TAKEOFF mode as long as the drone is flying.
                    } else {
                        bus.post(new ToastMessage("TakeOff / inProgress failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("TakeOff / inProgress failed: aircraft is null!"));
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
        return "TakeOff";
    }

}
