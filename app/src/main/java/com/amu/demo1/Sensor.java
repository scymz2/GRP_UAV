package com.amu.demo1;

import java.util.ArrayList;

public class Sensor {

    ArrayList<String> Id = new ArrayList<>();
    ArrayList<String> latitudes = new ArrayList<>();
    ArrayList<String> longtitudes = new ArrayList<>();
    ArrayList<String> temperatures = new ArrayList<>();
    ArrayList<String> humidities = new ArrayList<>();
    ArrayList<String> visibility = new ArrayList<>();

    public ArrayList<String> getId() {
        return Id;
    }

    public ArrayList<String> getLatitudes() {
        return latitudes;
    }

    public ArrayList<String> getLongtitudes() {
        return longtitudes;
    }

    public ArrayList<String> getTemperatures() {
        return temperatures;
    }

    public ArrayList<String> getHumidities() {
        return humidities;
    }

    public ArrayList<String> getVisibility() {
        return visibility;
    }

    public void setId(ArrayList<String> id) {
        Id = id;
    }

    public void setLatitudes(ArrayList<String> latitudes) {
        this.latitudes = latitudes;
    }

    public void setLongtitudes(ArrayList<String> longtitudes) {
        this.longtitudes = longtitudes;
    }

    public void setTemperatures(ArrayList<String> temperatures) {
        this.temperatures = temperatures;
    }

    public void setHumidities(ArrayList<String> humidities) {
        this.humidities = humidities;
    }

    public void setVisibility(ArrayList<String> visibility) {
        this.visibility = visibility;
    }

    private static Sensor instance;

    /**lazy man method to keep task info*/
    public static synchronized Sensor getInstance(){
        if (instance == null) {
            instance = new Sensor();
        }
        return instance;
    }

    public static void setInstance(Sensor instance) {
        Sensor.instance = instance;
    }

}
