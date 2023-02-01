package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class Hovering extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public Hovering() {
        super();
        mode = HighLevelOperationMode.Modes.finished; // This mode is always "finished"
    }

    // nextHightLevelOperationMode is not supported in Hovering


    public String toString() {
        return "Hovering";
    }

}
