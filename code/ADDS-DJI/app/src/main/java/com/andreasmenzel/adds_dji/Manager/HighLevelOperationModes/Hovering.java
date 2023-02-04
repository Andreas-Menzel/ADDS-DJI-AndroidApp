package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class Hovering extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public Hovering() {
        super();
        mode = Modes.active;
    }

    // nextHighLevelOperationMode is not supported in Hovering


    public String toString() {
        return "Hovering";
    }

}
