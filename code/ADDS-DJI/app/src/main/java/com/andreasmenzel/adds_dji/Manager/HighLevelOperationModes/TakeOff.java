package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class TakeOff extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public TakeOff() {
        super(new Hovering());
        mode = Modes.start;
    }
    public TakeOff(HighLevelOperationMode nextHightLevelOperationMode) {
        super(nextHightLevelOperationMode);
        mode = Modes.start;
    }


    public String toString() {
        return "TakeOff";
    }

}
