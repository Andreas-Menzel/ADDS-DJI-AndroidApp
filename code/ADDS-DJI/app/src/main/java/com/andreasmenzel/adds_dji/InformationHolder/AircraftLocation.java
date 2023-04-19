package com.andreasmenzel.adds_dji.InformationHolder;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class AircraftLocation implements InformationHolder {

    private final AtomicBoolean dataUpdatedSinceLastTrafficControlUpdate = new AtomicBoolean(true);

    private double timeRecorded;

    /*
     * NOTE: This differs from the DJI specification!
     *
     * 0: No GPS signal
     * 1: Almost no GPS signal
     * 2: Very weak GPS signal
     * 3: Weak GPS signal (minimum for "go home")
     * 4: Good GPS signal (minimum for normal operation)
     * 5: Very good GPS signal (preferred)
     * 6: Very strong GPS signal (preferred - better)
     */
    private int gpsSignalLevel;
    private int gpsSatellitesConnected;

    /*
     * GPS latitude (gpsLat) and GPS longitude (gpsLon) are only valid if this variable is true.
     */
    private boolean gpsValid;
    private double gpsLat;
    private double gpsLon;

    /*
     * Altitude in meters.
     */
    private float altitude;

    /*
     * Aircraft velocities in m/s using N-E-D (North-East-Down) coordinate system.
     */
    private float velocityX;
    private float velocityY;
    private float velocityZ;

    /*
     * Pitch, yaw and roll in degrees. -180 - 180.
     */
    private double pitch;
    private double yaw;
    private double roll;



    // TODO: Potential pitfall: altitude and attitude are 0 on default!
    public AircraftLocation() {
        this.timeRecorded = 0;

        this.gpsSignalLevel = 0;
        this.gpsSatellitesConnected = 0;

        this.gpsValid = false;
        this.gpsLat = 0;
        this.gpsLon = 0;

        this.altitude = 0;

        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;

        this.pitch = 0;
        this.yaw = 0;
        this.roll = 0;
    }


    /*
     * Setter method
     */
    public void updateData(int gpsSignalLevel, int gpsSatellitesConnected,
                           boolean gpsValid, double gpsLat, double gpsLon,
                           float altitude,
                           float velocityX, float velocityY, float velocityZ,
                           double pitch, double yaw, double roll) {
        this.gpsSignalLevel = gpsSignalLevel;
        this.gpsSatellitesConnected = gpsSatellitesConnected;

        this.gpsValid = gpsValid;
        this.gpsLat = gpsLat;
        if(Double.isNaN(this.gpsLat)) this.gpsLat = 0;
        this.gpsLon = gpsLon;
        if(Double.isNaN(this.gpsLon)) this.gpsLon = 0;

        this.altitude = altitude;

        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;

        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;

        dataUpdated();
    }

    public void dataUpdated() {
        this.timeRecorded = System.currentTimeMillis() / 1000.0;
        dataUpdatedSinceLastTrafficControlUpdate.set(true);
    }
    public boolean getAndSetDataUpdatedSinceLastTrafficControlUpdate() {
        return dataUpdatedSinceLastTrafficControlUpdate.getAndSet(false);
    }


    private double roundDouble(double val, int decimals) {
        return Math.round(val * Math.pow(10, decimals)) / Math.pow(10, decimals);
    }

    private float roundFloat(float val, int decimals) {
        return (float) (Math.round(val * Math.pow(10, decimals)) / Math.pow(10, decimals));
    }


    /*
     * Getter methods
     */

    public double getTimeRecorded() {
        return timeRecorded;
    }
    public int getGpsSignalLevel() {
        return gpsSignalLevel;
    }
    public int getGpsSatellitesConnected() {
        return gpsSatellitesConnected;
    }
    public boolean getGpsValid() {
        return gpsValid;
    }
    public double getGpsLat() {
        return gpsLat;
    }
    public double getGpsLon() {
        return gpsLon;
    }
    public float getAltitude() {
        return altitude;
    }
    public float getVelocityX() {
        return velocityX;
    }
    public float getVelocityY() {
        return velocityY;
    }
    public float getVelocityZ() {
        return velocityZ;
    }
    public double getPitch() {
        return pitch;
    }
    public double getYaw() {
        return yaw;
    }
    public double getRoll() {
        return roll;
    }


    /**
     * Returns a JSON-object containing the aircraft location data.
     *
     * @return The aircraft location data as a JSON-object.
     */
    @Override
    public JSONObject getDatasetAsJsonObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("time_recorded", timeRecorded);

            datasetAsJsonObject.put("gps_signal_level", gpsSignalLevel);
            datasetAsJsonObject.put("gps_satellites_connected", gpsSatellitesConnected);

            datasetAsJsonObject.put("gps_valid", gpsValid);
            datasetAsJsonObject.put("gps_lat", gpsLat);
            datasetAsJsonObject.put("gps_lon", gpsLon);

            datasetAsJsonObject.put("altitude", altitude);

            datasetAsJsonObject.put("velocity_x", velocityX);
            datasetAsJsonObject.put("velocity_y", velocityY);
            datasetAsJsonObject.put("velocity_z", velocityZ);

            datasetAsJsonObject.put("pitch", pitch);
            datasetAsJsonObject.put("yaw", yaw);
            datasetAsJsonObject.put("roll", roll);
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }

    /**
     * Returns a JSON-object containing the aircraft location data. This function removes
     * extremely unnecessary accuracy while still keeping the accuracy better than needed in this
     * application.
     *
     * @return The aircraft location data as a JSON-object.
     */
    @Override
    public JSONObject getDatasetAsSmallJsonObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        try {
            datasetAsJsonObject.put("time_recorded", timeRecorded);

            datasetAsJsonObject.put("gps_signal_level", gpsSignalLevel);
            datasetAsJsonObject.put("gps_satellites_connected", gpsSatellitesConnected);

            datasetAsJsonObject.put("gps_valid", gpsValid);
            datasetAsJsonObject.put("gps_lat", roundDouble(gpsLat, 8));     // accuracy: approx. 1mm
            datasetAsJsonObject.put("gps_lon", roundDouble(gpsLon, 8));     // accuracy: approx. 1mm

            datasetAsJsonObject.put("altitude", roundFloat(altitude, 3));    // accuracy: approx. 1mm

            datasetAsJsonObject.put("velocity_x", roundFloat(velocityX, 2));   // accuracy: approx. 0.01m/s ~= 0.036km/h
            datasetAsJsonObject.put("velocity_y", roundFloat(velocityY, 2));   // accuracy: approx. 0.01m/s ~= 0.036km/h
            datasetAsJsonObject.put("velocity_z", roundFloat(velocityZ, 2));   // accuracy: approx. 0.01m/s ~= 0.036km/h

            datasetAsJsonObject.put("pit", pitch);
            datasetAsJsonObject.put("yaw", yaw);
            datasetAsJsonObject.put("rol", roll);
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }

    /**
     * Returns a JSON-string containing the aircraft location data.
     *
     * @return The aircraft location data as a JSON-string.
     */
    @Override
    public String getDatasetAsJsonString() {
        return getDatasetAsJsonObject().toString();
    }

    /**
     * Returns a JSON-string containing the aircraft location data. This function removes
     * extremely unnecessary accuracy while still keeping the accuracy better than needed in this
     * application.
     *
     * @return The aircraft location data as a JSON-string.
     */
    @Override
    public String getDatasetAsSmallJsonString() {
        return getDatasetAsSmallJsonObject().toString();
    }

}
