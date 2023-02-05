package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class HighLevelOperationMode {

    public enum Modes {
        active { // If the high-level operation mode has no sub-modes, set it to active.
            @Override
            public String toString() {
                return "active";
            }
        },
        start { // The mode was just (re)started
            @Override
            public String toString() {
                return "start";
            }
        },
        attempting {
            @Override // The MSDK command was issued
            public String toString() {
                return "attempting";
            }
        },
        inProgress {
            @Override // The MSDK command was executed
            public String toString() {
                return "inProgress";
            }
        },
        finished {
            @Override // The action issued by the MSDK command was finished
            public String toString() {
                return "finished";
            }
        },
        restart {
            @Override // Instead of using finished to exit, restart can be used to restart this mode
            public String toString() {
                return "restart";
            }
        },
        failed {
            @Override // An error occurred (multiple times).
            public String toString() {
                return "failed";
            }
        }
    }
    Modes mode = Modes.failed;

    private int attempts = 0;
    int attemptsMax = 5;

    HighLevelOperationMode nextHighLevelOperationMode = null;
    Runnable runnableWhenFinished = null;
    Runnable runnableWhenFailed = null;


    public HighLevelOperationMode() {

    }
    public HighLevelOperationMode(HighLevelOperationMode nextHighLevelOperationMode) {
        this.nextHighLevelOperationMode = nextHighLevelOperationMode;
    }


    public void attemptFailed() {
        attempts++;

        if(attempts >= attemptsMax) {
            mode = Modes.failed;
        }
    }


    public String toString() {
        return "HighLevelFlightMode";
    }


    /*
     * Getter & setter methods
     */
    public Modes getMode() {
        return mode;
    }
    public void setMode(Modes mode) {
        this.mode = mode;
        attempts = 0;

        if(mode == Modes.finished) {
            if(runnableWhenFailed != null) {
                runnableWhenFinished.run();
            }
        } else if(mode == Modes.failed) {
            if(runnableWhenFailed != null) {
                runnableWhenFailed.run();
            }
        }
    }
    public HighLevelOperationMode getNextHighLevelOperationMode() {
        return nextHighLevelOperationMode;
    }
    public void setRunnableWhenFinished(Runnable runnableWhenFinished) {
        this.runnableWhenFinished = runnableWhenFinished;
    }
    public void setRunnableWhenFailed(Runnable runnableWhenFailed) {
        this.runnableWhenFailed = runnableWhenFailed;
    }

}
