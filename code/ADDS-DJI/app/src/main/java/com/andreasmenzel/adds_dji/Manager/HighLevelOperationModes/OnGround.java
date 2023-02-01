package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class OnGround extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public OnGround() {
        super();
        mode = Modes.finished; // This mode is always "finished"
    }

    // nextHightLevelOperationMode is not supported in OnGround


    public String toString() {
        return "OnGround";
    }

}
