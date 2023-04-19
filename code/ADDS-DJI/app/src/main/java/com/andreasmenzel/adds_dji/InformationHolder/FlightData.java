package com.andreasmenzel.adds_dji.InformationHolder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlightData implements InformationHolder {

    private final AtomicBoolean dataUpdatedSinceLastFlightControlUpdate = new AtomicBoolean(true);

    private double timeRecorded;

    private long takeOffTime;
    private boolean takeOffGpsValid;
    private double takeOffGpsLat;
    private double takeOffGpsLon;

    private long landingTime;
    private boolean landingGpsValid;
    private double landingGpsLat;
    private double landingGpsLon;

    // Save the last N Operation Modes
    private LinkedList<String> operationModes;
    private final int operationModesHistoryLength = 10;


    public FlightData() {
        timeRecorded = 0;

        takeOffTime = 0;
        takeOffGpsValid = false;
        takeOffGpsLat = 0;
        takeOffGpsLon = 0;

        landingTime = 0;
        landingGpsValid = false;
        landingGpsLat = 0;
        landingGpsLon = 0;

        operationModes = new LinkedList<>();
    }


    public void updateTakeOffData(long takeOffTime, boolean takeOffGpsValid, double takeOffGpsLat, double takeOffGpsLon) {
        this.takeOffTime = takeOffTime;
        this.takeOffGpsValid = takeOffGpsValid;
        this.takeOffGpsLat = takeOffGpsLat;
        if(Double.isNaN(this.takeOffGpsLat)) this.takeOffGpsLat = 0;
        this.takeOffGpsLon = takeOffGpsLon;
        if(Double.isNaN(this.takeOffGpsLon)) this.takeOffGpsLon = 0;

        dataUpdated();
    }

    public void updateLandingData(long landingTime, boolean landingGpsValid, double landingGpsLat, double landingGpsLon) {
        this.landingTime = landingTime;
        this.landingGpsValid = landingGpsValid;
        this.landingGpsLat = landingGpsLat;
        if(Double.isNaN(this.landingGpsLat)) this.landingGpsLat = 0;
        this.landingGpsLon = landingGpsLon;
        if(Double.isNaN(this.landingGpsLon)) this.landingGpsLon = 0;

        dataUpdated();
    }

    public void updateOperationMode(String operationMode) {
        // Only add Operation Mode if has changed
        if(operationModes.size() == 0 || !operationModes.getLast().equals(operationMode)) {
            operationModes.addLast(operationMode);

            dataUpdated();
        }
        if(operationModes.size() > operationModesHistoryLength) operationModes.removeFirst();
    }


    public void dataUpdated() {
        this.timeRecorded = System.currentTimeMillis() / 1000.0;
        dataUpdatedSinceLastFlightControlUpdate.set(true);
    }
    public boolean getAndSetDataUpdatedSinceLastFlightControlUpdate() {
        return dataUpdatedSinceLastFlightControlUpdate.getAndSet(false);
    }


    private double roundDouble(double val, int decimals) {
        return Math.round(val * Math.pow(10, decimals)) / Math.pow(10, decimals);
    }


    public double getTimeRecorded() {
        return timeRecorded;
    }

    public long getTakeOffTime() {
        return takeOffTime;
    }

    public boolean getTakeOffGpsValid() {
        return takeOffGpsValid;
    }

    public double getTakeOffGpsLat() {
        return takeOffGpsLat;
    }

    public double getTakeOffGpsLon() {
        return takeOffGpsLon;
    }

    public long getLandingTime() {
        return landingTime;
    }

    public boolean getLandingGpsValid() {
        return landingGpsValid;
    }

    public double getLandingGpsLat() {
        return landingGpsLat;
    }

    public double getLandingGpsLon() {
        return landingGpsLon;
    }


    @Override
    public JSONObject getDatasetAsJsonObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("time_recorded", timeRecorded);

            datasetAsJsonObject.put("takeoff_time", takeOffTime);
            datasetAsJsonObject.put("takeoff_gps_valid", takeOffGpsValid);
            datasetAsJsonObject.put("takeoff_gps_lat", takeOffGpsLat);
            datasetAsJsonObject.put("takeoff_gps_lon", takeOffGpsLon);

            datasetAsJsonObject.put("landing_time", landingTime);
            datasetAsJsonObject.put("landing_gps_valid", landingGpsValid);
            datasetAsJsonObject.put("landing_gps_lat", landingGpsLat);
            datasetAsJsonObject.put("landing_gps_lon", landingGpsLon);

            JSONArray operationModesJsonArray = new JSONArray();
            for(int i = operationModes.size() - 1; i >= 0; --i) operationModesJsonArray.put(operationModes.get(i));
            datasetAsJsonObject.put("operation_modes", operationModesJsonArray);
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }

    @Override
    public JSONObject getDatasetAsSmallJsonObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("time_recorded", timeRecorded);

            datasetAsJsonObject.put("takeoff_time", takeOffTime);
            datasetAsJsonObject.put("takeoff_gps_valid", takeOffGpsValid);
            datasetAsJsonObject.put("takeoff_gps_lat", roundDouble(takeOffGpsLat, 8));  // accuracy: approx. 1mm
            datasetAsJsonObject.put("takeoff_gps_lon", roundDouble(takeOffGpsLon, 8));  // accuracy: approx. 1mm

            datasetAsJsonObject.put("landing_time", landingTime);
            datasetAsJsonObject.put("landing_gps_valid", landingGpsValid);
            datasetAsJsonObject.put("landing_gps_lat", roundDouble(landingGpsLat, 8));  // accuracy: approx. 1mm
            datasetAsJsonObject.put("landing_gps_lon", roundDouble(landingGpsLon, 8));  // accuracy: approx. 1mm

            datasetAsJsonObject.put("operation_modes", operationModes);
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }

    @Override
    public String getDatasetAsJsonString() {
        return getDatasetAsJsonObject().toString();
    }

    @Override
    public String getDatasetAsSmallJsonString() {
        return getDatasetAsSmallJsonObject().toString();
    }

}
