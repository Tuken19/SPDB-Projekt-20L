package com.example.spdb;

import android.content.Context;
import android.os.AsyncTask;
import android.util.ArrayMap;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.GeometryMath;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.spdb.OverpassQueryResultNew.Element;


public class MapOverpassAdapter extends AsyncTask<ArrayList<GeoPoint>, OverpassQueryResultNew, OverpassQueryResultNew> {

    private MapView mapView = null;
    private Context context = null;
    private int radius;
    private int globalDistance;
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private ArrayList<GeoPoint> roadGeoPoints;
    private Map<Long, Element> poiMap;
    private List<Way> wayArray;
    private List<Way> wayArrayInRoad;
    private ArrayList<GeoPoint> alternativeRoad;

    public MapOverpassAdapter(Context ctx, MapView map) {
        this.context = ctx;
        this.mapView = map;
        this.radius = 100;
        this.globalDistance = 1000;
        this.poiMap = new HashMap<>();
        this.wayArray = new ArrayList<>();
        this.wayArrayInRoad = new ArrayList<>();
        this.alternativeRoad = new ArrayList<>();
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setGlobalDistance(int globalDistance) {
        this.globalDistance = globalDistance;
    }

    private OverpassQueryResultNew search(final ArrayList<GeoPoint> roadPoints) {

//        OverpassQuery query = new OverpassQuery()
//                .format(JSON)
//                .timeout(30)
//                .filterQuery()
//                .way()
//                .tag("highway","footway")
//                .boundingBox(
//                        52.5050700, 21.6264000,
//                        52.5086270, 21.6319020
//                )
//                .end()
//                .output(100);

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
        endPoint = roadGeoPoints.get(roadGeoPoints.size()-1);
        return search(roadGeoPoints);
    }

    @Override
    protected void onPostExecute(OverpassQueryResultNew result) {
        super.onPostExecute(result);
        Toast.makeText(context, "Mam odpowiedz", Toast.LENGTH_SHORT).show();
        if (result != null) {
            for (Element poi : result.elements) {
                if (!alreadyStored(poi)) {
                    storePoi(poi);
                    showPoi(poi);
                }
                // Znajdowanie wszystkich odcinków widokowych
                if (poi.type.equals("way")) {
                    List<GeoPoint> wayPoints = generateWays(poi.nodes);
                    Way way = new Way(poi.id, wayPoints);
                    storeWay(way);
                    showWay(way);
                }
            }

            //Wywołanie wyszukiwania trasy alternatywnej
            findAlternativeRoad();


        } else {
            Toast.makeText(context, "Pusta", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean alreadyStored(Element poi) {
        return poiMap.containsKey(poi.id);
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

    private boolean findAlternativeRoadAlgorithm() {
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
        return true;        // Proper road was found.
    }

    private void findAlternativeRoad(){
        if(findAlternativeRoadAlgorithm()){
            System.out.println("Znalazłem");
            // Graphhopper is generationg alternative road
            RoadFinder roadFinder = new RoadFinder(context, mapView);
            roadFinder.addWayPointsSet(alternativeRoad);
            roadFinder.execute(context.getString(R.string.graphhopper_api_key), MapActivity.ALTERNATIVE_ROAD);
        }
        else {
            System.out.println("Szukam innej trasy");
            MapOverpassAdapter mapOverpassAdapter = new MapOverpassAdapter(context, mapView);
            mapOverpassAdapter.setRadius(2 * radius);
            mapOverpassAdapter.setGlobalDistance(globalDistance);
            mapOverpassAdapter.execute(roadGeoPoints);
        }
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

    private void addAlterPoint(GeoPoint geoPoint) {
        if (!alternativeRoad.contains(geoPoint)) {
            alternativeRoad.add(geoPoint);
        }
    }
}
