package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.failed;
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
 * This Operation Mode uploads the generated waypoint mission to the drone.
 *
 * The available states in this mode are: "start" -> "attempting" -> "finished" | "failed".
 */
public class WaypointMissionUpload extends OperationMode {

    /**
     * Sets the initial state to "start" and the next Operation Mode to "None".
     */
    public WaypointMissionUpload() {
        super(new WaypointMissionStart());
        state = States.start;
    }

    /**
     * Sets the initial state to "start" and sets the next Operation Mode.
     *
     * @param nextOperationMode The next Operation Mode.
     */
    public WaypointMissionUpload(OperationMode nextOperationMode) {
        super(nextOperationMode);
        state = States.start;
    }


    /**
     * Gets the generated waypoint mission and uploads it to the drone.
     *
     * @param bus The event bus used to send events.
     */
    @Override
    public void perform(@NonNull EventBus bus)  {
        switch(state) {
            case start:
                DJIError djiErrorBuild = DJIManager.getWaypointMissionOperator().loadMission(DJIManager.getWaypointMissionBuilder().build());
                if(djiErrorBuild != null) {
                    bus.post(new ToastMessage("WaypointMissionUpload / start / build failed: ..."));
                    bus.post(new ToastMessage(djiErrorBuild.getDescription()));
                    attemptFailed();
                    break;
                }

                setState(attempting);
                DJIManager.getWaypointMissionOperator().uploadMission(djiError -> {
                    if(djiError == null) {
                        setState(finished);
                    } else {
                        bus.post(new ToastMessage("WaypointMissionUpload / start / upload failed: ..."));
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
        return "WaypointMissionUpload";
    }

}
