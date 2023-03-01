package com.andreasmenzel.adds_dji;

import android.app.Application;
import android.content.Context;

import com.andreasmenzel.adds_dji.Managers.BlackboxManager;
import com.andreasmenzel.adds_dji.Managers.DJIManager;
import com.andreasmenzel.adds_dji.Managers.TrafficControlManager;
import com.secneo.sdk.Helper;


/**
 * The Main Application. All globally required variables are stored here.
 */
public class MApplication extends Application {

    private static DJIManager djiManager;
    private static TrafficControlManager trafficControlManager;
    private static BlackboxManager blackboxManager;


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
        trafficControlManager = new TrafficControlManager();
        blackboxManager = new BlackboxManager();
    }


    /**
     * Returns the DJIManager.
     * @return djiManager.
     */
    public static DJIManager getDjiManager() {
        return djiManager;
    }

    /**
     * Returns the TrafficControlManager.
     * @return trafficControlManager.
     */
    public static TrafficControlManager getTrafficControlManager() {
        return trafficControlManager;
    }

    /**
     * Returns the BlackboxManager.
     * @return blackboxManager.
     */
    public static BlackboxManager getBlackboxManager() {
        return blackboxManager;
    }

}