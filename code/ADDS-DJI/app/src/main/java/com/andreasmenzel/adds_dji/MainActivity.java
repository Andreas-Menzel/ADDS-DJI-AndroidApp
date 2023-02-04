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

// Events
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ComponentChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductChanged;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnected;
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductDisconnected;
import com.andreasmenzel.adds_dji.Events.ProductModelChanged;
import com.andreasmenzel.adds_dji.Events.SdkRegistered;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.ConnectionCheckInProgress;
import com.andreasmenzel.adds_dji.Events.TrafficControl.Connectivity.ConnectionEvent;

// Manager
import com.andreasmenzel.adds_dji.Manager.DJIManager;
import com.andreasmenzel.adds_dji.Manager.TrafficControlManager;

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
 * The main activity. This is shown when the app is started.
 */
public class MainActivity extends AppCompatActivity {

    private static final EventBus bus = EventBus.getDefault();

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
    private final List<String> missingPermissions = new ArrayList<>();
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private final AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    private DJIManager djiManager;
    private TrafficControlManager trafficControlManager;


    /**
     * Initializes this activity: Gets the custom managers, checks the permissions, starts the
     * SDK registration and sets up the onClickListeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        // Get custom managers
        djiManager = MApplication.getDjiManager();
        trafficControlManager = MApplication.getTrafficControlManager();

        // Make sure that the app has all required permissions. This also starts the DJI SDK
        // registration afterwards (if all permissions were granted).
        checkAndRequestPermissions();


        findViewById(R.id.btn_showDroneInfoActivity).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, DroneInfoActivity.class);
            startActivity(switchActivityIntent);
        });
        findViewById(R.id.btn_showTestFlightModesActivity).setOnClickListener((View view) -> {
            Intent switchActivityIntent = new Intent(this, TestFlightModesActivity.class);
            startActivity(switchActivityIntent);
        });
    }

    /**
     * Registers to the event bus.
     */
    @Override
    protected void onStart() {
        super.onStart();
        bus.register(this);
    }

    /**
     * Makes sure that the UI is updated when this activity resumes.
     */
    @Override
    protected void onResume() {
        super.onResume();

        updateUITrafficControlConnectionState(null);
        updateUIProductModelName(null);
    }

    /**
     * Unregisters from the event bus.
     */
    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
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
            bus.post(new ToastMessage("Permissions need to be granted to continue."));
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[missingPermissions.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request. Calls permissionsGranted() afterwards if all required
     * permissions were granted.
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

            bus.post(new ToastMessage("Some required permissions are still not granted!"));
            for(int i = 0; i < missingPermissions.size(); i++) {
                bus.post(new ToastMessage(missingPermissions.get(i)));
            }
        }
    }

    private void permissionsGranted() {
        startSDKRegistration();
    }


    /**
     * Starts the SDK registration and sets up the DJI specific methods: onProductDisconnect(), ...
     */
    public void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            handler.post(() -> {
                TextView txtView_msdk = findViewById(R.id.txtView_msdkRegistrationState);
                txtView_msdk.setText(R.string.msdkRegistration_registration_in_progress);
            });

            AsyncTask.execute(() -> {
                DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            handler.post(() -> {
                                TextView txtView_msdk = findViewById(R.id.txtView_msdkRegistrationState);
                                txtView_msdk.setText(R.string.msdkRegistration_registered);
                            });

                            DJISDKManager.getInstance().startConnectionToProduct(); // TODO: Is this necessary?
                            bus.post(new SdkRegistered());
                        } else {
                            handler.post(() -> {
                                TextView txtView_msdk = findViewById(R.id.txtView_msdkRegistrationState);
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
                        bus.post(new ToastMessage("Product changed: e.g. (dis)connected to drone"));
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
     * This is called whenever a connection check of / to the Traffic Control is in progress. This
     * will set the text view to "checking connection..."
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void trafficControlConnectionCheckInProgress(ConnectionCheckInProgress event) {
        TextView txtView_trafficControl = findViewById(R.id.txtView_trafficControlConnectionState);
        txtView_trafficControl.setText(R.string.trafficControl_checking_connection);
    }

    /**
     * This is called whenever the connection state of / to the Traffic Control changes. Sets the
     * text view to the Traffic Control version or "not connected"
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUITrafficControlConnectionState(ConnectionEvent event) {
        TextView txtView_trafficControl = findViewById(R.id.txtView_trafficControlConnectionState);

        String version = trafficControlManager.getTrafficControlVersion();

        if(version != null) {
            txtView_trafficControl.setText(version);
        } else {
            txtView_trafficControl.setText(R.string.trafficControl_not_connected);
        }
    }


    // TODO: change to productConnectionStateCHanged
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUIProductModelName(ProductModelChanged event) {
        TextView txtView_productModelName = findViewById(R.id.txtView_productModelNameConnectionState);

        String modelName = djiManager.getModelName();

        if(modelName != null) {
            txtView_productModelName.setText(modelName);
        } else {
            txtView_productModelName.setText(R.string.product_not_connected);
        }
    }


    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_LONG).show();
    }

}
