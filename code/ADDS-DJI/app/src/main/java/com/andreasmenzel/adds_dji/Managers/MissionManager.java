package com.andreasmenzel.adds_dji.Managers;

import android.os.Handler;

import com.andreasmenzel.adds_dji.Datasets.Corridor;
import com.andreasmenzel.adds_dji.Datasets.Intersection;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.MApplication;
import com.andreasmenzel.adds_dji.OperationModes.Hovering;
import com.andreasmenzel.adds_dji.OperationModes.OnGround;
import com.andreasmenzel.adds_dji.OperationModes.UseVirtualStick;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionPause;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionReady;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionResume;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionStart;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionStop;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionUpload;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import okhttp3.OkHttpClient;

public class MissionManager {

    private final EventBus bus = EventBus.getDefault();

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

    // Last intersection that was uploaded to the drone.
    private Intersection lastUploadedIntersection = null;

    private final AtomicBoolean uploadInProgress = new AtomicBoolean(false);

    private final Handler uploadMissionHandler = new Handler();
    private final int uploadMissionHandlerDelay = 1000;


    public MissionManager() {
        //bus.register(this); // TODO: deregister?
    }

    public void startMission() {
        createAndUploadNewWaypoints();
    }

    public void stopMission() {
        DJIManager.changeOperationMode(new WaypointMissionStop());
    }

    public void pauseMission() {
        DJIManager.changeOperationMode(new WaypointMissionPause());
    }

    public void resumeMission() {
        DJIManager.changeOperationMode(new WaypointMissionResume());
    }


    private void checkAndInitiateClearanceAsk() {
        // Call this function every second.
        // corridorsPending not empty:
        //   addAsk? -> can also be part of TrafficControl
    }


    public void setLandAfterMissionFinished(boolean landAfterMissionFinished) {
        this.landAfterMissionFinished = landAfterMissionFinished;
    }

    public void setStartIntersection(Intersection startIntersection) {
        this.startIntersection = startIntersection;
    }

    public void addCorridor(Corridor corridor) {
        //corridorsPending.addLast(corridor);
        // TODO: REMOVE THIS TEST
        corridorsApproved.addLast(corridor);
    }

    /**
     * Builds, uploads and starts the current (new) mission.
     */
    private void createAndUploadNewWaypoints() {
        if(uploadInProgress.compareAndSet(false, true)) {
            uploadMissionHandler.removeCallbacksAndMessages(null);

            // Check if the Operation Mode allows for a new mission to be started now.
            if(MApplication.getDjiManager().getHighLevelOperationMode() instanceof OnGround
                    || MApplication.getDjiManager().getHighLevelOperationMode() instanceof Hovering
                    || MApplication.getDjiManager().getHighLevelOperationMode() instanceof UseVirtualStick
                    || MApplication.getDjiManager().getHighLevelOperationMode() instanceof WaypointMissionReady) {
                // If startIntersection is null, lastUploadedIntersection is also null
                if(startIntersection == null) {
                    bus.post(new ToastMessage("ERROR: MissionManager: Cannot create mission: startIntersection is null."));

                    uploadInProgress.set(false);
                    return;
                }
                if(corridorsApproved.isEmpty() && !corridorsPending.isEmpty()) {
                    //bus.post(new ToastMessage("MissionManager: Cannot create mission: Waiting for corridors to be approved."));
                    uploadMissionHandler.postDelayed(this::createAndUploadNewWaypoints, uploadMissionHandlerDelay);

                    uploadInProgress.set(false);
                    return;
                }
                if(corridorsApproved.isEmpty() && corridorsPending.isEmpty()) {
                    //bus.post(new ToastMessage("MissionManager: (Cannot create mission:) Mission already finished."));

                    uploadInProgress.set(false);
                    return;
                }

                // Move "uploaded" corridors to "finished".
                while(!corridorsUploaded.isEmpty()) {
                    corridorsFinished.addLast(corridorsUploaded.removeFirst());
                }

                DJIManager.waypointMissionClearWaypoints();

                if(lastUploadedIntersection == null) {
                    // Completely new mission
                    DJIManager.waypointMissionAddWaypoint(startIntersection.getGpsLat(), startIntersection.getGpsLon(), (float)(startIntersection.getAltitude()));
                    lastUploadedIntersection = startIntersection;
                } else {
                    // Mission extension
                    DJIManager.waypointMissionAddWaypoint(lastUploadedIntersection.getGpsLat(), lastUploadedIntersection.getGpsLon(), (float)(lastUploadedIntersection.getAltitude()));
                }

                // Move "approved" corridors to "uploaded" and upload to drone.
                while(!corridorsApproved.isEmpty()) {
                    Corridor cor = corridorsApproved.removeFirst();
                    corridorsUploaded.addLast(cor);

                    Intersection intersectionA = MApplication.getInfrastructureManager().getIntersection(cor.getIntersectionAId());
                    Intersection intersectionB = MApplication.getInfrastructureManager().getIntersection(cor.getIntersectionBId());

                    Intersection nextIntersection;
                    if(intersectionA != lastUploadedIntersection) {
                        nextIntersection = intersectionA;
                    } else if(intersectionB != lastUploadedIntersection) {
                        nextIntersection = intersectionB;
                    } else {
                        bus.post(new ToastMessage("MissionManager: Cannot create mission: path not connected?"));

                        uploadInProgress.set(false);
                        return;
                    }

                    DJIManager.waypointMissionAddWaypoint(nextIntersection.getGpsLat(), nextIntersection.getGpsLon(), (float)(nextIntersection.getAltitude()));
                    lastUploadedIntersection = nextIntersection;
                }

                // Set landing? after finished.
                WaypointMissionFinishedAction finishedAction;
                if(landAfterMissionFinished && corridorsPending.isEmpty() && corridorsApproved.isEmpty()) {
                    finishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else {
                    finishedAction = WaypointMissionFinishedAction.NO_ACTION;
                }
                DJIManager.getWaypointMissionBuilder().finishedAction(finishedAction);

                // Stop a potentially running waypoint mission and start the new mission.
                DJIManager.changeOperationMode(new WaypointMissionStop(new WaypointMissionUpload()));

                uploadMissionHandler.postDelayed(this::createAndUploadNewWaypoints, uploadMissionHandlerDelay);
            } else {
                uploadMissionHandler.postDelayed(this::createAndUploadNewWaypoints, uploadMissionHandlerDelay);
            }

            uploadInProgress.set(false);
        }
    }

}
