package com.andreasmenzel.adds_dji.Events.FlightControl.Communication;

public class InvalidAskResponse extends Communication {

    private String ask;

    public InvalidAskResponse(String ask) {
        this.ask = ask;
    }

    public String getAsk() {
        return ask;
    }
}
