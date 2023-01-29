package com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication;

import androidx.annotation.NonNull;

public class AskFailed extends RequestFailed {

    private String ask;

    public AskFailed(@NonNull String ask) {
        this.ask = ask;
    }

    public String getAsk() {
        return ask;
    }

}
