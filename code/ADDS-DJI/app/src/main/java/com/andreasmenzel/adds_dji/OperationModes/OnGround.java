package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

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


    @NonNull
    public String toString() {
        return "OnGround";
    }

}
