package com.andreasmenzel.adds_dji.InformationHolder;

public class AircraftPower {

    /*
     * Remaining battery energy in mAh.
     */
    private int batteryRemaining;
    private int batteryRemainingPercent;

    /*
     * Remaining flight time in seconds (+ DJI buffer).
     */
    private int remainingFlightTime;
    /*
     * Remaining flight radius in meters **from its home location**.
     */
    private float remainingFlightRadius;


    public AircraftPower() {
        this.batteryRemaining = 0;
        this.batteryRemainingPercent = 0;

        this.remainingFlightTime = 0;
        this.remainingFlightRadius = 0;
    }


    public void updateFromBatteryState(int batteryRemaining, int batteryRemainingPercent) {
        this.batteryRemaining = batteryRemaining;
        this.batteryRemainingPercent = batteryRemainingPercent;
    }

    public void updateFromFlightControllerState(int remainingFlightTime, float remainingFlightRadius) {
        this.remainingFlightTime = remainingFlightTime;
        this.remainingFlightRadius = remainingFlightRadius;
    }


    /*
     * Getter methods
     */
    public int getBatteryRemaining() {
        return batteryRemaining;
    }
    public int getBatteryRemainingPercent() {
        return batteryRemainingPercent;
    }
    public int getRemainingFlightTime() {
        return remainingFlightTime;
    }
    public float getRemainingFlightRadius() {
        return remainingFlightRadius;
    }

}