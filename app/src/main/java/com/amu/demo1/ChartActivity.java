package com.amu.demo1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class ChartActivity extends AppCompatActivity {

    private static final String TAG = ChartActivity.class.getName();

    RecyclerView recyclerView;
    private Sensor sensor;
    String lng[], lat[], tem[], hum[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        //scrollable recycle list
        recyclerView = findViewById(R.id.recyclerView);

        //获取sensor信息
        try {
            Log.i(TAG, "start get info");
            getInfo();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sensor = Sensor.getInstance();
        lng = (String[]) sensor.getLongtitudes().toArray(new String[0]);
        lat = (String[]) sensor.getLatitudes().toArray(new String[0]);
        tem = (String[]) sensor.getTemperatures().toArray(new String[0]);
        hum = (String[]) sensor.getHumidities().toArray(new String[0]);


        StationAdapter stationAdapter = new StationAdapter(this, lat, lng, tem, hum);
        recyclerView.setAdapter(stationAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    private void getInfo() throws InterruptedException {

        Runner1 r1 = new Runner1();
        Thread t1 = new Thread(r1, "Thread-A");
        t1.start(); //start thread
        t1.join(); //wait thread to finish and block main thread

    }

    class Runner1 implements Runnable{
        @Override
        public void run() {
            DataUtil data = new DataUtil();
            data.getSensorInfo();
        }
    }



}
