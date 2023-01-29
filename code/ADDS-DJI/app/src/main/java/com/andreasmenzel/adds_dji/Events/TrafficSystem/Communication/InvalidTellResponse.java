package com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication;

public class InvalidTellResponse extends Communication {

    private String tell;

    public InvalidTellResponse(String tell) {
        this.tell = tell;
    }

    public String getTell() {
        return tell;
    }
}
