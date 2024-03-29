package com.andreasmenzel.adds_dji;

import android.app.Application;
import android.content.Context;

import com.andreasmenzel.adds_dji.Managers.FlightControlManager;
import com.andreasmenzel.adds_dji.Managers.InfrastructureManager;
import com.andreasmenzel.adds_dji.Managers.DJIManager;
import com.andreasmenzel.adds_dji.Managers.MissionManager;
import com.secneo.sdk.Helper;


/**
 * The Main Application. All globally required variables are stored here.
 */
public class MApplication extends Application {

    private static String droneId = "";
    private static boolean droneActive = false;

    private static DJIManager djiManager;
    private static FlightControlManager flightControlManager;
    private static InfrastructureManager infrastructureManager;
    private static MissionManager missionManager;


    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
    }


    /**
     * Initializes all managers (see "Managers" package).
     */
    public static void initializeManagers() {
        djiManager = new DJIManager();
        missionManager = new MissionManager();
        flightControlManager = new FlightControlManager();
        infrastructureManager = new InfrastructureManager();
    }


    /**
     * Returns the drone id.
     * @return droneId.
     */
    public static String getDroneId() {
        return droneId;
    }

    /**
     * Sets a new drone id.
     * @param newDroneId The new drone id.
     */
    public static void setDroneId(String newDroneId) {
        droneId = newDroneId;
    }

    public static boolean getDroneActive() {
        return droneActive;
    }

    public static void setDroneActive(boolean newDroneActive) {
        droneActive = newDroneActive;
    }

    /**
     * Returns the DJIManager.
     * @return djiManager.
     */
    public static DJIManager getDjiManager() {
        return djiManager;
    }

    /**
     * Returns the FlightControlManager.
     * @return flightControlManager.
     */
    public static FlightControlManager getFlightControlManager() {
        return flightControlManager;
    }

    /**
     * Returns the InfrastructureManager.
     * @return infrastructureManager.
     */
    public static InfrastructureManager getInfrastructureManager() {
        return infrastructureManager;
    }

    /**
     * Returns the MissionManager.
     * @return missionManager.
     */
    public static MissionManager getMissionManager() {
        return missionManager;
    }
}