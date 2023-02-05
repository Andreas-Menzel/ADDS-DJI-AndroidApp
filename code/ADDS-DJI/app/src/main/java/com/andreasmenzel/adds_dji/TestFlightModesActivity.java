package com.andreasmenzel.adds_dji;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds_dji.Events.ToastMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Shows a list of flight modes that can be tested to make sure that the drone and all (sub)modes
 * function correctly.
 */
public class TestFlightModesActivity extends AppCompatActivity {

    private static final EventBus bus = EventBus.getDefault();


    /**
     * Initializes this activity: Sets up the onClickListeners.
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_flight_modes);

        findViewById(R.id.btn_showTakeOffLandingDemoActivity).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, TakeOffLandingDemoActivity.class);
            startActivity(switchActivityIntent);
        });
        findViewById(R.id.btn_showVirtualStickBasicDemoActivity).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, VirtualStickBasicDemoActivity.class);
            startActivity(switchActivityIntent);
        });
        findViewById(R.id.btn_showVirtualStickCrossDemoActivity).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, VirtualStickCrossDemoActivity.class);
            startActivity(switchActivityIntent);
        });
    }


    /**
     * Registers to the event bus.
     */
    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);
    }

    /**
     * Unregisters from the event bus.
     */
    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }


    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_LONG).show();
    }

}
