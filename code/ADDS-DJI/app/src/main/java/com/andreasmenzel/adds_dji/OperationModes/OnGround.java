package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Managers.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode states that the drone is currently on the ground.
 *
 * The only available state in this mode is: "active".
 */
public class OnGround extends OperationMode {

    /**
     * Sets the state to "active".
     */
    public OnGround() {
        super();
        state = States.active;
    }


    @Override
    public void perform(@NonNull EventBus bus) {
        // Not flying? -> OnGround()

        Aircraft aircraft = DJIManager.getAircraftInstance();
        FlightController flightController;

        if(aircraft != null) {
            flightController = aircraft.getFlightController();

            if (flightController != null) {
                if(flightController.getState().isFlying()) {
                    DJIManager.changeOperationMode(new Hovering());
                }
            }
        }
    }


    @NonNull
    public String toString() {
        return "OnGround";
    }

}
