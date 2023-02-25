package com.andreasmenzel.adds_dji;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import com.andreasmenzel.adds_dji.Events.ToastMessage;
import com.andreasmenzel.adds_dji.Manager.DJIManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

public class FPVDemoActivity extends Activity implements TextureView.SurfaceTextureListener, View.OnClickListener {

    private static final String TAG = FPVDemoActivity.class.getName();


    protected TextureView mVideoSurface = null;


    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    DJICodecManager mCodecManager = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fpvdemo_activity);
        initUI();
    }



    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

        if (mVideoSurface != null) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
    }



    /**
     * Shows a toast message.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showToast(ToastMessage toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage.message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View view) {

    }
}