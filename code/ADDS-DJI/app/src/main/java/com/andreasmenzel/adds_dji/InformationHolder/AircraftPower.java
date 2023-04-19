package com.andreasmenzel.adds_dji.InformationHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class AircraftPower implements InformationHolder {

    private final AtomicBoolean dataUpdatedSinceLastFlightControlUpdate = new AtomicBoolean(true);

    private double timeRecorded;

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
        this.timeRecorded = 0;

        this.batteryRemaining = 0;
        this.batteryRemainingPercent = 0;

        this.remainingFlightTime = 0;
        this.remainingFlightRadius = 0;
    }


    public void updateFromBatteryState(int batteryRemaining, int batteryRemainingPercent) {
        this.batteryRemaining = batteryRemaining;
        this.batteryRemainingPercent = batteryRemainingPercent;

        dataUpdated();
    }

    public void updateFromFlightControllerState(int remainingFlightTime, float remainingFlightRadius) {
        this.remainingFlightTime = remainingFlightTime;
        this.remainingFlightRadius = remainingFlightRadius;

        dataUpdated();
    }


    public void dataUpdated() {
        this.timeRecorded = System.currentTimeMillis() / 1000.0;
        dataUpdatedSinceLastFlightControlUpdate.set(true);
    }
    public boolean getAndSetDataUpdatedSinceLastFlightControlUpdate() {
        return dataUpdatedSinceLastFlightControlUpdate.getAndSet(false);
    }


    /*
     * Getter methods
     */

    public double getTimeRecorded() {
        return timeRecorded;
    }
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


    /**
     * Returns a JSON-object containing the aircraft power data.
     *
     * @return The aircraft power data as a JSON-object.
     */
    @Override
    public JSONObject getDatasetAsJsonObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("time_recorded", timeRecorded);

            datasetAsJsonObject.put("battery_remaining", batteryRemaining);
            datasetAsJsonObject.put("battery_remaining_percent", batteryRemainingPercent);

            datasetAsJsonObject.put("remaining_flight_time", remainingFlightTime);
            datasetAsJsonObject.put("remaining_flight_radius", remainingFlightRadius);
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }

    /**
     * Returns a JSON-object containing the aircraft power data. This function removes extremely
     * unnecessary accuracy while still keeping the accuracy better than needed in this application.
     *
     * @return The aircraft power data as a JSON-object.
     */
    @Override
    public JSONObject getDatasetAsSmallJsonObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("time_recorded", timeRecorded);

            datasetAsJsonObject.put("battery_remaining", batteryRemaining);
            datasetAsJsonObject.put("battery_remaining_percent", batteryRemainingPercent);

            datasetAsJsonObject.put("remaining_flight_time", remainingFlightTime);
            datasetAsJsonObject.put("remaining_flight_radius", (int) remainingFlightRadius);    // accuracy: approx. 1m
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }

    /**
     * Returns a JSON-string containing the aircraft power data.
     *
     * @return The aircraft power data as a JSON-string.
     */
    @Override
    public String getDatasetAsJsonString() {
        return getDatasetAsJsonObject().toString();
    }

    /**
     * Returns a JSON-string containing the aircraft power data. This function removes extremely
     * unnecessary accuracy while still keeping the accuracy better than needed in this application.
     *
     * @return The aircraft power data as a JSON-string.
     */
    @Override
    public String getDatasetAsSmallJsonString() {
        return getDatasetAsSmallJsonObject().toString();
    }

}
