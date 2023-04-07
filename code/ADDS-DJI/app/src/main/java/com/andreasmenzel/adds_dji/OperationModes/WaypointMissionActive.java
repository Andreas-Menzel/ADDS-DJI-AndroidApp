package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Managers.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.common.mission.waypoint.WaypointMissionState;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

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


    @Override
    public void perform(@NonNull EventBus bus) {
        WaypointMissionState missionState = DJIManager.getWaypointMissionOperator().getCurrentState();

        if (WaypointMissionState.EXECUTION_PAUSED.equals(missionState)
            || WaypointMissionState.READY_TO_EXECUTE.equals(missionState)) {
            DJIManager.changeOperationMode(new WaypointMissionReady());
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
        return "WaypointMissionActive";
    }

}
