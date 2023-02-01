package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class TurnOnMotors extends HighLevelOperationMode {

    // The modes are defined in the FlightMode class

    public TurnOnMotors() {
        super();
        mode = Modes.start;
    }
    public TurnOnMotors(HighLevelOperationMode nextHighLevelOperationMode) {
        super(nextHighLevelOperationMode);
        mode = Modes.start;
    }


    public String toString() {
        return "TurnOnMotors";
    }

}
