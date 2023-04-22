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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AdvancedMissionDemoActivity extends AppCompatActivity {

    private final EventBus bus = EventBus.getDefault();

    private DJIManager djiManager;
    private MissionManager missionManager;

    private String destIntersectionId = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_advanced_mission_demo);

        djiManager = MApplication.getDjiManager();
        missionManager = MApplication.getMissionManager();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);

        setupOnClickListeners();
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
        findViewById(R.id.btn_amd_start).setOnClickListener((View view) -> {
            EditText editText_amd_destIntersection = findViewById(R.id.editText_amd_destIntersection);
            String destinationIntersectionId = String.valueOf(editText_amd_destIntersection.getText());
            Intersection destinationIntersection = MApplication.getInfrastructureManager().getIntersection(destinationIntersectionId);

            if(destinationIntersection != null) {
                missionManager.setMissionDestination(destinationIntersection, true);
            } else {
                bus.post(new ToastMessage("Intersection \"" + destinationIntersectionId + "\" not found."));
            }
        });
        findViewById(R.id.btn_amd_stop).setOnClickListener((View view) -> {
            missionManager.stopMission();
        });
        findViewById(R.id.btn_amd_resume).setOnClickListener((View view) -> {
            missionManager.resumeMission();
        });
        findViewById(R.id.btn_amd_pause).setOnClickListener((View view) -> {
            missionManager.pauseMission();
        });
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUI(UIUpdated event) {
        TextView txtView_operationMode = findViewById(R.id.txtView_amd_operationMode);
        TextView txtView_operationModeState = findViewById(R.id.txtView_amd_operationModeState);

        OperationMode operationMode = djiManager.getHighLevelOperationMode();

        txtView_operationMode.setText(operationMode.toString());
        txtView_operationModeState.setText(operationMode.getState().toString());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_SHORT).show();
    }

}
