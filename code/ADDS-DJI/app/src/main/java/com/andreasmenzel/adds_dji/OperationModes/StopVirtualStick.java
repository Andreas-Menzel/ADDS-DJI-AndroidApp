package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.finished;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.inProgress;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Manager.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode stops the DJI Virtual Stick mode. Manual control is now available again
 * through the DJI remote.
 *
 * The available states in this mode are: "start" -> "attempting" -> "inProgress" ->
 * "finished" | "failed".
 */
public class StopVirtualStick extends OperationMode {

    /**
     * Sets the initial state to "start" and the next Operation Mode to "None".
     */
    public StopVirtualStick() {
        super(new None());
        state = States.start;
    }

    /**
     * Sets the initial state to "start" and sets the next Operation Mode.
     *
     * @param nextOperationMode The next Operation Mode.
     */
    public StopVirtualStick(OperationMode nextOperationMode) {
        super(nextOperationMode);
        state = States.start;
    }


    /**
     * Tells the drone to stop the virtual stick mode.
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
                        if(flightController.isVirtualStickControlModeAvailable()) {
                            setState(attempting);

                            flightController.setVirtualStickModeEnabled(false, djiError -> {
                                if(djiError == null) {
                                    setState(inProgress);
                                } else {
                                    bus.post(new ToastMessage("StopVirtualStick / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    attemptFailed();
                                }
                            });
                        } else {
                            setState(finished);
                        }
                    } else {
                        bus.post(new ToastMessage("StopVirtualStick / start failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("StopVirtualStick / start failed: aircraft is null!"));
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
                        if(!flightController.isVirtualStickControlModeAvailable()) {
                            setState(finished);
                        }
                    } else {
                        bus.post(new ToastMessage("StopVirtualStick / inProgress failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("StopVirtualStick / inProgress failed: aircraft is null!"));
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
        return "StopVirtualStick";
    }

}
