package com.andreasmenzel.adds_dji;

import android.app.Application;
import android.content.Context;

import com.andreasmenzel.adds_dji.Events.DJIManager.CreatedDJIManager;
import com.andreasmenzel.adds_dji.Events.SdkRegistered;
import com.andreasmenzel.adds_dji.Manager.DJIManager;
import com.andreasmenzel.adds_dji.Manager.TrafficControlManager;
import com.secneo.sdk.Helper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MApplication extends Application {

    private static final EventBus bus = EventBus.getDefault();

    private static DJIManager djiManager;
    private static TrafficControlManager trafficControlManager;


    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);

        bus.register(this);

        trafficControlManager = new TrafficControlManager();
    }


    @Subscribe
    public void sdkRegistered(SdkRegistered event) {
        djiManager = new DJIManager();
        bus.post(new CreatedDJIManager());
    }


    public static DJIManager getDjiManager() {
        return djiManager;
    }

    public static TrafficControlManager getTrafficControlManager() {
        return trafficControlManager;
    }

}