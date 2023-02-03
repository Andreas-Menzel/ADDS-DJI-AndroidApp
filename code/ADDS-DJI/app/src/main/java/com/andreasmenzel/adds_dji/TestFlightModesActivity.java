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

        findViewById(R.id.btn_testFlightModeTakeOffLanding).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, TakeOffLandingDemoActivity.class);
            startActivity(switchActivityIntent);
        });
        findViewById(R.id.btn_testFlightModeVirtualStickBasic).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, VirtualStickBasicDemoActivity.class);
            startActivity(switchActivityIntent);
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

}
