package com.amu.demo1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class waypoint_mission extends Activity implements View.OnClickListener, AMap.OnMapClickListener {

    protected static final String TAG = "MainActivity";

    private MapView mapView;
    private AMap aMap;
    private View infoWindow = null;
    private Route path;

    private Button locate, add, clear, load;
    private Button config, upload, start, stop,pause;
    private TextView label, remove;
    private CoordinateUtil coord = new CoordinateUtil();

    private boolean isAdd = false,pause_resume=true;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private final Map<Integer, Polyline> mPolylines = new ConcurrentHashMap<Integer, Polyline>();
    private List<LatLng> points = new ArrayList<LatLng>();
    private Marker droneMarker = null;
    private ArrayList<Marker> stations = new ArrayList<>();

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;
    private static double EarthRadius = 6378.137;

    private List<Waypoint> waypointList = new ArrayList<>();
    private List<LatLng> route = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mReceiver);
        removeListener();
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(waypoint_mission.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {

        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        load = (Button) findViewById(R.id.load);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        pause=(Button) findViewById(R.id.pause) ;

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        load.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
        pause.setOnClickListener(this);
    }

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        aMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.waypoint_xml);

        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        infoWindow = LayoutInflater.from(this).inflate(R.layout.amap_info_window, null);
        label = infoWindow.findViewById(R.id.tv_name);
        remove = infoWindow.findViewById(R.id.tv_submit);

        initMapView();
        initUI();
        addListener();

        AMap.OnInfoWindowClickListener listener = new AMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker arg0) {

            }
        };

        aMap.setOnInfoWindowClickListener(listener);

        AMap.InfoWindowAdapter adapter = new AMap.InfoWindowAdapter(){

            @Override
            public View getInfoWindow(Marker marker) {
                render(infoWindow,marker);
                return infoWindow;
            }

            @Override
            public View getInfoContents(Marker marker) {
                render(infoWindow,marker);
                return infoWindow;
            }

            public void render(View view, Marker marker) {
                label.setText(marker.getSnippet());
                remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        marker.hideInfoWindow();

                    }
                });
            }
        };


        aMap.setInfoWindowAdapter(adapter);


        AMap.OnMarkerClickListener mMarkerListener = new AMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                    //showDialog(marker);
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        };

        aMap.setOnMarkerClickListener(mMarkerListener);

    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
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
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = FPVDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            //获取WGS坐标
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();

                            //这里需要坐标转换到GCJ
                            LatLng position = coord.transformFromWGSToGCJ(new LatLng(droneLocationLat,droneLocationLng));
                            droneLocationLat = position.latitude;
                            droneLocationLng = position.longitude;
                            updateDroneLocation();
                        }
                    });

        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {
            setResultToToast(executionEvent.getProgress().targetWaypointIndex+"");
        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    private void getTaskInfo() throws InterruptedException {

        Runner1 r1 = new Runner1();
        Thread t1 = new Thread(r1, "Thread-A");
        t1.start(); //start thread
        t1.join(); //wait thread to finish and block main thread

    }

    class Runner1 implements Runnable{
        @Override
        public void run() {
            DataUtil data = new DataUtil();
            data.getRouteInfo();
            data.getSensorInfo(); //基站数据也是需要的
        }
    }

    public void markTask(){
        //标注基站的逻辑在updateLocation中完成

        //切一下镜头
        CameraUpdate c = CameraUpdateFactory.newLatLngZoom(new LatLng(29.802046,121.561299), 17);
        aMap.moveCamera(c);


        //最好先clear一下地图及任务
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                aMap.clear();
            }

        });

        if(waypointList.size()!=0){
            waypointList.clear();
            points.clear();
            route.clear();
            stations.clear();
            waypointMissionBuilder.waypointList(waypointList);
            updateDroneLocation();
        }

        //标注基站
        //添加基站的代码
        Sensor sen = Sensor.getInstance();
        for(int i=0;i<sen.Id.size();i++){
            //判断飞机坐标与
            Toast.makeText(waypoint_mission.this, "point size:" + String.valueOf(points.size()), Toast.LENGTH_SHORT).show();
            double la = Double.valueOf(sen.latitudes.get(i));
            double ln = Double.valueOf(sen.longtitudes.get(i));
            LatLng p = new LatLng(la, ln);
            //先设置为不可见
            Marker m = aMap.addMarker(new MarkerOptions().position(p).icon(BitmapDescriptorFactory.fromResource(R.drawable.station)).visible(false));
            stations.add(m);
        }


        //注意基站获取的格式如果为WGS，则需要转换为GCJ
        //将获得的坐标转换格式
        path = Route.getInstance();
        for(int i=0;i<path.id.size();i++){
            route.add(new LatLng(path.Lat.get(i),path.Lng.get(i)));
        }

        //标注路线
        for(LatLng p:route){
            markWaypoint(p);
            points.add(p);
            drawPolylines(points);
            //任务(无人机)需要WGS坐标
            LatLng WGS = coord.transformFromGCJToWGS(new LatLng(p.latitude,p.longitude));
            Waypoint mWaypoint = new Waypoint(WGS.latitude, WGS.longitude, altitude);
            //转弯半径
            mWaypoint.cornerRadiusInMeters=0.5F;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }

    }



    @Override
    public void onMapClick(LatLng point) {
        if (isAdd){
            markWaypoint(point);
            points.add(point);
            drawPolylines(points);

            LatLng WGS = coord.transformFromGCJToWGS(new LatLng(point.latitude,point.longitude));
            Waypoint mWaypoint = new Waypoint(WGS.latitude, WGS.longitude, altitude);
            mWaypoint.cornerRadiusInMeters=0.5F;
            //mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,-70));
            //mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,3));
            //mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,0));
            //mWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO,6));
            //mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,-50));
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    private void drawPolylines(List<LatLng> points){

        Polyline poly = aMap.addPolyline(new PolylineOptions().addAll(points).width(10).setDottedLine(false).color(Color.BLUE));
        mPolylines.put(mPolylines.size(),poly);
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public static double getDistance(double firstLatitude, double firstLongitude,
                                     double secondLatitude, double secondLongitude) {
        double firstRadLat = rad(firstLatitude);
        double firstRadLng = rad(firstLongitude);
        double secondRadLat = rad(secondLatitude);
        double secondRadLng = rad(secondLongitude);

        double a = firstRadLat - secondRadLat;
        double b = firstRadLng - secondRadLng;
        double cal = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(firstRadLat)
                * Math.cos(secondRadLat) * Math.pow(Math.sin(b / 2), 2))) * EarthRadius;
        double result = Math.round(cal * 10000d) / 10000d;
        return result;
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft1));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }

                for(Marker marker:stations){
                    //判断无人机当前是否有发现的基站
                    LatLng po = marker.getPosition();
                    if((getDistance(po.latitude,po.longitude,droneLocationLat,droneLocationLng) *1000) <= 70){
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.snippet(String.valueOf(point.latitude)+","+String.valueOf(point.longitude));
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.add:{
                enableDisableAdd();
                break;
            }
            case R.id.clear: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aMap.clear();
                    }

                });
                if(waypointList.size()!=0){
                    waypointList.clear();
                    points.clear();
                    route.clear();
                    stations.clear();
                    waypointMissionBuilder.waypointList(waypointList);
                    updateDroneLocation();
                }
                break;
            }
            case R.id.config:{
                showSettingDialog();
                break;
            }
            case R.id.upload:{
                uploadWayPointMission();
                break;
            }
            case R.id.start:{
                startWaypointMission();
                break;
            }
            case R.id.stop:{
                stopWaypointMission();
                break;
            }
            case R.id.pause:{
                if(pause_resume)
                    pauseWaypointMission();
                else
                    resumeWaypointMission();
                break;
            }
            case R.id.load:{
                //先获取数据
                try {
                    getTaskInfo();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //再标出具体的点


                markTask();
                break;
            }
            default:
                break;
        }
    }

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);

    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            add.setText("Exit");
        }else{
            isAdd = false;
            add.setText("Add");
        }
    }

    private void showSettingDialog(){
        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);

        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);

        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.lowSpeed){
                    mSpeed = 3.0f;
                } else if (checkedId == R.id.MidSpeed){
                    mSpeed = 5.0f;
                } else if (checkedId == R.id.HighSpeed){
                    mSpeed = 10.0f;
                }
            }

        });

        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select finish action");
                if (checkedId == R.id.finishNone){
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                } else if (checkedId == R.id.finishGoHome){
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                } else if (checkedId == R.id.finishAutoLanding){
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                } else if (checkedId == R.id.finishToFirst){
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        });

        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "Select heading");

                if (checkedId == R.id.headingNext) {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
                } else if (checkedId == R.id.headingInitDirec) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                } else if (checkedId == R.id.headingRC) {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                } else if (checkedId == R.id.headingWP) {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                }
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("")
                .setView(wayPointSettings)
                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id) {

                        String altitudeString = wpAltitude_TV.getText().toString();
                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
                        Log.e(TAG,"altitude "+altitude);
                        Log.e(TAG,"speed "+mSpeed);
                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
                        configWayPointMission();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }

                })
                .create()
                .show();
    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }

    }

    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void pauseWaypointMission(){

        getWaypointMissionOperator().pauseMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error==null) {
                    setResultToToast("Mission Pause: " + "Successfully");
                    pause_resume=false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pause.setText("Resume");
                        }
                    });
                }else
                    setResultToToast("Mission Pause: " + error.getDescription());
            }
        });

    }

    private void resumeWaypointMission(){

        getWaypointMissionOperator().resumeMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if(error==null) {
                    setResultToToast("Mission Resume: " + "Successfully");
                    pause_resume=true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pause.setText("Pause");
                        }
                    });
                }else
                    setResultToToast("Mission Resume: " + error.getDescription());
            }
        });

    }
}
