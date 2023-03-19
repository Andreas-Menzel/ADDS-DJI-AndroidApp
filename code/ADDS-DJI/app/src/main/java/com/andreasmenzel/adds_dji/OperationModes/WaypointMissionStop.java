package com.andreasmenzel.adds_dji.OperationModes;

import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.attempting;
import static com.andreasmenzel.adds_dji.OperationModes.OperationMode.States.finished;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Managers.DJIManager;

import org.greenrobot.eventbus.EventBus;

/**
 * This Operation Mode stops the currently running waypoint mission.
 *
 * The available states in this mode are: "start" -> "attempting" -> "finished" | "failed".
 */
public class WaypointMissionStop extends OperationMode {

    /**
     * Sets the initial state to "start" and the next Operation Mode to "None".
     */
    public WaypointMissionStop() {
        super(new None());
        state = States.start;
    }

    /**
     * Sets the initial state to "start" and sets the next Operation Mode.
     *
     * @param nextOperationMode The next Operation Mode.
     */
    public WaypointMissionStop(OperationMode nextOperationMode) {
        super(nextOperationMode);
        state = States.start;
    }


    /**
     * Stops the currently running waypoint mission.
     *
     * @param bus The event bus used to send events.
     */
    @Override
    public void perform(@NonNull EventBus bus)  {
        switch(state) {
            case start:
                setState(attempting);

                DJIManager.getWaypointMissionOperator().stopMission(djiError -> {
                    if(djiError == null) {
                        setState(finished);
                    } else {
                        bus.post(new ToastMessage("WaypointMissionStop / start failed: ..."));
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
        return "WaypointMissionStop";
    }

}
