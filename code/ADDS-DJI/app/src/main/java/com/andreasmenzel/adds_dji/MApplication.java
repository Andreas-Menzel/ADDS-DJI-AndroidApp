package com.andreasmenzel.adds_dji;

import android.app.Application;
import android.content.Context;

import com.andreasmenzel.adds_dji.Manager.DJIManager;
import com.andreasmenzel.adds_dji.Manager.TrafficControlManager;
import com.secneo.sdk.Helper;

public class MApplication extends Application {

    private static DJIManager djiManager;
    private static TrafficControlManager trafficControlManager;


    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);

        djiManager = new DJIManager();
        trafficControlManager = new TrafficControlManager();
    }

    public static DJIManager getDjiManager() {
        return djiManager;
    }

    public static TrafficControlManager getTrafficControlManager() {
        return trafficControlManager;
    }

}