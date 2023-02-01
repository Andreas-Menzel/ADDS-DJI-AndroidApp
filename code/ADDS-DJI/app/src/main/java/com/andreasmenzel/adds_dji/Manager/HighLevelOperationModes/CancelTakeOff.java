package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class CancelTakeOff extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public CancelTakeOff() {
        super();
        mode = Modes.start;
    }
    public CancelTakeOff(HighLevelOperationMode nextHighLevelOperationMode) {
        super(nextHighLevelOperationMode);
        mode = Modes.start;
    }


    public String toString() {
        return "CancelTakeOff";
    }

}
