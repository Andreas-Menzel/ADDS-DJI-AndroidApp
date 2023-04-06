package com.andreasmenzel.adds_dji;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ComponentChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnected;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductDisconnected;
import com.andreasmenzel.adds_dji.Events.ToastMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;


/**
 * When the app is started, this activity will be shown. It makes sure that everything being used
 * globally is initialized: DJI Mobile SDK, Managers (see "Managers" package).
 */
public class InitializeAppActivity extends AppCompatActivity {

    private static final EventBus bus = EventBus.getDefault();

    private static final String TAG = MainActivity.class.getName();

    private static final String[] REQUIRED_PERMISSIONS_LIST = new String[]{
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private final List<String> missingPermissions = new ArrayList<>();
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private final AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);


    /**
     * Initializes this activity: Gets the custom managers, checks the permissions, starts the
     * SDK registration and sets up the onClickListeners.
     *
     * @param savedInstanceState savedInstanceState.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize_app);

        SharedPreferences sharedPreferences = getSharedPreferences("MyDronePrefs", Context.MODE_PRIVATE);
        MApplication.setDroneId(sharedPreferences.getString("droneId", ""));
        setupCallbacks();

        // Make sure that the app has all required permissions. This also starts the DJI SDK
        // registration afterwards (if all permissions were granted).
        checkAndRequestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        MApplication.setDroneActive(false);

        EditText editText_droneId = findViewById(R.id.editTxt_droneId);
        editText_droneId.setText(MApplication.getDroneId());
    }

    /**
     * Sets up all the callbacks. E.g. onClickListeners
     */
    private void setupCallbacks() {
        findViewById(R.id.btn_goFly).setOnClickListener((View v) -> {
            EditText editText_droneId = findViewById(R.id.editTxt_droneId);

            MApplication.setDroneId(String.valueOf(editText_droneId.getText()));

            // Save drone id persistently
            SharedPreferences sharedPreferences = getSharedPreferences("MyDronePrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("droneId", MApplication.getDroneId());
            editor.apply();

            MApplication.setDroneActive(true);

            Intent switchActivityIntent = new Intent(this, MainActivity.class);
            startActivity(switchActivityIntent);
        });
    }


    /**
     * Checks if there are any missing permissions, and requests runtime permission(s) if needed.
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
            showToast(new ToastMessage("Permissions need to be granted to continue.", Toast.LENGTH_LONG));
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[missingPermissions.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request. Calls permissionsGranted() afterwards if all required
     * permissions were granted.
     *
     * @param requestCode requestCode.
     * @param permissions permissions.
     * @param grantResults grantResults.
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
            TextView txtView_msdk = findViewById(R.id.txtView_msdkRegistrationState);
            txtView_msdk.setText(R.string.msdkRegistration_missing_permissions);

            showToast(new ToastMessage("Some required permissions are still not granted:", Toast.LENGTH_LONG));
            for(int i = 0; i < missingPermissions.size(); i++) {
                showToast(new ToastMessage(missingPermissions.get(i), Toast.LENGTH_LONG));
            }
        }
    }

    /**
     * Execute startSDKRegistration()
     */
    private void permissionsGranted() {
        startSDKRegistration();
    }


    /**
     * Starts the SDK registration and sets up the DJI specific methods: onProductDisconnect(), ...
     */
    public void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            runOnUiThread(() -> {
                TextView txtView_msdk = findViewById(R.id.txtView_msdkRegistrationState);
                txtView_msdk.setText(R.string.msdkRegistration_registration_in_progress);
            });

            AsyncTask.execute(() -> {
                DJISDKManager.getInstance().registerApp(InitializeAppActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            runOnUiThread(() -> {
                                TextView txtView_msdk = findViewById(R.id.txtView_msdkRegistrationState);
                                txtView_msdk.setText(R.string.msdkRegistration_registered);
                            });

                            DJISDKManager.getInstance().startConnectionToProduct(); // TODO: Is this necessary?

                            readyToFly();
                        } else {
                            runOnUiThread(() -> {
                                TextView txtView_msdk = findViewById(R.id.txtView_msdkRegistrationState);
                                txtView_msdk.setText(R.string.msdkRegistration_registration_failed);
                            });

                            showToast(new ToastMessage("Registering sdk failed. Please check the bundle id and network connection!", Toast.LENGTH_LONG));
                        }
                    }

                    @Override
                    public void onProductDisconnect() {
                        showToast(new ToastMessage("Product disconnected", Toast.LENGTH_SHORT));
                        bus.post(new ProductDisconnected());
                    }
                    @Override
                    public void onProductConnect(BaseProduct baseProduct) {
                        showToast(new ToastMessage("Product connected", Toast.LENGTH_SHORT));
                        bus.post(new ProductConnected());
                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) {
                        showToast(new ToastMessage("Product changed: e.g. (dis)connected to drone", Toast.LENGTH_SHORT));
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


    /**
     * Initializes the managers (see "Managers" package) and enables the "Go Fly!" button.
     */
    private void readyToFly() {
        MApplication.initializeManagers();

        runOnUiThread(() -> {
            Button btn_goFly = findViewById(R.id.btn_goFly);

            btn_goFly.setEnabled(true);
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
