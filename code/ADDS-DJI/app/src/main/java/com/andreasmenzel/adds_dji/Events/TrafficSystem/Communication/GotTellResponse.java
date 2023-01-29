package com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication;

import androidx.annotation.NonNull;

public class GotTellResponse extends Communication {

    private String tell;
    private String response;

    public GotTellResponse(@NonNull String tell, @NonNull String response) {
        this.tell = tell;
        this.response = response;
    }

    public String getTell() {
        return tell;
    }
    public String getResponse() {
        return response;
    }

}