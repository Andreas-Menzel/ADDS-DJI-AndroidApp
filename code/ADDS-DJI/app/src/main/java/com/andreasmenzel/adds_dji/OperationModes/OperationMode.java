package com.andreasmenzel.adds_dji.OperationModes;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

/**
 * The OperationMode describes the "low-level" action the drone is currently performing. This can be
 * compared to DJIs Flight Modes (e.g. "GPS", "AUTO_TAKEOFF", ...). The Operation Modes are higher
 * level than the DJI Flight Modes, however.
 */
public abstract class OperationMode {

    /**
     * The possible states the Operation Mode can be in. Note that not every state is a valid state
     * for every Operation Mode. Check the corresponding Operation Mode class for more details.
     */
    public enum States {
        active { // If the Operation Mode has no different states, the state will be set to active.
            @NonNull
            @Override
            public String toString() {
                return "active";
            }
        },
        start { // The OperationMode was just (re)started.
            @NonNull
            @Override
            public String toString() {
                return "start";
            }
        },
        attempting { // The DJI MSDK command was issued and is currently executed.
            @NonNull
            @Override
            public String toString() {
                return "attempting";
            }
        },
        inProgress { // The DJI MSDK command was executed and the drone is performing the
                     // task / mode.
            @NonNull
            @Override
            public String toString() {
                return "inProgress";
            }
        },
        finished { // The action issued by the MSDK command was finished (e.g. "takeoff complete").
            @NonNull
            @Override
            public String toString() {
                return "finished";
            }
        },
        restart { // Instead of using finished to exit, restart can be used to restart this
                  // Operation Mode.
            @NonNull
            @Override
            public String toString() {
                return "restart";
            }
        },
        failed { // An error occurred (multiple times).
            @NonNull
            @Override
            public String toString() {
                return "failed";
            }
        }
    }

    States state = States.failed; // Fallback

    private int attempts = 0;
    private int attemptsMax = 5;

    OperationMode nextOperationMode = null;

    /**
     * Runnable that will be executed when the state changes to "finished".
     */
    Runnable runnableWhenFinished = null;
    /**
     * Runnable that will be executed when the state changes to "failed".
     */
    Runnable runnableWhenFailed = null;


    public OperationMode() {
    }

    /**
     * Sets the next Operation Mode.
     * @param nextOperationMode
     */
    public OperationMode(OperationMode nextOperationMode) {
        this.nextOperationMode = nextOperationMode;
    }


    /**
     * Increases the attempts counter and sets the state to "failed" if it exceeds attemptsMax.
     */
    public void attemptFailed() {
        attempts++;

        if(attempts >= attemptsMax) {
            state = States.failed;
        }
    }


    /**
     * This function is called in the control loop of the DJIManager. This function is overwritten
     * in every Operation Mode and performs the appropriate action.
     *
     * @param bus The event bus used to send events.
     */
    public void perform(@NonNull EventBus bus) {

    }


    @NonNull
    public String toString() {
        return "HighLevelFlightMode";
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                    GETTERS AND SETTERS                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the state the Operation Mode is currently in.
     *
     * @return The state.
     */
    public States getState() {
        return state;
    }

    /**
     * Sets the state. This will also call the runnableWhen... if it was set previously.
     *
     * @param state The new state.
     */
    public void setState(States state) {
        this.state = state;
        attempts = 0;

        if(state == States.finished) {
            if(runnableWhenFailed != null) {
                runnableWhenFinished.run();
            }
        } else if(state == States.failed) {
            if(runnableWhenFailed != null) {
                runnableWhenFailed.run();
            }
        }
    }

    /**
     * Returns the next Operation Mode.
     *
     * @return The next Operation Mode.
     */
    public OperationMode getNextOperationMode() {
        return nextOperationMode;
    }

    /**
     * Sets the runnable that will be executed when the state changes to "finished".
     *
     * @param runnableWhenFinished The runnable.
     */
    public void setRunnableWhenFinished(Runnable runnableWhenFinished) {
        this.runnableWhenFinished = runnableWhenFinished;
    }

    /**
     * Sets the runnable that will be executed when the state changes to "failed".
     *
     * @param runnableWhenFailed The runnable.
     */
    public void setRunnableWhenFailed(Runnable runnableWhenFailed) {
        this.runnableWhenFailed = runnableWhenFailed;
    }

}
