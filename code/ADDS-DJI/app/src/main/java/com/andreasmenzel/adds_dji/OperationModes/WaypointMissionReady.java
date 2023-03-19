package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

/**
 * This Operation Mode indicates that a waypoint mission was uploaded to the drone and can now be
 * started. It is also possible that a waypoint mission is currently paused.
 *
 * The available states in this mode are: "active".
 */
public class WaypointMissionReady extends OperationMode {

    /**
     * Sets the initial state to "active".
     */
    public WaypointMissionReady() {
        super();
        state = States.active;
    }


    @NonNull
    public String toString() {
        return "WaypointMissionReady";
    }

}
