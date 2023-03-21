package com.andreasmenzel.adds_dji.Datasets;

public class Corridor {

    private String id;

    private boolean dataValid;

    private String intersectionAId;
    private String intersectionBId;


    public Corridor(String id) {
        this.id = id;
    }


    public void setValues(String intersectionAId, String intersectionBId) {
        dataValid = true;

        this.intersectionAId = intersectionAId;
        this.intersectionBId = intersectionBId;
    }


    public String getId() {
        return id;
    }
    public boolean getDataValid() {
        return dataValid;
    }
    public String getIntersectionAId() {
        return intersectionAId;
    }
    public String getIntersectionBId() {
        return intersectionBId;
    }
}
