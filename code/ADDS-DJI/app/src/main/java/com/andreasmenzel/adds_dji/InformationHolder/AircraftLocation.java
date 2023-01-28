package com.andreasmenzel.adds_dji.InformationHolder;

public class AircraftLocation {

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
     * Pitch, yaw and roll in degrees. -180 - 180.
     */
    private double pitch;
    private double yaw;
    private double roll;




    // TODO: Potential pitfall: altitude and attitude are 0 on default!
    public AircraftLocation() {
        this.gpsSignalLevel = 0;
        this.gpsSatellitesConnected = 0;

        this.gpsValid = false;
        this.gpsLat = 0;
        this.gpsLon = 0;

        this.altitude = 0;

        this.pitch = 0;
        this.yaw = 0;
        this.roll = 0;
    }

    public AircraftLocation(int gpsSignalLevel, int gpsSatellitesConnected,
                            boolean gpsValid, double gpsLat, double gpsLon,
                            float altitude,
                            double pitch, double yaw, double roll) {
        this.gpsSignalLevel = gpsSignalLevel;
        this.gpsSatellitesConnected = gpsSatellitesConnected;

        this.gpsValid = gpsValid;
        this.gpsLat = gpsLat;
        this.gpsLon = gpsLon;

        this.altitude = altitude;

        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }


    /*
     * Setter method
     */
    public void updateData(int gpsSignalLevel, int gpsSatellitesConnected,
                           boolean gpsValid, double gpsLat, double gpsLon,
                           float altitude,
                           double pitch, double yaw, double roll) {
        this.gpsSignalLevel = gpsSignalLevel;
        this.gpsSatellitesConnected = gpsSatellitesConnected;

        this.gpsValid = gpsValid;
        this.gpsLat = gpsLat;
        this.gpsLon = gpsLon;

        this.altitude = altitude;

        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }


    /*
     * Getter methods
     */
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
    public double getPitch() {
        return pitch;
    }
    public double getYaw() {
        return yaw;
    }
    public double getRoll() {
        return roll;
    }

}
