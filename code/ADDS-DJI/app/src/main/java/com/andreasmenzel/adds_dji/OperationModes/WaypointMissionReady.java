package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Managers.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.common.mission.waypoint.WaypointMissionState;

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


    @Override
    public void perform(@NonNull EventBus bus) {
        WaypointMissionState missionState = DJIManager.getWaypointMissionOperator().getCurrentState();

        if (WaypointMissionState.EXECUTING.equals(missionState)) {
            DJIManager.changeOperationMode(new WaypointMissionActive());
        } else if (WaypointMissionState.UNKNOWN.equals(missionState)
                || WaypointMissionState.DISCONNECTED.equals(missionState)
                || WaypointMissionState.RECOVERING.equals(missionState)
                || WaypointMissionState.READY_TO_UPLOAD.equals(missionState)
                || WaypointMissionState.NOT_SUPPORTED.equals(missionState)) {
            DJIManager.changeOperationMode(new None());
        }
    }


    @NonNull
    public String toString() {
        return "WaypointMissionReady";
    }

}
