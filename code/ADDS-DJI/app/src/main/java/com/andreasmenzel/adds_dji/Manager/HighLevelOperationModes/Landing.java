package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class Landing extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public Landing() {
        super(new OnGround());
        mode = Modes.start;
    }
    public Landing(HighLevelOperationMode nextHighLevelOperationMode) {
        super(nextHighLevelOperationMode);
        mode = Modes.start;
    }


    public String toString() {
        return "Landing";
    }

}
