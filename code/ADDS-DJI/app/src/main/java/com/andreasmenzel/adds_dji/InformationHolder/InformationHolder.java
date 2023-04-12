package com.andreasmenzel.adds_dji.InformationHolder;

import org.json.JSONObject;

public interface InformationHolder {

    JSONObject getDatasetAsJsonObject();
    JSONObject getDatasetAsSmallJsonObject();

    String getDatasetAsJsonString();
    String getDatasetAsSmallJsonString();

    void dataUpdated();
    boolean getAndSetDataUpdatedSinceLastTrafficControlUpdate();

}
