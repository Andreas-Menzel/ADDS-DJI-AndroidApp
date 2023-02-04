package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class StopVirtualStick extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public StopVirtualStick() {
        super(new None());
        mode = Modes.start;
    }
    public StopVirtualStick(HighLevelOperationMode nextHighLevelOperationMode) {
        super(nextHighLevelOperationMode);
        mode = Modes.start;
    }


    public String toString() {
        return "StopVirtualStick";
    }

}
