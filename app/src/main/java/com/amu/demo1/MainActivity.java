package com.amu.demo1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.product.Model;
import dji.common.remotecontroller.AircraftMappingStyle;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends Activity implements SurfaceTextureListener, OnClickListener {

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn,mDownLoadBtn;
    private ToggleButton mRecordBtn;
    private TextView recordingTime,con_mode,distanc,compas,speed,height,battery,sat_nu;
    private FlightController mFlightController;
    private Battery mBattery;
    private RemoteController mRemoteController;
    private Compass compass;
    private float compass_float = 0.0f,distance=0,horizontal_distance=0;
    private double droneLocationLat = 181, droneLocationLng = 181;
    private double home_droneLocationLat =181, home_droneLocationLng = 181;
    private float altitude = 100.0f,VelocityX=0,VelocityY=0,VelocityZ=0,Velocity=0;
    private int sat_num=0;
    private Camera camera,camera1;
    private Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        initUI();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        Camera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });

        }

    }

    protected void onProductChange() {
        initPreviewer();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        con_mode= (TextView) findViewById(R.id.con_mode);
        distanc= (TextView) findViewById(R.id.distance);
        compas= (TextView) findViewById(R.id.compass);
        speed= (TextView) findViewById(R.id.speed);
        height= (TextView) findViewById(R.id.height);
        battery= (TextView) findViewById(R.id.battery);
        sat_nu= (TextView) findViewById(R.id.sat_num);
        recordingTime = (TextView) findViewById(R.id.timer);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
        mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
        mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);
        mDownLoadBtn= (Button) findViewById(R.id.btn_download);
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        mCaptureBtn.setOnClickListener(this);
        mRecordBtn.setOnClickListener(this);
        mShootPhotoModeBtn.setOnClickListener(this);
        mRecordVideoModeBtn.setOnClickListener(this);
        mDownLoadBtn.setOnClickListener(this);

        recordingTime.setVisibility(View.INVISIBLE);

        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startRecord();
                } else {
                    stopRecord();
                }
            }
        });
    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
            mFlightController = ((Aircraft) product).getFlightController();
            mBattery =((Aircraft) product).getBattery();
            mRemoteController=((Aircraft) product).getRemoteController();
            compass=mFlightController.getCompass();
            camera1=((Aircraft) product).getCameras().get(1);
            camera=((Aircraft) product).getCameras().get(0);
        }

        if(mFlightController!=null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {
                    droneLocationLat = flightControllerState.getAircraftLocation().getLatitude();
                    droneLocationLng = flightControllerState.getAircraftLocation().getLongitude();
                    altitude = flightControllerState.getAircraftLocation().getAltitude();
                    home_droneLocationLat = flightControllerState.getHomeLocation().getLatitude();
                    home_droneLocationLng = flightControllerState.getHomeLocation().getLongitude();
                    horizontal_distance= AMapUtils.calculateLineDistance(new LatLng(droneLocationLat, droneLocationLng),new LatLng(home_droneLocationLat, home_droneLocationLng));
                    distance=(float) Math.sqrt(altitude*altitude+horizontal_distance*horizontal_distance);
                    if (null != compass) {
                        compass_float = compass.getHeading();
                    }
                    sat_num = flightControllerState.getSatelliteCount();
                    VelocityX=flightControllerState.getVelocityX();
                    VelocityY=flightControllerState.getVelocityY();
                    VelocityZ=flightControllerState.getVelocityZ();
                    Velocity=(float) Math.sqrt(VelocityX*VelocityX+VelocityY*VelocityY+VelocityZ*VelocityZ);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            distanc.setText("距离："+distance);
                            compas.setText("朝向："+compass_float);
                            speed.setText("速度："+Velocity);
                            height.setText("高度："+altitude);
                            sat_nu.setText("卫星数："+sat_num);
                        }
                    });
                }
            });
        }

        if (mBattery!=null){
            mBattery.setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState batteryState) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            battery.setText("电量："+batteryState.getChargeRemainingInPercent());
                        }
                    });

                }
            });
        }
        if(mRemoteController!=null){
            mRemoteController.getAircraftMappingStyle(new CommonCallbacks.CompletionCallbackWith<AircraftMappingStyle>() {
                @Override
                public void onSuccess(AircraftMappingStyle aircraftMappingStyle) {
                    switch (aircraftMappingStyle){
                        case STYLE_1:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    con_mode.setText("模式：日本手");
                                }
                            });
                            break;
                        case STYLE_2:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    con_mode.setText("模式：美国手");
                                }
                            });
                            break;
                        case STYLE_3:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    con_mode.setText("模式：中国手");
                                }
                            });
                            break;
                    }
                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
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
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_capture:{
                captureAction();
                break;
            }
            case R.id.btn_shoot_photo_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            }
            case R.id.btn_record_video_mode:{
                switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            }
            case R.id.btn_download:{
                Intent i=new Intent(MainActivity.this, MediaActivity.class);
                startActivity(i);
                finish();
                break;
            }
            default:
                break;
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
            }
    }

    // Method for taking photo
    private void captureAction(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError == null) {
                                                showToast("take photo: success");
                                            } else {
                                                showToast(djiError.getDescription());
                                            }
                                        }
                                    });
                                }
                            }, 2000);
                        }
                    }
            });
        }
    }

    // Method for starting recording
    private void startRecord(){

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError)
                {
                    if (djiError == null) {
                        showToast("Record video: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord(){

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){

                @Override
                public void onResult(DJIError djiError)
                {
                    if(djiError == null) {
                        showToast("Stop recording: success");
                    }else {
                        showToast(djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }

    }
}
