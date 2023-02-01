package com.andreasmenzel.adds_dji;

import static java.lang.Math.round;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds_dji.Events.InformationHolder.AircraftLocationChanged;
import com.andreasmenzel.adds_dji.Events.InformationHolder.AircraftPowerChanged;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftPower;
import com.andreasmenzel.adds_dji.Manager.DJIManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class DroneInfoActivity extends AppCompatActivity {

    EventBus bus = EventBus.getDefault();

    DJIManager djiManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        djiManager = MApplication.getDjiManager();

        setContentView(R.layout.activity_drone_info);
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


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateAircraftLocation(AircraftLocationChanged event) {
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

        String gpsSignalLevel = aircraftLocation.getGpsSignalLevel() + " / 5";
        String gpsSatellitesConnected = String.valueOf(aircraftLocation.getGpsSatellitesConnected());
        String gpsValid = String.valueOf(aircraftLocation.getGpsValid());
        String gpsLat = "LAT: " + (round(aircraftLocation.getGpsLat()*100000000) / 100000000.0);
        String gpsLon = "LON: " + (round(aircraftLocation.getGpsLon()*100000000) / 100000000.0);
        String altitude = aircraftLocation.getAltitude() + "m";
        String velocityX = "N: " + aircraftLocation.getVelocityX() + "m/s";
        String velocityY = "E: " + aircraftLocation.getVelocityY() + "m/s";
        String velocityZ = "D: " + aircraftLocation.getVelocityZ() + "m/s";

        txtView_gpsSignalLevel.setText(gpsSignalLevel);
        txtView_gpsSatellitesConnected.setText(gpsSatellitesConnected);
        txtView_gpsValid.setText(gpsValid);
        txtView_gpsLat.setText(gpsLat);
        txtView_gpsLon.setText(gpsLon);
        txtView_altitude.setText(altitude);
        txtView_velocityX.setText(velocityX);
        txtView_velocityY.setText(velocityY);
        txtView_velocityZ.setText(velocityZ);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updateAircraftPower(AircraftPowerChanged event) {
        AircraftPower aircraftPower = djiManager.getAircraftPower();

        TextView txtView_batteryRemaining = findViewById(R.id.txtView_batteryRemaining);
        TextView txtView_batteryRemainingPercent = findViewById(R.id.txtView_batteryRemainingPercent);
        TextView txtView_remainingFlightTime = findViewById(R.id.txtView_remainingFlightTime);
        TextView txtView_remainingFlightRadius = findViewById(R.id.txtView_remainingFlightRadius);

        String batteryRemaining = aircraftPower.getBatteryRemaining() + "mAh";
        String batteryRemainingPercent = aircraftPower.getBatteryRemainingPercent() + "%";
        String remainingFlightTime = aircraftPower.getRemainingFlightTime() + "s";
        String remainingFlightRadius = aircraftPower.getRemainingFlightRadius() + "m";

        txtView_batteryRemaining.setText(batteryRemaining);
        txtView_batteryRemainingPercent.setText(batteryRemainingPercent);
        txtView_remainingFlightTime.setText(remainingFlightTime);
        txtView_remainingFlightRadius.setText(remainingFlightRadius);
    }

}
