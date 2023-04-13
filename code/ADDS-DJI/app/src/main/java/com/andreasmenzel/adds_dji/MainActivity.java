package com.andreasmenzel.adds_dji;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

// Events
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnectivityChange;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.ConnectionCheckInProgress;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.ConnectionEvent;

// Manager
import com.andreasmenzel.adds_dji.Managers.DJIManager;
import com.andreasmenzel.adds_dji.Managers.TrafficControlManager;

/**
 * The main activity. This is shown when the app is started.
 */
public class MainActivity extends AppCompatActivity {

    private static final EventBus bus = EventBus.getDefault();

    private DJIManager djiManager;
    private TrafficControlManager trafficControlManager;


    /**
     * Initializes this activity: Gets the custom managers, checks the permissions, starts the
     * SDK registration and sets up the onClickListeners.
     *
     * @param savedInstanceState savedInstanceState.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        djiManager = MApplication.getDjiManager();
        trafficControlManager = MApplication.getTrafficControlManager();


        findViewById(R.id.btn_showDroneInfoActivity).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, DroneInfoActivity.class);
            startActivity(switchActivityIntent);
        });
        findViewById(R.id.btn_showTestFlightModesActivity).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, TestFlightModesActivity.class);
            startActivity(switchActivityIntent);
        });
        findViewById(R.id.btn_showFPVDemoActivity).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, FPVDemoActivity.class);
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
     * Makes sure that the UI is updated when this activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();

        updateUI();
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
     * Executes all updateUI functions.
     */
    private void updateUI() {
        updateUIProductModelName(null);
        updateUITrafficControlConnectionState(null);
    }


    /**
     * This is called whenever a connection check of / to the Traffic Control is in progress. This
     * will set the text view to "checking connection...".
     *
     * @param event Event.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void trafficControlConnectionCheckInProgress(ConnectionCheckInProgress event) {
        TextView txtView_trafficControlConnectionState = findViewById(R.id.txtView_trafficControlConnectionState);
        txtView_trafficControlConnectionState.setText(R.string.trafficControl_checking_connection);
    }

    /**
     * This is called whenever the connection state of / to the Traffic Control changes. Sets the
     * text view to the Traffic Control version or "not connected".
     *
     * @param event Event.
     */
    @Subscribe
    public void updateUITrafficControlConnectionState(ConnectionEvent event) {
        runOnUiThread(() -> {
            TextView txtView_trafficControlConnectionState = findViewById(R.id.txtView_trafficControlConnectionState);

            String version = trafficControlManager.getTrafficControlVersion();

            if(version != null) {
                txtView_trafficControlConnectionState.setText(version);
            } else {
                txtView_trafficControlConnectionState.setText(R.string.trafficControl_not_connected);
            }
        });
    }


    /**
     * This is called whenever tge connection state of the drone changes. Sets the text view of the
     * product model name.
     *
     * @param event Event.
     */
    @Subscribe
    public void updateUIProductModelName(ProductConnectivityChange event) {
        runOnUiThread(() -> {
            TextView txtView_productModelName = findViewById(R.id.txtView_productModelNameConnectionState);

            String modelName = djiManager.getModelName();

            if(modelName != null) {
                txtView_productModelName.setText(modelName);
            } else {
                txtView_productModelName.setText(R.string.product_not_connected);
            }
        });
    }


    /**
     * Shows a toast message.
     *
     * @param toastMessage toastMessage.
     */
    @Subscribe
    public void showToast(ToastMessage toastMessage) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), toastMessage.message, toastMessage.toastLength).show();
        });
    }

}
