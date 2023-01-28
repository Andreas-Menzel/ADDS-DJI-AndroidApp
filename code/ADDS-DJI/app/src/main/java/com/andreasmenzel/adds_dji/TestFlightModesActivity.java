package com.andreasmenzel.adds_dji;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

public class TestFlightModesActivity extends AppCompatActivity {

    private static EventBus bus = EventBus.getDefault();


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test_flight_modes);

        findViewById(R.id.btn_startVirtualStickDemo).setOnClickListener((View view) -> {
            startVirtualStickDemo();
        });
        findViewById(R.id.btn_startWaypointMissionDemo).setOnClickListener((View view) -> {
            startWaypointMissionDemo();
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        //bus.register(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        //bus.unregister(this);
    }


    private void startVirtualStickDemo() {
        Intent switchActivityIntent = new Intent(this, VirtualStickDemoActivity.class);
        startActivity(switchActivityIntent);
    }


    private void startWaypointMissionDemo() {
        Intent switchActivityIntent = new Intent(this, WaypointMissionDemoActivity.class);
        startActivity(switchActivityIntent);
    }

}
