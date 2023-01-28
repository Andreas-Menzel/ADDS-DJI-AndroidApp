package com.andreasmenzel.adds_dji;

import android.app.Application;
import android.content.Context;

import com.andreasmenzel.adds_dji.Manager.DJIManager;
import com.andreasmenzel.adds_dji.Manager.TrafficSystemManager;
import com.secneo.sdk.Helper;

public class MApplication extends Application {

    private static DJIManager djiManager;
    private static TrafficSystemManager trafficSystemManager;


    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);

        djiManager = new DJIManager();
        trafficSystemManager = new TrafficSystemManager();
    }

    public static DJIManager getDjiManager() {
        return djiManager;
    }

    public static TrafficSystemManager getTrafficSystemManager() {
        return trafficSystemManager;
    }

}