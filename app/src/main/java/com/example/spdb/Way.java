package com.example.spdb;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class Way implements Comparable<Way>{
    private Long id;
    private double distance;
    private List<GeoPoint> geoPoints;
    private double closeness;

    Way(@NonNull Long id, @NonNull List<GeoPoint> wayGPs){
        this.id = id;
        this.geoPoints = wayGPs;
        this.distance = calculateDistance(geoPoints);
        this.closeness = Double.MAX_VALUE;
    }

    public Long getId() {
        return id;
    }

    public double getDistance() {
        return calculateDistance(geoPoints);
    }

    public List<GeoPoint> getGeoPoints() {
        return geoPoints;
    }

    public GeoPoint getStartPoint(){
        return this.geoPoints.get(0);
    }

    public GeoPoint getCenterPoint() {
        return GeoPoint.fromCenterBetween(getStartPoint(), getEndPoint());
    }

    public GeoPoint getEndPoint(){
        return this.geoPoints.get(geoPoints.size()-1);
    }

    public double getCloseness() {
        return closeness;
    }

    private double calculateDistance(List<GeoPoint> geoPoints) {
        double distance = 0;
        for (int i = 0; i < geoPoints.size() - 1; i++) {
            distance += geoPoints.get(i).distanceToAsDouble(geoPoints.get(i + 1));
        }
        return distance;
    }

    public void setCloseness(GeoPoint startPoint, GeoPoint endPoint){
        double ss = getStartPoint().distanceToAsDouble(startPoint);
        double se = getStartPoint().distanceToAsDouble(endPoint);
        double es = getEndPoint().distanceToAsDouble(startPoint);
        double ee = getEndPoint().distanceToAsDouble(endPoint);
        if(ss + ee < se + es){
            this.closeness = (ss + ee)/2;
        }
        else {
            Collections.reverse(geoPoints);
            this.closeness = (se + es)/2;
        }
    }

    @Override
    public int compareTo(Way way) {
        return Double.compare(this.closeness, way.getCloseness());
    }

    public void setSubWay(GeoPoint geoPoint){
        double dist = Double.MAX_VALUE;
        int index = 0;
        for(int i = 0; i < geoPoints.size(); i++){
            if(dist > geoPoint.distanceToAsDouble(geoPoints.get(i))){
                dist = geoPoint.distanceToAsDouble(geoPoints.get(i));
                index = i;
            }
        }
        this.geoPoints = geoPoints.subList(index, geoPoints.size());
        System.out.println("SIZE: " + geoPoints.size());
    }
}
