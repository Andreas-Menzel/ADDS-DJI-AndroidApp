package com.andreasmenzel.adds_dji.Events.TrafficSystem.Communication;

import androidx.annotation.NonNull;

public class TellFailed extends Communication {

    private String tell;

    public TellFailed(@NonNull String tell) {
        this.tell = tell;
    }

    public String getTell() {
        return tell;
    }

}
