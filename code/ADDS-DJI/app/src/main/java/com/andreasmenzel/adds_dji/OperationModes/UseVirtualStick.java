package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.restart;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.start;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Manager.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode uses the DJI Virtual Stick mode and sends virtual stick commands to the
 * drone.
 *
 * The available states in this mode are: "start" -> "attempting" -> "restart" | "failed".
 */
public class UseVirtualStick extends OperationMode {

    /**
     * Sets the initial state to "start".
     */
    public UseVirtualStick() {
        super();
        state = start;
    }


    /**
     * Tells the drone to use the virtual stick mode by sending FlightControlData.
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

                            flightController.sendVirtualStickFlightControlData(DJIManager.getVirtualStickFlightControlData(), djiError -> {
                                if(djiError == null) {
                                    setState(restart);
                                } else {
                                    bus.post(new ToastMessage("UseVirtualStick / start failed: send data failed! ..."));
                                    bus.post(new ToastMessage(djiError.getDescription()));
                                    attemptFailed();
                                }
                            });
                        } else {
                            // Virtual Stick is disabled. Restart.
                            DJIManager.changeOperationMode(new StartVirtualStick(this));
                        }
                    } else {
                        bus.post(new ToastMessage("UseVirtualStick / start failed: flightController is null!"));
                        attemptFailed();
                    }
                } else {
                    bus.post(new ToastMessage("UseVirtualStick / start failed: aircraft is null!"));
                    attemptFailed();
                }

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case restart:
                state = start;
                break;
        }
    }


    @NonNull
    public String toString() {
        return "UseVirtualStick";
    }

}
