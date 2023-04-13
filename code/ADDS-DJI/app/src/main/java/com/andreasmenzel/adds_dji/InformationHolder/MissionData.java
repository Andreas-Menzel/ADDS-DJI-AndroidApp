package com.andreasmenzel.adds_dji.InformationHolder;

import com.andreasmenzel.adds_dji.Datasets.Corridor;
import com.andreasmenzel.adds_dji.Datasets.Intersection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MissionData implements InformationHolder {

    private AtomicBoolean dataUpdatedSinceLastTrafficControlUpdate = new AtomicBoolean(true);

    private double timeRecorded = 0;

    private Intersection startIntersection = null;
    private boolean landAfterMissionFinished = false;

    // Complete path: finished-uploaded-approved-pending
    // Corridor path with no clearance from Traffic Control yet.
    private LinkedList<Corridor> corridorsPending = new LinkedList<>();
    // Corridor path with clearance from Traffic Control not uploaded to the drone or finished.
    private LinkedList<Corridor> corridorsApproved = new LinkedList<>();
    // Corridor path (with clearance) uploaded to the drone.
    private LinkedList<Corridor> corridorsUploaded = new LinkedList<>();
    // Corridor path that was uploaded to the drone and finished (no longer uploaded to the drone).
    private LinkedList<Corridor> corridorsFinished = new LinkedList<>();

    // Last intersection that of the mission.
    private Intersection lastMissionIntersection = null;
    // Last intersection that was uploaded to the drone.
    private Intersection lastUploadedIntersection = null;


    public Intersection getStartIntersection() {
        return startIntersection;
    }

    public boolean getLandAfterMissionFinished() {
        return landAfterMissionFinished;
    }

    public LinkedList<Corridor> getCorridorsPending() {
        return corridorsPending;
    }

    public LinkedList<Corridor> getCorridorsApproved() {
        return corridorsApproved;
    }

    public LinkedList<Corridor> getCorridorsUploaded() {
        return corridorsUploaded;
    }

    public LinkedList<Corridor> getCorridorsFinished() {
        return corridorsFinished;
    }

    public Intersection getLastMissionIntersection() {
        return lastMissionIntersection;
    }

    public Intersection getLastUploadedIntersection() {
        return lastUploadedIntersection;
    }


    public void setLandAfterMissionFinished(boolean landAfterMissionFinished) {
        this.landAfterMissionFinished = landAfterMissionFinished;
        dataUpdated();
    }

    public void setStartIntersection(Intersection startIntersection) {
        this.startIntersection = startIntersection;
        dataUpdated();
    }

    public void setLastMissionIntersection(Intersection lastMissionIntersection) {
        this.lastMissionIntersection = lastMissionIntersection;
        dataUpdated();
    }

    public void setLastUploadedIntersection(Intersection lastUploadedIntersection) {
        this.lastUploadedIntersection = lastUploadedIntersection;
        dataUpdated();
    }


    public void dataUpdated() {
        this.timeRecorded = System.currentTimeMillis() / 1000.0;
        dataUpdatedSinceLastTrafficControlUpdate.set(true);
    }
    public boolean getAndSetDataUpdatedSinceLastTrafficControlUpdate() {
        return dataUpdatedSinceLastTrafficControlUpdate.getAndSet(false);
    }


    @Override
    public JSONObject getDatasetAsJsonObject() {
        JSONObject datasetAsJsonObject = new JSONObject();

        String startIntersectionId = null;
        if(startIntersection != null) {
            startIntersectionId = startIntersection.getId();
        }

        try {
            datasetAsJsonObject.put("time_recorded", timeRecorded);

            datasetAsJsonObject.put("start_intersection", startIntersectionId);
            datasetAsJsonObject.put("last_uploaded_intersection", lastUploadedIntersection);
            datasetAsJsonObject.put("last_mission_intersection", lastMissionIntersection);
            datasetAsJsonObject.put("land_after_mission_finished", landAfterMissionFinished);

            JSONArray corridorsPendingJsonArray = new JSONArray();
            for(int i = 0; i < corridorsPending.size(); ++i) {
                corridorsPendingJsonArray.put(corridorsPending.get(i));
            }
            datasetAsJsonObject.put("corridors_pending", corridorsPendingJsonArray);

            JSONArray corridorsApprovedJsonArray = new JSONArray();
            for(int i = 0; i < corridorsApproved.size(); ++i) {
                corridorsApprovedJsonArray.put(corridorsApproved.get(i));
            }
            datasetAsJsonObject.put("corridors_approved", corridorsApprovedJsonArray);

            JSONArray corridorsUploadedJsonArray = new JSONArray();
            for(int i = 0; i < corridorsUploaded.size(); ++i) {
                corridorsUploadedJsonArray.put(corridorsUploaded.get(i));
            }
            datasetAsJsonObject.put("corridors_uploaded", corridorsUploadedJsonArray);

            JSONArray corridorsFinishedJsonArray = new JSONArray();
            for(int i = 0; i < corridorsFinished.size(); ++i) {
                corridorsUploadedJsonArray.put(corridorsFinished.get(i));
            }
            datasetAsJsonObject.put("corridors_finished", corridorsFinishedJsonArray);
        } catch (JSONException e) {
            // TODO: error handling
            datasetAsJsonObject = null;
        }

        return datasetAsJsonObject;
    }

    @Override
    // TODO: Make small object
    public JSONObject getDatasetAsSmallJsonObject() {
        return getDatasetAsJsonObject();
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
