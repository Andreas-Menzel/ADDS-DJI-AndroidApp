package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.finished;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.inProgress;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Manager.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.common.flightcontroller.FlightMode;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode initiates a landing.
 *
 * The available states in this mode are: "start" -> "attempting" -> "inProgress" ->
 * "finished" | "failed".
 */
public class Landing extends OperationMode {

    /**
     * Sets the initial state to "start" and the next Operation Mode to "OnGround".
     */
    public Landing() {
        super(new OnGround());
        state = States.start;
    }

    /**
     * Sets the initial state to "start" and sets the next Operation Mode.
     *
     * @param nextOperationMode The next Operation Mode.
     */
    public Landing(OperationMode nextOperationMode) {
        super(nextOperationMode);
        state = States.start;
    }


    /**
     * Tells the drone to land. This method is executed when the high-level operation mode is
     * Landing().
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
                        if(flightController.getState().isFlying()) {
                            setState(attempting);

                            flightController.startLanding(djiError -> {
                                if(djiError == null) {
                                    setState(inProgress);
                                } else {
                                    bus.post(new ToastMessage("Landing / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    attemptFailed();
                                }
                            });
                        } else {
                            setState(finished);
                        }
                    } else {
                        bus.post(new ToastMessage("Landing / start failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("Landing / start failed: aircraft is null!"));
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
                        if(!flightController.getState().isFlying()
                                && !flightController.getState().getFlightMode()._equals(FlightMode.AUTO_LANDING.value())) {
                            setState(finished);
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
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("Landing / inProgress failed: aircraft is null!"));
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
        return "Landing";
    }

}
