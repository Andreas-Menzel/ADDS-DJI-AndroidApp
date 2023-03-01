package com.andreasmenzel.adds_dji.InformationHolder;

import org.json.JSONException;
import org.json.JSONObject;

public class BlackboxDataset {

    private AircraftLocation aircraftLocation;
    private AircraftPower aircraftPower;
    private AircraftHealth aircraftHealth;
    private FlightMission flightMission;


    public BlackboxDataset() {
        aircraftLocation = new AircraftLocation();
        aircraftPower = new AircraftPower();
        aircraftHealth = new AircraftHealth();
        flightMission = new FlightMission();
    }


    public JSONObject getDatasetAsJSONObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("aircraft_location", aircraftLocation.getDatasetAsJSONObject());
            datasetAsJsonObject.put("aircraft_power", aircraftPower.getDatasetAsJSONObject());
            datasetAsJsonObject.put("aircraft_health", aircraftHealth.getDatasetAsJSONObject());
            datasetAsJsonObject.put("flight_mission", flightMission.getDatasetAsJSONObject());
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }


    public String getDatasetAsString() {
        String datasetAsString = "";

        try {
            datasetAsString = getDatasetAsJSONObject().toString(4);
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsString = null;
        }

        return datasetAsString;
    }


    public void setAircraftLocation(AircraftLocation aircraftLocation) {
        this.aircraftLocation = aircraftLocation;
    }

    public void setAircraftPower(AircraftPower aircraftPower) {
        this.aircraftPower = aircraftPower;
    }

    public void setAircraftHealth(AircraftHealth aircraftHealth) {
        this.aircraftHealth = aircraftHealth;
    }

    public void setFlightMission(FlightMission flightMission) {
        this.flightMission = flightMission;
    }

}
