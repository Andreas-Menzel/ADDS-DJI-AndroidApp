package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

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


    @NonNull
    public String toString() {
        return "None";
    }

}
