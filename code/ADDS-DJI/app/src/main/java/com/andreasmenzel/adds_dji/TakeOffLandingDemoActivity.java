package com.andreasmenzel.adds_dji;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds_dji.Events.DJIManager.UIUpdated;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Managers.DJIManager;
import com.andreasmenzel.adds_dji.OperationModes.OperationMode;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class TakeOffLandingDemoActivity extends AppCompatActivity {

    private EventBus bus = EventBus.getDefault();

    private DJIManager djiManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_takeoff_landing_demo);

        djiManager = MApplication.getDjiManager();


        findViewById(R.id.btn_tld_takeOff).setOnClickListener((View view) -> {
            djiManager.takeOff();
        });
        findViewById(R.id.btn_tld_land).setOnClickListener((View view) -> {
            djiManager.land();
        });
        findViewById(R.id.btn_tld_takeOffLand).setOnClickListener((View view) -> {
            djiManager.takeOffLand();
        });
        findViewById(R.id.btn_tld_cancel).setOnClickListener((View view) -> {
            djiManager.cancel();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);

        djiManager.controlDrone();
    }


    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUI(UIUpdated event) {
        TextView txtView_operationMode = findViewById(R.id.txtView_tld_operationMode);
        TextView txtView_operationModeState = findViewById(R.id.txtView_tld_operationModeState);

        OperationMode operationMode = djiManager.getHighLevelOperationMode();

        txtView_operationMode.setText(operationMode.toString());
        txtView_operationModeState.setText(operationMode.getState().toString());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_SHORT).show();
    }

}
