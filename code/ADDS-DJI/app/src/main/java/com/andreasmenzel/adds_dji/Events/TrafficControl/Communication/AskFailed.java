package com.andreasmenzel.adds_dji.Events.TrafficControl.Communication;

import androidx.annotation.NonNull;

/**
 * Sending an ASK request to Traffic Control failed.
 */
public class AskFailed extends RequestFailed {

    private String ask;

    public AskFailed(@NonNull String ask) {
        this.ask = ask;
    }

    /**
     * Get the type of ASK request.
     *
     * @return The type of the ASK request.
     */
    public String getAsk() {
        return ask;
    }

}
