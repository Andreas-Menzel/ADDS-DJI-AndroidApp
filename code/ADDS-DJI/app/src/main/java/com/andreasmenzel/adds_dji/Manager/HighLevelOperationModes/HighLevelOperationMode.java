package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class HighLevelOperationMode {

    public enum Modes {
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

    HighLevelOperationMode nextHightLevelOperationMode = null;

    public HighLevelOperationMode() {

    }
    public HighLevelOperationMode(HighLevelOperationMode nextHightLevelOperationMode) {
        this.nextHightLevelOperationMode = nextHightLevelOperationMode;
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
    }
    public HighLevelOperationMode getNextHightLevelOperationMode() {
        return nextHightLevelOperationMode;
    }

}
