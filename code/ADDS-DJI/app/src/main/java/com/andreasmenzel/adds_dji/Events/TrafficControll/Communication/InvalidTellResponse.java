package com.andreasmenzel.adds_dji.Events.TrafficControll.Communication;

public class InvalidTellResponse extends Communication {

    private String tell;

    public InvalidTellResponse(String tell) {
        this.tell = tell;
    }

    public String getTell() {
        return tell;
    }
}
