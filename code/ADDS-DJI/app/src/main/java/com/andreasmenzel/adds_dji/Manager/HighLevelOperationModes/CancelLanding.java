package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class CancelLanding extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public CancelLanding() {
        super();
        mode = Modes.start;
    }
    public CancelLanding(HighLevelOperationMode nextHighLevelOperationMode) {
        super(nextHighLevelOperationMode);
        mode = Modes.start;
    }


    public String toString() {
        return "CancelLanding";
    }

}
