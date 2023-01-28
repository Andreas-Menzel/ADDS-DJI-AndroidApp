package com.andreasmenzel.adds_dji;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.andreasmenzel.adds_dji.Events.*;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ComponentChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnected;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductDisconnected;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Connectivity.TrafficSystemConnectionCheckInProgeress;
import com.andreasmenzel.adds_dji.Events.TrafficSystem.Connectivity.TrafficSystemConnectionEvent;
import com.andreasmenzel.adds_dji.Manager.DJIManager;
import com.andreasmenzel.adds_dji.Manager.TrafficSystemManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class MainActivity extends AppCompatActivity {

    private static EventBus bus = EventBus.getDefault();

    private static final String TAG = MainActivity.class.getName();
    private Handler handler;

    private static final String[] REQUIRED_PERMISSIONS_LIST = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermissions = new ArrayList<>();
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    private DJIManager djiManager;
    private TrafficSystemManager trafficSystemManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        djiManager = MApplication.getDjiManager();
        trafficSystemManager = MApplication.getTrafficSystemManager();

        // Make sure that the app has all required permissions. This also starts the DJI SDK
        // registration afterwards (if all permissions were granted).
        checkAndRequestPermissions();


        trafficSystemManager.checkConnection(true);


        findViewById(R.id.button).setOnClickListener((View view) -> {
            showDroneInfoActivity();
        });
        findViewById(R.id.btn_testFlightModes).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, TestFlightModesActivity.class);
            startActivity(switchActivityIntent);
        });

        findViewById(R.id.btn_start).setOnClickListener((View view) -> {
            trafficSystemManager.startAutoCommunicationTell();
        });
        findViewById(R.id.btn_stop).setOnClickListener((View view) -> {
            trafficSystemManager.stopAutoCommunicationTell();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }


    /**
     * Checks if there are any missing permissions, and
     * requests runtime permission(s) if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSIONS_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(eachPermission);
            }
        }

        // Request for missing permissions
        if (missingPermissions.isEmpty()) {
            permissionsGranted();
        } else {
            bus.post(new ToastMessage("Permissions need to be granted to continue."));
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[missingPermissions.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.remove(permissions[i]);
                }
            }
        }

        // If there is enough permission, we will start the registration
        if (missingPermissions.isEmpty()) {
            permissionsGranted();
        } else {
            TextView txtView_msdk = findViewById(R.id.txtView_msdk);
            txtView_msdk.setText(R.string.msdkRegistration_missing_permissions);

            bus.post(new ToastMessage("Some required permissions are still not granted!"));
            for(int i = 0; i < missingPermissions.size(); i++) {
                bus.post(new ToastMessage(missingPermissions.get(i)));
            }
        }
    }

    private void permissionsGranted() {
        // TODO: show text on screen: permissions missing?
        startSDKRegistration();
    }


    public void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            handler.post(() -> {
                TextView txtView_msdk = findViewById(R.id.txtView_msdk);
                txtView_msdk.setText(R.string.msdkRegistration_registration_in_progress);
            });

            AsyncTask.execute(() -> {
                DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            handler.post(() -> {
                                TextView txtView_msdk = findViewById(R.id.txtView_msdk);
                                txtView_msdk.setText(R.string.msdkRegistration_registered);
                            });

                            DJISDKManager.getInstance().startConnectionToProduct(); // TODO: button for this if not connected to drone?
                            bus.post(new SdkRegistered());
                        } else {
                            handler.post(() -> {
                                TextView txtView_msdk = findViewById(R.id.txtView_msdk);
                                txtView_msdk.setText(R.string.msdkRegistration_registration_failed);
                            });

                            bus.post(new ToastMessage("Registering sdk failed. Please check the bundle id and network connection!"));
                        }
                    }

                    @Override
                    public void onProductDisconnect() {
                        bus.post(new ToastMessage("Product disconnected"));
                        bus.post(new ProductDisconnected());
                    }
                    @Override
                    public void onProductConnect(BaseProduct baseProduct) {
                        bus.post(new ToastMessage("Product connected"));
                        bus.post(new ProductConnected());
                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) {
                        bus.post(new ToastMessage("Product changed"));
                        bus.post(new ProductChanged());
                    }

                    @Override
                    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                  BaseComponent newComponent) {
                        if (newComponent != null) {
                            newComponent.setComponentListener(isConnected -> {
                                Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
                                bus.post(new ComponentChanged());
                            });
                        }
                        Log.d(TAG,
                                String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                        componentKey,
                                        oldComponent,
                                        newComponent));

                    }

                    @Override
                    public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                    }

                    @Override
                    public void onDatabaseDownloadProgress(long l, long l1) {

                    }

                });
            });
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_LONG).show();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void trafficSystemConnectionCheckInProgress(TrafficSystemConnectionCheckInProgeress event) {
        TextView txtView_trafficSystem = findViewById(R.id.txtView_trafficSystem);
        txtView_trafficSystem.setText(R.string.trafficSystem_checking_connection);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void trafficSystemNowConnectionEvent(TrafficSystemConnectionEvent event) {
        TextView txtView_trafficSystem = findViewById(R.id.txtView_trafficSystem);

        String version = trafficSystemManager.getTrafficSystemVersion();

        if(version != null) {
            txtView_trafficSystem.setText(version);
        } else {
            txtView_trafficSystem.setText(R.string.trafficSystem_not_connected);
        }
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void productModelChanged(ProductModelChanged change) {
        TextView txtView_productModelName = findViewById(R.id.txtView_productModelName);

        String modelName = djiManager.getModelName();

        if(modelName != null) {
            txtView_productModelName.setText(modelName);
        } else {
            txtView_productModelName.setText(R.string.product_not_connected);
        }
    }


    public void showDroneInfoActivity() {
        Intent switchActivityIntent = new Intent(this, DroneInfoActivity.class);
        startActivity(switchActivityIntent);
    }

}
