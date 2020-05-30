package com.example.spdb.ui.properties;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.spdb.MapActivity;
import com.example.spdb.R;
import com.example.spdb.ui.map.MapFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import org.osmdroid.util.GeoPoint;

public class PropertiesFragment extends Fragment {

    private FloatingActionButton fabStart;
    private FloatingActionButton fabEnd;
    private TextView startLatitude;
    private TextView startLongitude;
    private TextView endLatitude;
    private TextView endLongitude;
    private IndicatorSeekBar seekBarRadius;
    private IndicatorSeekBar seekBarDistance;
    private Button buttonFindRoad;

    private GeoPoint startGeoPoint = null;
    private GeoPoint endGeoPoint = null;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_properties, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);

        addStartPoint();
        addEndPoint();
        findRoad();
        seekRadius();
        seekMinimalDistance();
    }

    private void init(View view) {
        fabStart = view.findViewById(R.id.fab_add_start);
        fabEnd = view.findViewById(R.id.fab_add_end);
        startLatitude = view.findViewById(R.id.text_view_start_lat_position);
        startLongitude = view.findViewById(R.id.text_view_start_lon_position);
        endLatitude = view.findViewById(R.id.text_view_end_lat_position);
        endLongitude = view.findViewById(R.id.text_view_end_lon_position);
        seekBarRadius = view.findViewById(R.id.seek_radius);
        seekBarDistance = view.findViewById(R.id.seek_distance);
        buttonFindRoad = view.findViewById(R.id.button_find_road);

        startGeoPoint = MapActivity.getStartPoint();
        endGeoPoint = MapActivity.getEndPoint();
        if (startGeoPoint != null) {
            startLatitude.setText(String.format("%s", startGeoPoint.getLatitude()));
            startLongitude.setText(String.format("%s", startGeoPoint.getLongitude()));
        }
        if (endGeoPoint != null) {
            endLatitude.setText(String.format("%s", endGeoPoint.getLatitude()));
            endLongitude.setText(String.format("%s", endGeoPoint.getLongitude()));
        }
        if(startGeoPoint != null && endGeoPoint != null){
            buttonFindRoad.setEnabled(true);
        }
        else{
            buttonFindRoad.setEnabled(false);
        }
    }

    private void addStartPoint() {
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_add_start_point);
            }
        });
    }

    private void addEndPoint() {
        fabEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_add_end_point);
            }
        });
    }

    private void seekRadius(){
        seekBarRadius.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {

            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                MapActivity.setRadius(seekBar.getProgress());
            }
        });
    }

    private void seekMinimalDistance(){
        seekBarDistance.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {

            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                MapActivity.setGlobalDistance(seekBar.getProgress() * 1000); // km --> m
            }
        });
    }

    private void findRoad(){
        buttonFindRoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(v);
                navController.navigate(R.id.action_find_road);
            }
        });
    }
}
