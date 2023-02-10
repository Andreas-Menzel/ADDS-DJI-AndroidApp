package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

/**
 * This Operation Mode states that the drone is currently hovering and holding position.
 *
 * The only available state in this mode is: "active".
 */
public class Hovering extends OperationMode {

    /**
     * Sets the initial state to "active".
     */
    public Hovering() {
        super();
        state = States.active;
    }


    @NonNull
    public String toString() {
        return "Hovering";
    }

}
