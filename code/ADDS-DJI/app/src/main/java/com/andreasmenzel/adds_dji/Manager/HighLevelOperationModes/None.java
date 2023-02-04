package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class None extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public None() {
        super();
        mode = Modes.active;
    }

    // nextHighLevelOperationMode is not supported in OnGround


    public String toString() {
        return "None";
    }

}
