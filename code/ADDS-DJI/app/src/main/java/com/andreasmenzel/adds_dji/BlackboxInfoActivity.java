package com.andreasmenzel.adds_dji;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.andreasmenzel.adds_dji.Events.DJIManager.CreatedManagers;
import com.andreasmenzel.adds_dji.Events.InformationHolder.BlackboxDatasetChanged;
import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.InformationHolder.BlackboxDataset;
import com.andreasmenzel.adds_dji.Managers.BlackboxManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class BlackboxInfoActivity extends AppCompatActivity {

    private final EventBus bus = EventBus.getDefault();

    private BlackboxManager blackboxManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blackbox_info);

        blackboxManager = MApplication.getBlackboxManager();
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

        updateUI();
    }

    /**
     * Unregisters from the event bus.
     */
    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }


    @Subscribe
    public void createdManagers(CreatedManagers event) {
        blackboxManager = MApplication.getBlackboxManager();

        updateUI();
    }


    @Subscribe
    public void blackboxDatasetChanged(BlackboxDatasetChanged event) {
        updateUI();
    }


    private void updateUI() {
        runOnUiThread(() -> {
            if(blackboxManager == null) {
                return;
            }
            BlackboxDataset blackboxDataset = blackboxManager.getBlackboxDataset();

            TextView txtView_blackboxInfo_packetsSent = findViewById(R.id.txtView_blackboxInfo_packetsSent);
            TextView txtView_blackboxInfo_lastDatasetString = findViewById(R.id.txtView_blackboxInfo_lastDatasetString);

            String packetsSent = String.valueOf(blackboxManager.getRecordedDatasetsCounter());
            String lastDataset = blackboxDataset.getDatasetAsString();

            txtView_blackboxInfo_packetsSent.setText(packetsSent);
            txtView_blackboxInfo_lastDatasetString.setText(lastDataset);
        });
    }


    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_SHORT).show();
    }

}
