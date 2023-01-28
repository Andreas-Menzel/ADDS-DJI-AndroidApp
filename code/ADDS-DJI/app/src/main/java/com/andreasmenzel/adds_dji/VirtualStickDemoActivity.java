package com.andreasmenzel.adds_dji;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

public class VirtualStickDemoActivity extends AppCompatActivity {

    private EventBus bus = EventBus.getDefault();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_virtual_stick_demo);
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
