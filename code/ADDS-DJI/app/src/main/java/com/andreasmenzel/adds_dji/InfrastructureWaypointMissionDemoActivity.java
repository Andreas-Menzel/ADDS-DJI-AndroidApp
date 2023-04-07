package com.andreasmenzel.adds_dji;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds_dji.Datasets.Corridor;
import com.andreasmenzel.adds_dji.Datasets.Intersection;
import com.andreasmenzel.adds_dji.Events.DJIManager.UIUpdated;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Managers.DJIManager;
import com.andreasmenzel.adds_dji.Managers.MissionManager;
import com.andreasmenzel.adds_dji.OperationModes.OperationMode;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionPause;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionResume;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionStop;
import com.andreasmenzel.adds_dji.OperationModes.WaypointMissionUpload;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class InfrastructureWaypointMissionDemoActivity extends AppCompatActivity {

    private final EventBus bus = EventBus.getDefault();

    private DJIManager djiManager;
    private MissionManager missionManager;

    private String endIntersectionId = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_infrastructure_waypoint_mission_demo);

        djiManager = MApplication.getDjiManager();
        missionManager = MApplication.getMissionManager();
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
        findViewById(R.id.btn_iswpmd_start).setOnClickListener((View view) -> {
            missionManager.setLandAfterMissionFinished(true);
            missionManager.startMission();
        });
        findViewById(R.id.btn_iswpmd_stop).setOnClickListener((View view) -> {
            missionManager.stopMission();
        });
        findViewById(R.id.btn_iswpmd_resume).setOnClickListener((View view) -> {
            missionManager.resumeMission();
        });
        findViewById(R.id.btn_iswpmd_pause).setOnClickListener((View view) -> {
            missionManager.pauseMission();
        });

        findViewById(R.id.btn_iswpmd_addCorridor).setOnClickListener((View view) -> {
            if(!endIntersectionId.equals("")) {
                String nextCorridorId = ((EditText)findViewById(R.id.editText_iswpmd_nextCorridor)).getText().toString();
                Corridor nextCorridor = MApplication.getInfrastructureManager().getCorridor(nextCorridorId);

                if(nextCorridor == null) {
                    bus.post(new ToastMessage("Corridor does not exist: " + nextCorridorId));
                    return;
                }

                if(MApplication.getInfrastructureManager().getCorridorsConnectedAtIntersection(endIntersectionId).contains(nextCorridor)) {
                    endIntersectionId = nextCorridor.getIntersectionAId().equals(endIntersectionId) ? nextCorridor.getIntersectionBId() : nextCorridor.getIntersectionAId();
                    missionManager.addCorridor(nextCorridor);

                    TextView txtView_missionPath = findViewById(R.id.txtView_iswpmd_missionPath);
                    txtView_missionPath.setText(txtView_missionPath.getText() + " -> " + nextCorridorId);
                } else {
                    bus.post(new ToastMessage("Corridor not connected to last intersection: " + endIntersectionId));
                }
            } else { // "Set start intersection"
                String startIntersectionId = ((EditText)findViewById(R.id.editText_iswpmd_nextCorridor)).getText().toString();
                Intersection startIntersection = MApplication.getInfrastructureManager().getIntersection(startIntersectionId);

                if(MApplication.getInfrastructureManager().getIntersection(startIntersectionId) != null) {
                    endIntersectionId = startIntersectionId;
                    missionManager.setStartIntersection(startIntersection);

                    TextView txtView_missionPath = findViewById(R.id.txtView_iswpmd_missionPath);
                    txtView_missionPath.setText("<" + startIntersectionId + ">");
                } else {
                    bus.post(new ToastMessage("Intersection does not exist: " + startIntersectionId));
                }
            }
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUI(UIUpdated event) {
        TextView txtView_operationMode = findViewById(R.id.txtView_iswpmd_operationMode);
        TextView txtView_operationModeState = findViewById(R.id.txtView_iswpmd_operationModeState);

        OperationMode operationMode = djiManager.getHighLevelOperationMode();

        txtView_operationMode.setText(operationMode.toString());
        txtView_operationModeState.setText(operationMode.getState().toString());

        TextView editText_nextCorridor = findViewById(R.id.editText_iswpmd_nextCorridor);

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_SHORT).show();
    }

}
