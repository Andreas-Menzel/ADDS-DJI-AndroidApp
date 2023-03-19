package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

/**
 * This Operation Mode indicates that a waypoint mission is currently active.
 *
 * The available states in this mode are: "active".
 */
public class WaypointMissionActive extends OperationMode {

    /**
     * Sets the initial state to "active".
     */
    public WaypointMissionActive() {
        super();
        state = States.active;
    }


    @NonNull
    public String toString() {
        return "WaypointMissionActive";
    }

}
