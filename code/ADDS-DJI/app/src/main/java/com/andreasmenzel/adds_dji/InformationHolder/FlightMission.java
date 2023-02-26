package com.andreasmenzel.adds_dji.InformationHolder;

import org.json.JSONException;
import org.json.JSONObject;

public class FlightMission {

    private long takeOffTime;
    private double takeOffLat;
    private double takeOffLon;

    private long landingTime;
    private double landingLat;
    private double landingLon;

    private long missionStartTime;
    private long missionUpdateTime;


    public FlightMission() {
        takeOffTime = 0;
        takeOffLat = 0;
        takeOffLon = 0;

        landingTime = 0;
        landingLat = 0;
        landingLon = 0;

        missionStartTime = 0;
        missionUpdateTime = 0;
    }


    public long getTakeOffTime() {
        return takeOffTime;
    }

    public double getTakeOffLat() {
        return takeOffLat;
    }

    public double getTakeOffLon() {
        return takeOffLon;
    }

    public long getMissionStartTime() {
        return missionStartTime;
    }

    public long getMissionUpdateTime() {
        return missionUpdateTime;
    }

    public JSONObject getDatasetAsJSONObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("takeoff_time", takeOffTime);
            datasetAsJsonObject.put("takeoff_lat", takeOffLat);
            datasetAsJsonObject.put("takeoff_lon", takeOffLon);

            datasetAsJsonObject.put("landing_time", landingTime);
            datasetAsJsonObject.put("landing_lat", landingLat);
            datasetAsJsonObject.put("landing_lon", landingLon);

            datasetAsJsonObject.put("mission_start_time", missionStartTime);
            datasetAsJsonObject.put("mission_update_time", missionUpdateTime);
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }

}
