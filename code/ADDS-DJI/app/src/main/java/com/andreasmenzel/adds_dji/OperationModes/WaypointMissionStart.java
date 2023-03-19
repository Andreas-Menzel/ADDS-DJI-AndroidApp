package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.finished;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.inProgress;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Managers.DJIManager;

import org.greenrobot.eventbus.EventBus;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

/**
 * This Operation Mode starts the currently uploaded waypoint mission.
 *
 * The available states in this mode are: "start" -> "attempting" -> "finished" | "failed".
 */
public class WaypointMissionStart extends OperationMode {

    /**
     * Sets the initial state to "start" and the next Operation Mode to "WaypointMissionActive".
     */
    public WaypointMissionStart() {
        super(new WaypointMissionActive());
        state = States.start;
    }


    /**
     * Starts the currently uploaded waypoint mission.
     *
     * @param bus The event bus used to send events.
     */
    @Override
    public void perform(@NonNull EventBus bus)  {
        switch(state) {
            case start:
                setState(attempting);

                DJIManager.getWaypointMissionOperator().startMission(djiError -> {
                    if(djiError == null) {
                        setState(finished);
                    } else {
                        bus.post(new ToastMessage("WaypointMissionStart / start failed: ..."));
                        bus.post(new ToastMessage(djiError.getDescription()));
                        attemptFailed();
                    }
                });

                break;
            case attempting:
                // Is currently attempting. Do nothing.
                break;
            case finished:
                DJIManager.changeOperationMode(nextOperationMode);
                break;
        }
    }


    @NonNull
    public String toString() {
        return "WaypointMissionStart";
    }

}
