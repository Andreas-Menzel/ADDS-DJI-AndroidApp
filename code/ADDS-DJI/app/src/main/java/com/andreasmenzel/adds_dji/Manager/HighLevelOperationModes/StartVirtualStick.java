package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class StartVirtualStick extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public StartVirtualStick() {
        super(new UseVirtualStick());
        mode = Modes.start;
    }
    public StartVirtualStick(HighLevelOperationMode nextHighLevelOperationMode) {
        super(nextHighLevelOperationMode);
        mode = Modes.start;
    }

    // nextHighLevelOperationMode is not supported in OnGround


    public String toString() {
        return "StartVirtualStick";
    }

}
