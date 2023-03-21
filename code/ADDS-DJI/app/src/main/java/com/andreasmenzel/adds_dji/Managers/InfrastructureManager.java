package com.andreasmenzel.adds_dji.Managers;

import android.util.Log;

import com.andreasmenzel.adds_dji.Datasets.Corridor;
import com.andreasmenzel.adds_dji.Datasets.Intersection;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.MApplication;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;

public class InfrastructureManager {

    private EventBus bus = EventBus.getDefault();

    private String trafficControlUrl;

    private final OkHttpClient client = new OkHttpClient();

    private Map<String, Intersection> intersections;
    private Map<String, Corridor> corridors;


    public InfrastructureManager() {
        //bus.register(this); // TODO: deregister?

        trafficControlUrl = MApplication.getTrafficControlManager().getTrafficControlUrl();

        intersections = new HashMap<>();
        corridors = new HashMap<>();
    }


    public void updateIntersectionList(JSONObject responseData) {
        Set<String> intersectionIdsToKeep = new HashSet<>();

        Iterator<String> intersectionIds = responseData.keys();
        while(intersectionIds.hasNext()) {
            String intId = intersectionIds.next();
            intersectionIdsToKeep.add(intId);
            try {
                JSONObject intersectionData = responseData.getJSONObject(intId);
                double gpsLat = intersectionData.getDouble("gps_lat");
                double gpsLon = intersectionData.getDouble("gps_lon");
                double altitude = intersectionData.getDouble("altitude");

                if(intersections.get(intId) == null) {
                    intersections.put(intId, new Intersection(intId));
                }
                intersections.get(intId).setValues(gpsLat, gpsLon, altitude);
            } catch (JSONException e) {
                // TODO: error handling
            }
        }

        // Remove old intersections (that were deleted)
        intersections.keySet().retainAll(intersectionIdsToKeep);
    }

    public void updateCorridorList(JSONObject responseData) {
        Set<String> corridorIdsToKeep = new HashSet<>();

        Iterator<String> corridorIds = responseData.keys();
        while(corridorIds.hasNext()) {
            String corId = corridorIds.next();
            corridorIdsToKeep.add(corId);
            try {
                JSONObject corridorData = responseData.getJSONObject(corId);
                String intersectionAId = corridorData.getString("intersection_a");
                String intersectionBId = corridorData.getString("intersection_b");

                if(corridors.get(corId) == null) {
                    corridors.put(corId, new Corridor(corId));
                }
                corridors.get(corId).setValues(intersectionAId, intersectionBId);
            } catch (JSONException e) {
                // TODO: error handling
            }
        }

        // Remove old corridors (that were deleted)
        corridors.keySet().retainAll(corridorIdsToKeep);
    }


    public Intersection getIntersection(String intersectionId) {
        return intersections.get(intersectionId);
    }

    public Corridor getCorridor(String corridorId) {
        return corridors.get(corridorId);
    }

}
