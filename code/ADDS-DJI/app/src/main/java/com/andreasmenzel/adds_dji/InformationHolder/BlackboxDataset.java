package com.andreasmenzel.adds_dji.InformationHolder;

import org.json.JSONException;
import org.json.JSONObject;

public class BlackboxDataset {

    private AircraftLocation aircraftLocation;
    private AircraftPower aircraftPower;
    private AircraftHealth aircraftHealth;
    private FlightData flightData;


    public BlackboxDataset() {
        aircraftLocation = new AircraftLocation();
        aircraftPower = new AircraftPower();
        aircraftHealth = new AircraftHealth();
        flightData = new FlightData();
    }


    public JSONObject getDatasetAsJSONObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("aircraft_location", aircraftLocation.getDatasetAsJsonObject());
            datasetAsJsonObject.put("aircraft_power", aircraftPower.getDatasetAsJsonObject());
            datasetAsJsonObject.put("aircraft_health", aircraftHealth.getDatasetAsJSONObject());
            datasetAsJsonObject.put("flight_data", flightData.getDatasetAsJsonObject());
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

    public void setFlightMission(FlightData flightData) {
        this.flightData = flightData;
    }

}
