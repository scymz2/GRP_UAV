package com.amu.demo1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.MyViewHolder>{

    Context context;
    String lng[], lat[], temperature[], humidity[];


    public StationAdapter(Context ct, String s1[], String s2[], String s3[], String s4[]){
        context = ct;
        if(s1.length == 0){
            String d1[] = {"--"};
            String d2[] = {"--"};
            String d3[] = {"--"};
            String d4[] = {"--"};
            lng = d1;
            lat = d2;
            temperature = d3;
            humidity = d4;
        }else {
            lng = s1;
            lat = s2;
            temperature = s3;
            humidity = s4;
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.station_segment, parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.id.setText("Sensor "+position);
        holder.longitude.setText(String.valueOf(lng[position]) + " W");
        holder.latitude.setText(String.valueOf(lat[position])+ " E");
        holder.temperature.setText(String.valueOf(temperature[position]) + "℃");
        holder.humidity.setText(String.valueOf(humidity[position]));

    }

    @Override
    public int getItemCount() {
        return lng.length;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView id, latitude, longitude, temperature, humidity;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.textView7);
            latitude = itemView.findViewById(R.id.latitude);
            longitude = itemView.findViewById(R.id.longitude);
            temperature = itemView.findViewById(R.id.temperature);
            humidity = itemView.findViewById(R.id.humidity);
        }
    }
}
