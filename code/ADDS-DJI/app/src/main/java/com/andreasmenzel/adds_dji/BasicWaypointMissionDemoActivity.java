package com.andreasmenzel.adds_dji;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds_dji.Events.DJIManager.UIUpdated;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.Managers.DJIManager;
import com.andreasmenzel.adds_dji.OperationModes.OperationMode;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionPause;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionResume;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionStart;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionStop;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionUpload;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;

public class BasicWaypointMissionDemoActivity extends AppCompatActivity {

    private final EventBus bus = EventBus.getDefault();

    private DJIManager djiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_basic_waypoint_mission_demo);

        djiManager = MApplication.getDjiManager();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);

        setupOnClickListeners();

        djiManager.controlDrone();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateUI(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }


    private void setupOnClickListeners() {
        findViewById(R.id.btn_bwpmd_add).setOnClickListener((View view) -> {
            // Add waypoint
            AircraftLocation aircraftLocation = djiManager.getAircraftLocation();
            DJIManager.waypointMissionAddWaypoint(aircraftLocation.getGpsLat(), aircraftLocation.getGpsLon(), aircraftLocation.getAltitude());
        });

        findViewById(R.id.btn_bwpmd_upload).setOnClickListener((View view) -> {
            // Upload mission
            DJIManager.changeOperationMode(new WaypointMissionUpload());
        });

        findViewById(R.id.btn_bwpmd_start).setOnClickListener((View view) -> {
            // Start mission
            DJIManager.changeOperationMode(new WaypointMissionStart());
        });
        findViewById(R.id.btn_bwpmd_stop).setOnClickListener((View view) -> {
            // Stop mission
            DJIManager.changeOperationMode(new WaypointMissionStop());
        });
        findViewById(R.id.btn_bwpmd_resume).setOnClickListener((View view) -> {
            // Resume mission
            DJIManager.changeOperationMode(new WaypointMissionResume());
        });
        findViewById(R.id.btn_bwpmd_pause).setOnClickListener((View view) -> {
            // Pause mission
            DJIManager.changeOperationMode(new WaypointMissionPause());
        });
        findViewById(R.id.btn_bwpm_clearWaypoints).setOnClickListener((View view) -> {
            // Clear waypoints
            djiManager.cancel();
            DJIManager.waypointMissionClearWaypoints();
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUI(UIUpdated event) {
        TextView txtView_operationMode = findViewById(R.id.txtView_bwpmd_operationMode);
        TextView txtView_operationModeState = findViewById(R.id.txtView_bwpmd_operationModeState);

        OperationMode operationMode = djiManager.getHighLevelOperationMode();

        txtView_operationMode.setText(operationMode.toString());
        txtView_operationModeState.setText(operationMode.getState().toString());


        TextView txtView_wpm_nowp = findViewById(R.id.txtView_bwpmd_nowp);
        txtView_wpm_nowp.setText(String.valueOf(DJIManager.getWaypointMissionBuilder().getWaypointCount()));
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_SHORT).show();
    }

}
