package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class TurnOffMotors extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public TurnOffMotors() {
        super();
        mode = Modes.start;
    }
    public TurnOffMotors(HighLevelOperationMode nextHighLevelOperationMode) {
        super(nextHighLevelOperationMode);
        mode = Modes.start;
    }


    public String toString() {
        return "TurnOffMotors";
    }

}
