package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class UseVirtualStick extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public UseVirtualStick() {
        super();
        mode = HighLevelOperationMode.Modes.start;
    }

    // nextHighLevelOperationMode is not supported in OnGround


    public String toString() {
        return "UseVirtualStick";
    }

}
