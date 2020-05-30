package com.example.spdb;

import org.osmdroid.util.GeoPoint;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class Way {
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
        return distance;
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

}
