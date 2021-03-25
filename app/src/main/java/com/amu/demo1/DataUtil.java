package com.amu.demo1;

import android.util.Log;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;
import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class DataUtil {

    JdbcUtil jdbcUtil = JdbcUtil.getInstance();
    Connection conn = jdbcUtil.getConnection();
    private Sensor sensor;
    private Route route;

    public void getSensorInfo() {

        if (conn == null) {
            Log.i(TAG, "login:conn is null");
        } else {
            Log.i(TAG, "connection good");
            try {
                String sql = "select * FROM sensor";
                PreparedStatement pres = conn.prepareStatement(sql);
                ResultSet res = pres.executeQuery();
                sensor = Sensor.getInstance();
                while (res.next()) {
                    String Id = res.getString("Id");
                    String Lat = res.getString("Lat");
                    String Lng = res.getString("Lng");
                    String humidity = res.getString("Humidity");
                    String temperature = res.getString("Temperature");
                    String visibility = res.getString("Visible");

                    sensor.Id.add(Id);
                    sensor.latitudes.add(Lat);
                    sensor.longtitudes.add(Lng);
                    sensor.humidities.add(humidity);
                    sensor.temperatures.add(temperature);
                    sensor.visibility.add(visibility);
                }
                Sensor.setInstance(sensor);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void getRouteInfo() {
        if (conn == null) {
            Log.i(TAG, "login:conn is null");
        } else {
            Log.i(TAG, "connection good");
            try {
                String sql = "select * FROM route";
                PreparedStatement pres = conn.prepareStatement(sql);
                ResultSet res = pres.executeQuery();
                route = Route.getInstance();
                while (res.next()) {
                    int id = res.getInt("Id");
                    double lat = res.getDouble("Lat");
                    double lng = res.getDouble("Lng");
                    route.id.add(id);
                    route.Lat.add(lat);
                    route.Lng.add(lng);
                }
                Route.setInstance(route);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
