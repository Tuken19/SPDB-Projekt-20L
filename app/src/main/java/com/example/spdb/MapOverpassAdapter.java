package com.example.spdb;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.spdb.OverpassQueryResultNew.Element;
import com.example.spdb.ui.map.MapFragment;


public class MapOverpassAdapter extends AsyncTask<ArrayList<GeoPoint>, OverpassQueryResultNew, OverpassQueryResultNew> {

    private MapView mapView = null;
    private Context context = null;
    private int radius;
    private int globalDistance;
    private String algorhitm;
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private ArrayList<GeoPoint> roadGeoPoints;
    private Map<Long, Element> poiMap;
    private List<Way> wayArray;
    private List<Way> wayListInRoad;
    private ArrayList<GeoPoint> alternativeRoad;

    public MapOverpassAdapter(Context ctx, MapView map) {
        this.context = ctx;
        this.mapView = map;
        this.radius = 100;
        this.globalDistance = 1000;
        this.poiMap = new HashMap<>();
        this.wayArray = new ArrayList<>();
        this.wayListInRoad = new LinkedList<>();
        this.alternativeRoad = new ArrayList<>();
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setGlobalDistance(int globalDistance) {
        this.globalDistance = globalDistance;
    }

    public void setAlgorhitm(String algorhitm) {
        this.algorhitm = algorhitm;
    }

    private OverpassQueryResultNew search(final ArrayList<GeoPoint> roadPoints) {

//        StringBuilder builder = new StringBuilder();
//        builder.append("[out:json][timeout:620];\n");
//        builder.append("(");
//        for (GeoPoint gp : roadPoints) {
//            builder.append("way(around:").append(radius * 10).append(",");
//            builder.append(gp.getLatitude()).append(",").append(gp.getLongitude()).append(")");
//            builder.append("[waterway~\"^stream|river$\"];\n");
//            builder.append("way(around:").append(radius * 10).append(",");
//            builder.append(gp.getLatitude()).append(",").append(gp.getLongitude()).append(")");
//            builder.append("[landuse~\"^forest|farm$\"];\n");
//        }
//        builder.append(")->.intrest;\n");
//
//        builder.append("way(around.intrest:").append(radius).append(")").append("[highway~\"^(motorway|trunk|primary|secondary|tertiary|path)$\"]->.ways;\n");
//
//        builder.append("(");
//        for (GeoPoint gp : roadPoints) {
//            builder.append("way.ways(around:").append(radius).append(",");
//            builder.append(gp.getLatitude()).append(",").append(gp.getLongitude()).append(");\n");
//        }
//        builder.append(")->.taken;\n");
//
//        builder.append("(.taken;);\n");
//        builder.append("(._;>;);out;\n");
        //builder.append("out skel qt;");



        StringBuilder builder = new StringBuilder();
        builder.append("[out:json];\n");
        builder.append("(");
        for (GeoPoint gp : roadPoints) {
            builder.append("way(around:").append(radius).append(",");
            builder.append(gp.getLatitude()).append(",").append(gp.getLongitude()).append(")");
            builder.append("[\"highway\"=\"residential\"];\n").append("foreach{(._;>;);out;};");
        }
        builder.append(");");



        System.out.println(builder.toString());

        return interpret(builder.toString());
    }

    private OverpassQueryResultNew interpret(String query) {
        try {
            OverpassQueryResultNew result = OverpassServiceProviderNew.get().interpreter(query).execute().body();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new OverpassQueryResultNew();
        }
    }

    @Override
    protected OverpassQueryResultNew doInBackground(ArrayList<GeoPoint>[] roadPoints) {
        roadGeoPoints = roadPoints[0];
        startPoint = roadGeoPoints.get(0);
        endPoint = roadGeoPoints.get(roadGeoPoints.size() - 1);
        return search(roadGeoPoints);
    }

    @Override
    protected void onPostExecute(OverpassQueryResultNew result) {
        super.onPostExecute(result);
        Toast.makeText(context, "Mam odpowiedz", Toast.LENGTH_SHORT).show();
        if (result != null) {
            for (Element poi : result.elements) {
                if (!alreadyStoredPOI(poi)) {
                    storePoi(poi);
                    showPoi(poi);
                }
                // Znajdowanie wszystkich odcinków widokowych
                if (poi.type.equals("way")) {
                    if(!alreadyStoredWay(poi)) {
                        List<GeoPoint> wayPoints = generateWays(poi.nodes);
                        Way way = new Way(poi.id, wayPoints);
                        way.setCloseness(startPoint, endPoint);
                        storeWay(way);
                        showWay(way);
                    }
                }
            }

            //Wywołanie wyszukiwania trasy alternatywnej
            findAlternativeRoad();


        } else {
            Toast.makeText(context, "Pusta", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean alreadyStoredPOI(Element poi) {
        return poiMap.containsKey(poi.id);
    }

    private boolean alreadyStoredWay(Element poi) {
        return wayArray.contains(poi);
    }

    private void storePoi(Element poi) {
        poiMap.put(poi.id, poi);
    }

    private void storeWay(Way way) {
        wayArray.add(way);
    }

    private void showPoi(Element poi) {
        // Set points
        GeoPoint point = new GeoPoint(poi.lat, poi.lon);
        Polygon polygon = new Polygon();
        final double r = 1;
        ArrayList<GeoPoint> circlePoints = Polygon.pointsAsCircle(point, r);
        polygon.setPoints(circlePoints);
        polygon.getFillPaint().setColor(context.getResources().getColor(R.color.colorPOIFill));
        polygon.setStrokeColor(context.getResources().getColor(R.color.colorPOI));
        mapView.getOverlays().add(polygon);
    }

    private void showWay(Way way){
        Polyline polyline = new Polyline();
        polyline.setPoints(way.getGeoPoints());
        polyline.setColor(context.getResources().getColor(R.color.colorWayPoint));
        mapView.getOverlays().add(polyline);
    }

    private List<GeoPoint> generateWays(List<Long> pointIds){
        List<GeoPoint> wayPoints = new ArrayList<>();
        for(Long id : pointIds){
            double lat = Objects.requireNonNull(poiMap.get(id)).lat;
            double lon = Objects.requireNonNull(poiMap.get(id)).lon;
            GeoPoint geoPoint = new GeoPoint(lat, lon);
            wayPoints.add(geoPoint);
        }
        return wayPoints;
    }

    private Way lookFor(GeoPoint lastPoint){
        Way closestWay = null;
        double minDist = Double.MAX_VALUE;

        for(Way w : wayArray){
            double dist = lastPoint.distanceToAsDouble(w.getStartPoint())
                    + lastPoint.distanceToAsDouble(w.getEndPoint());
            if(dist < minDist) {
                minDist = dist;
                closestWay = w;
            }
        }

        wayArray.remove(closestWay);

        closestWay.setSubWay(lastPoint);

        return closestWay;
    }

    private boolean findAlternativeRoadAlgorithm1() {
        double wholeDistance = 0;
        GeoPoint gp = this.startPoint;
        addAlterPoint(startPoint);

        Collections.sort(wayArray);

        int i = 0;
        while(wholeDistance < this.globalDistance && i <= 100){
            if(wayArray.isEmpty()){
                return false;
            }
            Way w = lookFor(gp);
            if(w.getGeoPoints().size() == 1){
                continue;
            }
            gp = w.getEndPoint();
            wholeDistance += w.getDistance();
            for(GeoPoint g : w.getGeoPoints()){
                addAlterPoint(g);
            }
            i++;
        }

        addAlterPoint(this.endPoint);

        if(i >= 100){
            return false;   // Road is too short.
        }
        System.out.println("Dystans: " + wholeDistance);
        MapFragment.setTvAltSections1(convertDistance(wholeDistance));
        return true;        // Proper road was found.
    }

    private boolean findAlternativeRoadAlgorithm2(){
        double wholeDistance = 0;
        ArrayList<GeoPoint> sw= new ArrayList<GeoPoint>();
        ArrayList<GeoPoint> ew= new ArrayList<GeoPoint>();
        sw.add(startPoint);
        ew.add(endPoint);
        Way startWay = new Way((long) 0, sw);
        Way endWay = new Way((long) 1, ew);

        wayListInRoad.add(startWay);
        wayListInRoad.add(endWay);

        Collections.sort(wayArray);

        int i = 0;
        while(wholeDistance < this.globalDistance && i <= 100) {
            if(wayArray.isEmpty()){
                return false;
            }
            int indexToAdd = whereToAdd();
            Way bfw = bestFittingWay(indexToAdd,indexToAdd + 1);
            if(bfw.getGeoPoints().size() == 1){
                continue;
            }

            wholeDistance += bfw.getDistance();

            wayListInRoad.add(indexToAdd + 1, bfw);

            i++;
        }

        if(i >= 100){
            return false;   // Road is too short.
        }

        for(Way w : wayListInRoad){
            for (GeoPoint gp : w.getGeoPoints()){
                addAlterPoint(gp);
            }
        }

        System.out.println("Dystans: " + wholeDistance);
        MapFragment.setTvAltSections2(convertDistance(wholeDistance));
        return true;        // Proper road was found.
    }

    private int whereToAdd(){
        double dist = 0;
        int index = 0;
        for(int i = 0; i < wayListInRoad.size() - 1; i++ ){
            GeoPoint sw = wayListInRoad.get(i).getEndPoint();
            GeoPoint ew = wayListInRoad.get(i+1).getStartPoint();
            double d = sw.distanceToAsDouble(ew);
            if(d > dist){
                dist = d;
                index = i;
            }
        }
        return index;
    }

    private Way bestFittingWay(int index1, int index2){
        GeoPoint sp = wayListInRoad.get(index1).getEndPoint();
        GeoPoint ep = wayListInRoad.get(index2).getStartPoint();

        for(Way w : wayArray){
            w.setCloseness(sp, ep);
        }
        Collections.sort(wayArray);

        Way bestFittingWay =  wayArray.get(0);
        wayArray.remove(0);
        bestFittingWay.setSubWay(sp);
        return bestFittingWay;
    }

    private void findAlternativeRoad(){
        if(algorhitm.equals(MapActivity.ALTERNATIVE_ROAD_1)){
            if(findAlternativeRoadAlgorithm1()){
                System.out.println("Znalazłem 1");
                // Graphhopper is generationg alternative road 1
                RoadFinder roadFinder = new RoadFinder(context, mapView);
                roadFinder.addWayPointsSet(alternativeRoad);
                roadFinder.execute(context.getString(R.string.graphhopper_api_key), MapActivity.ALTERNATIVE_ROAD_1);

                // Look for alternative road 2
                MapOverpassAdapter mapOverpassAdapter = new MapOverpassAdapter(context, mapView);
                mapOverpassAdapter.setAlgorhitm(MapActivity.ALTERNATIVE_ROAD_2);
                mapOverpassAdapter.setRadius(radius);
                mapOverpassAdapter.setGlobalDistance(globalDistance);
                mapOverpassAdapter.execute(roadGeoPoints);
            }
            else {
                System.out.println("Szukam innej trasy 1");
                MapOverpassAdapter mapOverpassAdapter = new MapOverpassAdapter(context, mapView);
                mapOverpassAdapter.setRadius(2 * radius);
                mapOverpassAdapter.setAlgorhitm(MapActivity.ALTERNATIVE_ROAD_1);
                mapOverpassAdapter.setGlobalDistance(globalDistance);
                mapOverpassAdapter.execute(roadGeoPoints);
            }
        }
        else{
            if(findAlternativeRoadAlgorithm2()){
                System.out.println("Znalazłem 2");
                // Graphhopper is generationg alternative road 2
                RoadFinder roadFinder = new RoadFinder(context, mapView);
                roadFinder.addWayPointsSet(alternativeRoad);
                roadFinder.execute(context.getString(R.string.graphhopper_api_key), MapActivity.ALTERNATIVE_ROAD_2);
            }
            else {
                System.out.println("Szukam innej trasy 2");
                MapOverpassAdapter mapOverpassAdapter = new MapOverpassAdapter(context, mapView);
                mapOverpassAdapter.setAlgorhitm(MapActivity.ALTERNATIVE_ROAD_2);
                mapOverpassAdapter.setRadius(2 * radius);
                mapOverpassAdapter.setGlobalDistance(globalDistance);
                mapOverpassAdapter.execute(roadGeoPoints);
            }
        }
    }

    private void addAlterPoint(GeoPoint geoPoint) {
        if (!alternativeRoad.contains(geoPoint)) {
            alternativeRoad.add(geoPoint);
        }
    }

    private String convertDistance(double distance){
        return new DecimalFormat("#.0# km").format(distance/1000);
    }
}