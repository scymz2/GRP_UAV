package com.amu.demo1;

import java.util.ArrayList;

public class Route {
    ArrayList<Integer> id = new ArrayList<>();
    ArrayList<Double> Lng = new ArrayList<>();
    ArrayList<Double> Lat = new ArrayList<>();


    private static Route instance;

    /**lazy man method to keep task info*/
    public static synchronized Route getInstance(){
        if (instance == null) {
            instance = new Route();
        }
        return instance;
    }

    public static void setInstance(Route instance){
        Route.instance = instance;
    }
}
