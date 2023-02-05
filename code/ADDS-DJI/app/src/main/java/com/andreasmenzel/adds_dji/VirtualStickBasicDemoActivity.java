package com.andreasmenzel.adds_dji;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds_dji.Events.DJIManager.UIUpdated;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Manager.DJIManager;
import com.andreasmenzel.adds_dji.Manager.HighLevelOperationModes.HighLevelOperationMode;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Text;


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


        findViewById(R.id.btn_takeOff).setOnClickListener((View view) -> {
            djiManager.takeOff();
        });
        findViewById(R.id.btn_land).setOnClickListener((View view) -> {
            djiManager.land();
        });

        findViewById(R.id.btn_cancel).setOnClickListener((View view) -> {
            stickDataSenderHandler.removeCallbacksAndMessages(null);
            pitch = 0;
            yaw = 0;
            roll = 0;
            verticalThrottle = 0;
            djiManager.cancel();
            bus.post(new UIUpdated());
        });

        findViewById(R.id.btn_startVirtualStick).setOnClickListener((View view) -> {
            djiManager.virtualStick();
            sendStickData();
            bus.post(new UIUpdated());
        });

        findViewById(R.id.btn_virtualStickAddLeft).setOnClickListener((View view) -> {
            roll = roll - (float)0.1;
            if(roll < -1) roll = 1;
            bus.post(new UIUpdated());
        });
        findViewById(R.id.btn_virtualStickAddRight).setOnClickListener((View view) -> {
            roll = roll + (float)0.1;
            if(roll > 1) roll = 1;
            bus.post(new UIUpdated());
        });
        findViewById(R.id.btn_virtualStickAddFront).setOnClickListener((View view) -> {
            pitch = pitch + (float)0.1;
            if(pitch > 1) pitch = 1;
            bus.post(new UIUpdated());
        });
        findViewById(R.id.btn_virtualStickAddBack).setOnClickListener((View view) -> {
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
        TextView txtView_highLevelFlightMode = findViewById(R.id.txtView_highLevelFlightMode);
        TextView txtView_flightModeState = findViewById(R.id.txtView_flightModeState);

        HighLevelOperationMode highLevelOperationMode = djiManager.getHighLevelOperationMode();

        txtView_highLevelFlightMode.setText(highLevelOperationMode.toString());
        txtView_flightModeState.setText(highLevelOperationMode.getMode().toString());


        TextView txtView_roll = findViewById(R.id.txtView_virtualStickRollValue);
        TextView txtView_pitch = findViewById(R.id.txtView_virtualStickPitchValue);

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
