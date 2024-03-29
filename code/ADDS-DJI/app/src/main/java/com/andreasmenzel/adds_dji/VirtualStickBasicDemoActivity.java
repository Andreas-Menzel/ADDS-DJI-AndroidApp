package com.andreasmenzel.adds_dji;

import android.os.Bundle;
import android.os.Handler;
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


public class VirtualStickBasicDemoActivity extends AppCompatActivity {

    private EventBus bus = EventBus.getDefault();

    private DJIManager djiManager;

    private static final Handler stickDataSenderHandler = new Handler();

    private float pitch = 0;
    private float yaw = 0;
    private float roll = 0;
    private float verticalThrottle = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_virtual_stick_basic_demo);

        djiManager = MApplication.getDjiManager();


        findViewById(R.id.btn_vsbd_takeOff).setOnClickListener((View view) -> {
            djiManager.takeOff();
        });
        findViewById(R.id.btn_vsbd_land).setOnClickListener((View view) -> {
            djiManager.land();
        });

        findViewById(R.id.btn_vsbd_cancel).setOnClickListener((View view) -> {
            stickDataSenderHandler.removeCallbacksAndMessages(null);
            pitch = 0;
            yaw = 0;
            roll = 0;
            verticalThrottle = 0;
            djiManager.cancel();
            bus.post(new UIUpdated());
        });

        findViewById(R.id.btn_vsbd_startVirtualStick).setOnClickListener((View view) -> {
            djiManager.virtualStick();
            sendStickData();
            bus.post(new UIUpdated());
        });

        findViewById(R.id.btn_vsbd_virtualStickAddLeft).setOnClickListener((View view) -> {
            roll = roll - (float)0.1;
            if(roll < -1) roll = 1;
            bus.post(new UIUpdated());
        });
        findViewById(R.id.btn_vsbd_virtualStickAddRight).setOnClickListener((View view) -> {
            roll = roll + (float)0.1;
            if(roll > 1) roll = 1;
            bus.post(new UIUpdated());
        });
        findViewById(R.id.btn_vsbd_virtualStickAddFront).setOnClickListener((View view) -> {
            pitch = pitch + (float)0.1;
            if(pitch > 1) pitch = 1;
            bus.post(new UIUpdated());
        });
        findViewById(R.id.btn_vsbd_virtualStickAddBack).setOnClickListener((View view) -> {
            pitch = pitch - (float)0.1;
            if(pitch < -1) pitch = -1;
            bus.post(new UIUpdated());
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
        TextView txtView_OperationMode = findViewById(R.id.txtView_vsbd_operationMode);
        TextView txtView_operationModeState = findViewById(R.id.txtView_vsbd_operationModeState);

        OperationMode operationMode = djiManager.getHighLevelOperationMode();

        txtView_OperationMode.setText(operationMode.toString());
        txtView_operationModeState.setText(operationMode.getState().toString());


        TextView txtView_roll = findViewById(R.id.txtView_vsbd_virtualStickRollValue);
        TextView txtView_pitch = findViewById(R.id.txtView_vsbd_virtualStickPitchValue);

        txtView_roll.setText(String.valueOf(Math.round(roll * 10) / 10.0));
        txtView_pitch.setText(String.valueOf(Math.round(pitch * 10) / 10.0));
    }


    private void sendStickData() {
        stickDataSenderHandler.removeCallbacksAndMessages(null);

        djiManager.setVirtualSticks(pitch, yaw, roll, verticalThrottle);

        stickDataSenderHandler.postDelayed(this::sendStickData, 50);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_SHORT).show();
    }

}
