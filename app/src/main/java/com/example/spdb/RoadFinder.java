package com.example.spdb;

import android.content.Context;
import android.os.AsyncTask;

import com.example.spdb.ui.map.MapFragment;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.bonuspack.routing.GraphHopperRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

public class RoadFinder extends AsyncTask<String, Road[], Road[]> {

    private ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
    private String[] parameters = null;
    private MapView mapView = null;
    private Context context = null;
    private int radius;
    private int globalDistance;

    public RoadFinder(Context ctx, MapView map) {
        this.context = ctx;
        this.mapView = map;
        this.radius = 100;
        this.globalDistance = 1000;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setGlobalDistance(int globalDistance) {
        this.globalDistance = globalDistance;
    }

    public void addWaypoint(GeoPoint geoPoint) {
        waypoints.add(geoPoint);
    }

    public void addWayPointsSet(ArrayList<GeoPoint> wayPoints) {
        this.waypoints = wayPoints;
    }

    public void clear() {
        waypoints.clear();
    }

    @Override
    protected Road[] doInBackground(String[] params) {
        parameters = params;
        String apiKey = params[0];
        RoadManager roadManager = new GraphHopperRoadManager(apiKey, false);
        roadManager.addRequestOption("vehicle=foot");
        Road[] roads = roadManager.getRoads(waypoints);
        return roads;
    }

    @Override
    protected void onPostExecute(Road[] roads) {
        super.onPostExecute(roads);
        Road road = roads[0];

        Polyline roadOverlay;

        if (parameters[1].equals(MapActivity.BEST_ROAD)) {
            // Generowanie trasy alternatywnej
            generateAlternativeRoad(road);

            // Wyświetlanie najlepszej trasy
            roadOverlay = RoadManager.buildRoadOverlay(road);
            MapFragment.setTvBestDistanceText(convertDistance(road.mLength));
            MapFragment.setTvBestTimeText(convertTime(road.mDuration));

        }
        else{
            // Wyświetlenie trasy alternatywnej
            roadOverlay = RoadManager.buildRoadOverlay(road, context.getResources().getColor(R.color.colorAlternativeWayFill), 10);
            MapFragment.setTvAltDistanceText(convertDistance(road.mLength));
            MapFragment.setTvAltTimeText(convertTime(road.mDuration));
        }

        mapView.getOverlays().add(roadOverlay);
        mapView.invalidate();
        String lengthAndTime = road.getLengthDurationText(context, -1);
        Snackbar.make(mapView, lengthAndTime, Snackbar.LENGTH_SHORT).show();
    }

    private ArrayList<GeoPoint> getRoadPoints(Road road) {
        ArrayList<GeoPoint> roadPoints = new ArrayList<>();
        for (RoadNode roadNode : road.mNodes) {
            double lat = roadNode.mLocation.getLatitude();
            double lon = roadNode.mLocation.getLongitude();
            roadPoints.add(new GeoPoint(lat, lon));
        }
        return roadPoints;
    }

    private void fetchPoints(ArrayList<GeoPoint> geoPoints) {
        MapOverpassAdapter mapOverpassAdapter = new MapOverpassAdapter(context, mapView);
        mapOverpassAdapter.setRadius(radius);
        mapOverpassAdapter.setGlobalDistance(globalDistance);
        mapOverpassAdapter.execute(geoPoints);
    }

    private void generateAlternativeRoad(Road road){
        ArrayList<GeoPoint> roadPoints = getRoadPoints(road);
        if (!roadPoints.isEmpty()) {
            fetchPoints(roadPoints);
        }
    }

    private String convertDistance(double distance){
        return new DecimalFormat("#.0# km").format(distance);
    }

    private String convertTime(double time){
        int h = (int) (time/3600);
        int min = (int) (time - h * 3600)/60;
        int s = (int) (time -  h * 3600 - min * 60);
        return String.format(Locale.US, "%d h %d min %d s", h, min, s);
    }
}
