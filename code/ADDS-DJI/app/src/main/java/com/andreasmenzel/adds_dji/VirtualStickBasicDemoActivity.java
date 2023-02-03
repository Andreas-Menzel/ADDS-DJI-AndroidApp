package com.andreasmenzel.adds_dji;

import android.os.Bundle;
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


public class VirtualStickBasicDemoActivity extends AppCompatActivity {

    private EventBus bus = EventBus.getDefault();

    private DJIManager djiManager;


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
            djiManager.cancel();
        });
        findViewById(R.id.btn_startVirtualStick).setOnClickListener((View view) -> {
            djiManager.virtualStick();
        });
        findViewById(R.id.btn_virtualStickLeft).setOnClickListener((View view) -> {
            djiManager.virtualStickAddLeft();
        });
        findViewById(R.id.btn_virtualStickRight).setOnClickListener((View view) -> {
            djiManager.virtualStickAddRight();
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
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_SHORT).show();
    }

}
