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
import com.andreasmenzel.adds_dji.OperationModes.Hovering;
import com.andreasmenzel.adds_dji.OperationModes.Landing;
import com.andreasmenzel.adds_dji.OperationModes.OnGround;
import com.andreasmenzel.adds_dji.OperationModes.StartVirtualStick;
import com.andreasmenzel.adds_dji.OperationModes.TakeOff;
import com.andreasmenzel.adds_dji.OperationModes.UseVirtualStick;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class VirtualStickCrossDemoActivity extends AppCompatActivity {

    private final EventBus bus = EventBus.getDefault();

    private DJIManager djiManager;

    private static final Handler stickDataSenderHandler = new Handler();
    private static final Handler nextProgressStateHandler = new Handler();

    private float pitch = 0;
    private float yaw = 0;
    private float roll = 0;
    private float verticalThrottle = 0;

    private static float activeValue = (float)0.2;
    private static int activeDuration = 1000;

    // 0: center -> front
    // 1: pause
    // 2: front -> back
    // 3: pause
    // 4: back -> center
    // 5: pause
    // 6: center -> left
    // 7: pause
    // 8: left -> right
    // 9: pause
    // 10: right -> center
    private int crossProgress = 0;

    // -1: Only cross performance active. Not the entire performance.
    // 0: TakeOff
    // 1: pause
    // 2: cross performance
    // 3: pause
    // 4: Landing
    private int performanceProgress = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_virtual_stick_cross_demo);

        djiManager = MApplication.getDjiManager();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);

        djiManager.controlDrone();
        setupOnClickListeners();
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

        OperationMode operationMode = djiManager.getHighLevelOperationMode();

        txtView_highLevelFlightMode.setText(operationMode.toString());
        txtView_flightModeState.setText(operationMode.getState().toString());


        TextView txtView_roll = findViewById(R.id.txtView_virtualStickRollValue);
        TextView txtView_pitch = findViewById(R.id.txtView_virtualStickPitchValue);

        txtView_roll.setText(String.valueOf(Math.round(roll * 10) / 10.0));
        txtView_pitch.setText(String.valueOf(Math.round(pitch * 10) / 10.0));


        TextView txtView_activeValue = findViewById(R.id.txtView_activeValue);
        TextView txtView_activeDuration = findViewById(R.id.txtView_activeDuration);

        txtView_activeValue.setText(String.valueOf(Math.round(activeValue * 10) / 10.0));
        txtView_activeDuration.setText(String.valueOf(activeDuration));
    }


    private void setupOnClickListeners() {
        findViewById(R.id.btn_takeOff).setOnClickListener((View view) -> {
            djiManager.takeOff();
        });
        findViewById(R.id.btn_land).setOnClickListener((View view) -> {
            djiManager.land();
        });

        findViewById(R.id.btn_cancel).setOnClickListener((View view) -> {
            nextProgressStateHandler.removeCallbacksAndMessages(null);
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

        findViewById(R.id.btn_decreaseActiveValue).setOnClickListener((View view) -> {
            activeValue -= 0.1;
            if(activeValue < 0) activeValue = 0;
            bus.post(new UIUpdated());
        });
        findViewById(R.id.btn_increaseActiveValue).setOnClickListener((View view) -> {
            activeValue += 0.1;
            if(activeValue > 1) activeValue = 1;
            bus.post(new UIUpdated());
        });

        findViewById(R.id.btn_decreaseActiveDuration).setOnClickListener((View view) -> {
            activeDuration -= 100;
            if(activeDuration < 100) activeDuration = 100;
            bus.post(new UIUpdated());
        });
        findViewById(R.id.btn_increaseActiveDuration).setOnClickListener((View view) -> {
            activeDuration += 100;
            if(activeDuration > 10000) activeDuration = 10000;
            bus.post(new UIUpdated());
        });

        findViewById(R.id.btn_startVirtualStickCrossMode).setOnClickListener((View view) -> {
            if(djiManager.getHighLevelOperationMode() instanceof StartVirtualStick || djiManager.getHighLevelOperationMode() instanceof UseVirtualStick) {
                performanceProgress = -1;
                crossProgress = 0;
                nextProgressState();
            } else {
                bus.post(new ToastMessage("Virtual Stick must be started first."));
            }
        });

        findViewById(R.id.btn_startCompleteVirtualStickCrossPerformance).setOnClickListener((View view) -> {
            crossProgress = 0;
            performanceProgress = 0;
            sendStickData();
            nextProgressState();
            bus.post(new UIUpdated());
        });
    }


    private void sendStickData() {
        stickDataSenderHandler.removeCallbacksAndMessages(null);

        djiManager.setVirtualSticks(pitch, yaw, roll, verticalThrottle);

        stickDataSenderHandler.postDelayed(this::sendStickData, 50);
    }


    private void nextProgressState() {
        nextProgressStateHandler.removeCallbacksAndMessages(null); // just in case

        // -1: Only cross performance active. Not the entire performance.
        // 0: TakeOff
        // 1: pause
        // 2: cross performance
        // 3: pause
        // 4: Landing
        // 5: Stop performance
        int delayToNextCall = 0;
        OperationMode operationMode = null;
        switch(performanceProgress) {
            case 1:
            case 3:
                // Pause
                delayToNextCall = 3000;
                performanceProgress++;
                break;
            case 0:
                // TakeOff
                operationMode = djiManager.getHighLevelOperationMode();

                if(operationMode instanceof TakeOff) {
                    // wait
                } else if(operationMode instanceof Hovering) {
                    // TakeOff complete
                    performanceProgress++;
                    delayToNextCall = 3000;
                } else {
                    djiManager.changeOperationMode(new TakeOff());
                }
                break;
            case 4:
                // Landing
                operationMode = djiManager.getHighLevelOperationMode();

                if(operationMode instanceof Landing) {
                    // wait
                } else if(operationMode instanceof OnGround) {
                    // TakeOff complete
                    performanceProgress = 5; // stop performance
                } else {
                    djiManager.changeOperationMode(new Landing());
                }
                break;
            case -1:
                delayToNextCall = flyCrossManouvre();

                if(crossProgress > 11) { // cross finished
                    performanceProgress = 5; // stop performance
                }
                break;
            case 2:
                delayToNextCall = flyCrossManouvre();

                if(crossProgress > 11) {
                    performanceProgress++;
                }
                break;
            case 5:
                bus.post(new ToastMessage("Stopping Performance"));

                // Stop performance
                nextProgressStateHandler.removeCallbacksAndMessages(null); // just in case
                stickDataSenderHandler.removeCallbacksAndMessages(null);
                djiManager.cancel();

                pitch = 0; // just in case
                yaw = 0;   // just in case
                roll = 0;  // just in case
                verticalThrottle = 0; // just in case
                bus.post(new UIUpdated());
                return;
        }

        nextProgressStateHandler.postDelayed(this::nextProgressState, delayToNextCall);
    }


    private int flyCrossManouvre() {
        OperationMode operationMode = djiManager.getHighLevelOperationMode();

        if(!(operationMode instanceof StartVirtualStick) && !(operationMode instanceof UseVirtualStick)) {
            djiManager.changeOperationMode(new UseVirtualStick());
        }

        // 0: center -> front
        // 1: pause
        // 2: front -> back
        // 3: pause
        // 4: back -> center
        // 5: pause
        // 6: center -> left
        // 7: pause
        // 8: left -> right
        // 9: pause
        // 10: right -> center
        // 11: finished

        int delayToNextCall = 0;
        switch(crossProgress) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
            case 11:
                // Pause: Do nothing
                pitch = 0;
                roll = 0;
                delayToNextCall = 3000;
                break;
            case 0:
                pitch = activeValue;
                roll = 0;
                delayToNextCall = activeDuration / 2;
                break;
            case 2:
                pitch = -activeValue;
                roll = 0;
                delayToNextCall = activeDuration;
                break;
            case 4:
                pitch = activeValue;
                roll = 0;
                delayToNextCall = activeDuration / 2;
                break;
            case 6:
                pitch = 0;
                roll = -activeValue;
                delayToNextCall = activeDuration / 2;
                break;
            case 8:
                pitch = 0;
                roll = activeValue;
                delayToNextCall = activeDuration;
                break;
            case 10:
                pitch = 0;
                roll = -activeValue;
                delayToNextCall = activeDuration / 2;
                break;
        }

        crossProgress++;

        return delayToNextCall;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_SHORT).show();
    }

}
