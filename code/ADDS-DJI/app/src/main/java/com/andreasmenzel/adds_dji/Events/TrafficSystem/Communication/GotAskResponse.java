package com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication;

import androidx.annotation.NonNull;

public class GotAskResponse extends RequestSucceeded {

    private String ask;
    private String response;

    public GotAskResponse(@NonNull String ask, @NonNull String response) {
        this.ask = ask;
        this.response = response;
    }

    public String getAsk() {
        return ask;
    }
    public String getResponse() {
        return response;
    }

}
