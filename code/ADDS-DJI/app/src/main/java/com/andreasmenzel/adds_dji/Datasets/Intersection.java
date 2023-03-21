package com.andreasmenzel.adds_dji.Datasets;

public class Intersection {

    private String id;

    private boolean dataValid;

    private double gpsLat;
    private double gpsLon;

    private double altitude;


    public Intersection(String id) {
        this.id = id;
    }


    public void setValues(double gpsLat, double gpsLon, double altitude) {
        dataValid = true;

        this.gpsLat = gpsLat;
        this.gpsLon = gpsLon;
        this.altitude = altitude;
    }


    public String getId() {
        return id;
    }
    public boolean getDataValid() {
        return dataValid;
    }
    public double getGpsLat() {
        return gpsLat;
    }
    public double getGpsLon() {
        return gpsLon;
    }
    public double getAltitude() {
        return altitude;
    }
}
