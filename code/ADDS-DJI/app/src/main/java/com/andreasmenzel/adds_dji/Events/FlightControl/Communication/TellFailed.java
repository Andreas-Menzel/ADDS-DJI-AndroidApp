package com.andreasmenzel.adds_dji.Events.FlightControl.Communication;

import androidx.annotation.NonNull;

public class TellFailed extends RequestFailed {

    private String tell;

    public TellFailed(@NonNull String tell) {
        this.tell = tell;
    }

    public String getTell() {
        return tell;
    }

}
