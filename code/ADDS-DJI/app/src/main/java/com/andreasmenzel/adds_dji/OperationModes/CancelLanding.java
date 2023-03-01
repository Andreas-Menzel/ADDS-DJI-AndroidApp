package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.finished;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.inProgress;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Managers.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.common.flightcontroller.FlightMode;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode cancels the landing.
 *
 * The available states in this mode are: "start" -> "attempting" -> "inProgress" ->
 * "finished" | "failed".
 */
public class CancelLanding extends OperationMode {

    /**
     * Sets the initial state to "start". The next Operation Mode is set to "None" by default.
     */
    public CancelLanding() {
        super(new None());
        state = States.start;
    }

    /**
     * Sets the initial state to "start" and sets the next Operation Mode.
     *
     * @param nextOperationMode The next Operation Mode.
     */
    public CancelLanding(OperationMode nextOperationMode) {
        super(nextOperationMode);
        state = States.start;
    }


    /**
     * Tells the drone to cancel the landing.
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
                        if(flightController.getState().getFlightMode()._equals(FlightMode.AUTO_LANDING.value())
                                || flightController.getState().getFlightMode()._equals(FlightMode.CONFIRM_LANDING.value())) {
                            setState(attempting);

                            flightController.cancelLanding(djiError -> {
                                if(djiError == null) {
                                    setState(inProgress);
                                } else {
                                    bus.post(new ToastMessage("CancelLanding / start failed: ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    attemptFailed();
                                }
                            });
                        } else {
                            setState(finished);
                        }
                    } else {
                        bus.post(new ToastMessage("CancelLanding / start failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("CancelLanding / start failed: aircraft is null!"));
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
                        if(!flightController.getState().getFlightMode()._equals(FlightMode.AUTO_LANDING.value())
                                && !flightController.getState().getFlightMode()._equals(FlightMode.CONFIRM_LANDING.value())) {
                            setState(finished);
                        }
                    } else {
                        bus.post(new ToastMessage("CancelLanding / inProgress failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("CancelLanding / inProgress failed: aircraft is null!"));
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
        return "CancelLanding";
    }

}
