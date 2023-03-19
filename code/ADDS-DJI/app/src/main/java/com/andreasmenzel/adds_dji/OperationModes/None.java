package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Managers.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode is used if the current Operation Mode is not known. This mode tries to infer
 * the correct Operation Mode and change it accordingly.
 *
 * The only available state in this mode is: "active".
 */
public class None extends OperationMode {

    /**
     * Sets the state to "active".
     */
    public None() {
        super();
        state = States.active;
    }


    @Override
    public void perform(@NonNull EventBus bus) {
        // Flying? -> Hovering()
        // Not flying? -> OnGround()

        Aircraft aircraft = DJIManager.getAircraftInstance();
        FlightController flightController;

        if(aircraft != null) {
            flightController = aircraft.getFlightController();

            if (flightController != null) {
                if(flightController.getState().isFlying()) {
                    DJIManager.changeOperationMode(new Hovering());
                } else {
                    DJIManager.changeOperationMode(new OnGround());
                }
            }
        }
    }


    @NonNull
    public String toString() {
        return "None";
    }

}
