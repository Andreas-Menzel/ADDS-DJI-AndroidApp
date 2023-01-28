package com.andreasmenzel.adds_dji;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds_dji.Events.LocationChanged;
import com.andreasmenzel.adds_dji.InformationHolder.AircraftLocation;
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
    public void updateLocation(LocationChanged changed) {
        AircraftLocation aircraftLocation = djiManager.getAircraftLocation();

        TextView txtView_gpsStrength = findViewById(R.id.txtView_gpsStrength);
        TextView txtView_gpsLat = findViewById(R.id.txtView_gpsLat);
        TextView txtView_gpsLon = findViewById(R.id.txtView_gpsLon);
        TextView txtView_altitude = findViewById(R.id.txtView_altitude);
        TextView txtView_pitch = findViewById(R.id.txtView_pitch);
        TextView txtView_yaw = findViewById(R.id.txtView_yaw);
        TextView txtView_roll = findViewById(R.id.txtView_roll);

        txtView_gpsStrength.setText(String.valueOf(aircraftLocation.getGpsSignalLevel()));
        txtView_gpsLat.setText(String.valueOf(aircraftLocation.getGpsLat()));
        txtView_gpsLon.setText(String.valueOf(aircraftLocation.getGpsLon()));
        txtView_altitude.setText(String.valueOf(aircraftLocation.getAltitude()));
        txtView_pitch.setText(String.valueOf(aircraftLocation.getPitch()));
        txtView_yaw.setText(String.valueOf(aircraftLocation.getYaw()));
        txtView_roll.setText(String.valueOf(aircraftLocation.getRoll()));
    }

}
