package com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes;

public class HighLevelOperationMode {

    public enum Modes {
        failed {
            @Override
            public String toString() {
                return "failed";
            }
        },

        start {
            @Override
            public String toString() {
                return "start";
            }
        },
        attempting {
            @Override
            public String toString() {
                return "attempting";
            }
        },
        inProgress {
            @Override
            public String toString() {
                return "inProgress";
            }
        },
        finished {
            @Override
            public String toString() {
                return "finished";
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
