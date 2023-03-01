package com.andreasmenzel.adds_dji;

import static java.lang.Math.round;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// Events
import com.andreasmenzel.adds_dji.Events.InformationHolder.AircraftLocationChanged;
import com.andreasmenzel.adds_dji.Events.InformationHolder.AircraftPowerChanged;

// Information Holder
import com.andreasmenzel.adds_dji.Events.ProductConnectivityChange.ProductConnectivityChange;
import com.andreasmenzel.adds_dji.Events.ProductModelChanged;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;

// Manager
import com.andreasmenzel.adds_dji.Managers.DJIManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays information about the drone, e.g. AircraftLocation, AircraftPower, ... This activity is
 * intended to be used for debugging and testing to make sure that the app can correctly get all
 * important (sensor) data from the drone.
 */
public class DroneInfoActivity extends AppCompatActivity {

    private static final EventBus bus = EventBus.getDefault();

    private static DJIManager djiManager;


    /**
     * Initializes this activity: Gets the djiManager.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_info);

        djiManager = MApplication.getDjiManager();
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

        updateUIProductModelName(null);
        updateUIAircraftLocation(null);
        updateUIAircraftPower(null);
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
     * This is called whenever the product connectivity changes, e.g. the drone is turned on / off.
     * Updates the UI.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void productConnectivityChange(ProductConnectivityChange event) {
        updateUIProductModelName(null);
        updateUIAircraftLocation(null);
        updateUIAircraftPower(null);
    }


    /**
     * This is called whenever the product model name changes. Updates the model name in the UI.
     */
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
     * This is called whenever the AircraftLocation changes. Updates the AircraftLocation
     * information in the UI.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUIAircraftLocation(AircraftLocationChanged event) {
        AircraftLocation aircraftLocation = djiManager.getAircraftLocation();

        TextView txtView_gpsSignalLevel = findViewById(R.id.txtView_gpsSignalLevel);
        TextView txtView_gpsSatellitesConnected = findViewById(R.id.txtView_gpsSatellitesConnected);
        TextView txtView_gpsValid = findViewById(R.id.txtView_gpsValid);
        TextView txtView_gpsLat = findViewById(R.id.txtView_gpsLat);
        TextView txtView_gpsLon = findViewById(R.id.txtView_gpsLon);
        TextView txtView_altitude = findViewById(R.id.txtView_altitude);
        TextView txtView_velocityX = findViewById(R.id.txtView_velocityX);
        TextView txtView_velocityY = findViewById(R.id.txtView_velocityY);
        TextView txtView_velocityZ = findViewById(R.id.txtView_velocityZ);
        TextView txtView_pitch = findViewById(R.id.txtView_pitch);
        TextView txtView_yaw = findViewById(R.id.txtView_yaw);
        TextView txtView_roll = findViewById(R.id.txtView_roll);

        String gpsSignalLevel;
        String gpsSatellitesConnected;
        String gpsValid;
        String gpsLat;
        String gpsLon;
        String altitude;
        String velocityX;
        String velocityY;
        String velocityZ;
        String pitch;
        String yaw;
        String roll;

        if(djiManager.getModelName() != null) { // Drone connected
            gpsSignalLevel = aircraftLocation.getGpsSignalLevel() + " / 5";
            gpsSatellitesConnected = String.valueOf(aircraftLocation.getGpsSatellitesConnected());
            gpsValid = String.valueOf(aircraftLocation.getGpsValid());
            gpsLat = "LAT: " + (round(aircraftLocation.getGpsLat()*100000000) / 100000000.0);
            gpsLon = "LON: " + (round(aircraftLocation.getGpsLon()*100000000) / 100000000.0);
            altitude = aircraftLocation.getAltitude() + "m";
            velocityX = "N: " + aircraftLocation.getVelocityX() + "m/s";
            velocityY = "E: " + aircraftLocation.getVelocityY() + "m/s";
            velocityZ = "D: " + aircraftLocation.getVelocityZ() + "m/s";
            pitch = "P: " + aircraftLocation.getPitch() + "deg";
            yaw = "Y: " + aircraftLocation.getYaw() + "deg";
            roll = "R: " + aircraftLocation.getRoll() + "deg";
        } else { // Drone not connected.
            gpsSignalLevel = getString(R.string.gps_signal_level);
            gpsSatellitesConnected = getString(R.string.gps_satellites_connected);
            gpsValid = getString(R.string.gps_valid);
            gpsLat = getString(R.string.gps_lat);
            gpsLon = getString(R.string.gps_lon);
            altitude = getString(R.string.altitude);
            velocityX = getString(R.string.velocity_x);
            velocityY = getString(R.string.velocity_y);
            velocityZ = getString(R.string.velocity_z);
            pitch = getString(R.string.pitch);
            yaw = getString(R.string.yaw);
            roll = getString(R.string.roll);
        }


        txtView_gpsSignalLevel.setText(gpsSignalLevel);
        txtView_gpsSatellitesConnected.setText(gpsSatellitesConnected);
        txtView_gpsValid.setText(gpsValid);
        txtView_gpsLat.setText(gpsLat);
        txtView_gpsLon.setText(gpsLon);
        txtView_altitude.setText(altitude);
        txtView_velocityX.setText(velocityX);
        txtView_velocityY.setText(velocityY);
        txtView_velocityZ.setText(velocityZ);
        txtView_pitch.setText(pitch);
        txtView_yaw.setText(yaw);
        txtView_roll.setText(roll);
    }


    /**
     * This is called whenever the AircraftPower changes. Updates the AircraftPower information in
     * the UI.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateUIAircraftPower(AircraftPowerChanged event) {
        AircraftPower aircraftPower = djiManager.getAircraftPower();

        TextView txtView_batteryRemaining = findViewById(R.id.txtView_batteryRemaining);
        TextView txtView_batteryRemainingPercent = findViewById(R.id.txtView_batteryRemainingPercent);
        TextView txtView_remainingFlightTime = findViewById(R.id.txtView_remainingFlightTime);
        TextView txtView_remainingFlightRadius = findViewById(R.id.txtView_remainingFlightRadius);

        String batteryRemaining;
        String batteryRemainingPercent;
        String remainingFlightTime;
        String remainingFlightRadius;

        if(djiManager.getModelName() != null) { // Drone connected
            batteryRemaining = aircraftPower.getBatteryRemaining() + "mAh";
            batteryRemainingPercent = aircraftPower.getBatteryRemainingPercent() + "%";

            int tmp_remaining_flight_time = aircraftPower.getRemainingFlightTime();
            if(tmp_remaining_flight_time > 0) {
                int tmp_remaining_flight_time_minutes = tmp_remaining_flight_time / 60;
                int tmp_remaining_flight_time_seconds = tmp_remaining_flight_time - (tmp_remaining_flight_time_minutes * 60);

                remainingFlightTime = "";
                if(tmp_remaining_flight_time_minutes > 0) {
                    remainingFlightTime = tmp_remaining_flight_time_minutes + "min ";
                }
                remainingFlightTime += tmp_remaining_flight_time_seconds + "sec";
            } else {
                remainingFlightTime = "0s";
            }

            int tmp_remaining_flight_radius = (int) aircraftPower.getRemainingFlightRadius();
            if(tmp_remaining_flight_radius > 0) {
                int tmp_remaining_flight_radius_km = tmp_remaining_flight_radius / 1000;
                int tmp_remaining_flight_radius_m = tmp_remaining_flight_radius - (tmp_remaining_flight_radius_km * 1000);

                remainingFlightRadius = "";
                if(tmp_remaining_flight_radius_km > 0) {
                    remainingFlightRadius = tmp_remaining_flight_radius_km + "km ";
                }
                remainingFlightRadius += tmp_remaining_flight_radius_m + "m";
            } else {
                remainingFlightRadius = "0m";
            }
        } else { // Drone not connected
            batteryRemaining = getString(R.string.battery_remaining);
            batteryRemainingPercent = getString(R.string.battery_remaining_percent);
            remainingFlightTime = getString(R.string.remaining_flight_time);
            remainingFlightRadius = getString(R.string.remaining_flight_radius);
        }

        txtView_batteryRemaining.setText(batteryRemaining);
        txtView_batteryRemainingPercent.setText(batteryRemainingPercent);
        txtView_remainingFlightTime.setText(remainingFlightTime);
        txtView_remainingFlightRadius.setText(remainingFlightRadius);
    }


    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_LONG).show();
    }

}
