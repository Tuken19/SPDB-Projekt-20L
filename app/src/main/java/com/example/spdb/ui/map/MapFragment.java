package com.example.spdb.ui.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.spdb.MapActivity;
import com.example.spdb.R;
import com.example.spdb.RoadFinder;
import com.example.spdb.ui.properties.PropertiesFragmentArgs;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.example.spdb.MapActivity.getEndPoint;
import static com.example.spdb.MapActivity.getStartPoint;


public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private boolean locationPermissionGranted = false;

    private static final float DEFAULT_ZOOM_LEVEL = 16.0f;
    private GeoPoint DEFAULT_LOCATION = new GeoPoint(52.5105185, 21.6331596);

    private static Location lastKnownLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 1001;
    private static MapView mapView = null;
    private MyLocationNewOverlay locationOverlay = null;
    private CompassOverlay compassOverlay = null;

    private static TextView tvBestDistance;
    private static TextView tvBestTime;
    private static TextView tvAltDistance1;
    private static TextView tvAltTime1;
    private static TextView tvAltSections1;
    private static TextView tvAltDistance2;
    private static TextView tvAltTime2;
    private static TextView tvAltSections2;

    private static List<Overlay> listOfRoads;

    private ConstraintLayout layoutBest;
    private ConstraintLayout layoutAlt1;
    private ConstraintLayout layoutAlt2;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        //handle permissions first, before map is created. not depicted here
        getLocationPermission();
        //load/initialize the osmdroid configuration, this can be done
        Context ctx = this.getContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's
        //tile servers will get you banned based on this string

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        View root = inflater.inflate(R.layout.fragment_map, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        listOfRoads = new LinkedList<>();

        // ===== My Location Overlay =====
        mapView.setMultiTouchControls(true);
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this.getContext()), mapView);
        locationOverlay.enableMyLocation();
        mapView.getOverlays().add(locationOverlay);

        // ===== Compass Overlay =====
        compassOverlay = new CompassOverlay(this.getContext(), new InternalCompassOrientationProvider(this.getContext()), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        // ===== Ways info =====
        tvBestDistance = view.findViewById(R.id.best_distance);
        tvBestTime = view.findViewById(R.id.best_time);
        tvAltDistance1 = view.findViewById(R.id.alt_distance1);
        tvAltTime1 = view.findViewById(R.id.alt_time1);
        tvAltSections1 = view.findViewById(R.id.alt_sections1);
        tvAltDistance2 = view.findViewById(R.id.alt_distance2);
        tvAltTime2 = view.findViewById(R.id.alt_time2);
        tvAltSections2 = view.findViewById(R.id.alt_sections2);
        layoutBest = view.findViewById(R.id.layout_best);
        layoutAlt1 = view.findViewById(R.id.layout_alt1);
        layoutAlt2 = view.findViewById(R.id.layout_alt2);


        if (getArguments() != null) {// to avoid the NullPointerException
            int actionType = PropertiesFragmentArgs.fromBundle(getArguments()).getActionType();
            if (actionType == 1) {
                getPointLatLan(actionType);
            } else if (actionType == 2) {
                getPointLatLan(actionType);
            } else if (actionType == 3) {
                GeoPoint startPoint = getStartPoint();
                GeoPoint endPoint = getEndPoint();
                findRoad(startPoint, endPoint);
            }
        }

        updateLocationUI();
        drawStartPoint(getStartPoint());
        drawEndPoint(getEndPoint());

        bestOnOff();
        alt1OnOff();
        alt2OnOff();
    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Configuration.getInstance().load(getContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        mapView.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Configuration.getInstance().save(getContext(), prefs);
        mapView.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this.getActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE);
        }
    }

    private void updateLocationUI() {
        if (mapView == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                getDeviceLocation();
            } else {
                IMapController mapController = mapView.getController();
                mapController.setZoom(9.5);
                GeoPoint startPoint = DEFAULT_LOCATION;
                mapController.setCenter(startPoint);

                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                final Task locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = (Location) task.getResult();
                            IMapController mapController = mapView.getController();
                            mapController.setZoom(DEFAULT_ZOOM_LEVEL);
                            GeoPoint startPoint = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                            mapController.setCenter(startPoint);
                        } else {
                            Toast.makeText(getActivity(), "Current location is null. Using defaults.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            IMapController mapController = mapView.getController();
                            mapController.setZoom(DEFAULT_ZOOM_LEVEL);
                            mapController.setCenter(DEFAULT_LOCATION);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public static void centerMapOnMyLocation(Context context, View view) {
        if (isLocationEnabled(context)) {
            IMapController mapController = mapView.getController();
            mapController.setZoom(DEFAULT_ZOOM_LEVEL);
            GeoPoint startPoint = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            mapController.animateTo(startPoint);

            Snackbar.make(view, "Centering on my location.", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        } else {
            Snackbar.make(view, "Please enable location services.", Snackbar.LENGTH_SHORT).show();
        }
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    public static MapView getMapView() {
        return mapView;
    }

    private void getPointLatLan(final int actionType) {
        final MapEventsReceiver mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Toast.makeText(mapView.getContext(), p.getLatitude() + " - " + p.getLongitude(), Toast.LENGTH_SHORT).show();
                if (actionType == 1) {
                    MapActivity.setStartPoint(p);
                    // Go back to properties fragment
                    NavController navController = Navigation.findNavController(mapView);
                    navController.popBackStack();
                }
                if (actionType == 2) {
                    MapActivity.setEndPoint(p);
                    // Go back to properties fragment
                    NavController navController = Navigation.findNavController(mapView);
                    navController.popBackStack();
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        mapView.getOverlays().add(new MapEventsOverlay(mReceive));
    }

    private void drawStartPoint(GeoPoint startPoint){
        if(startPoint != null) {
            Polygon polygon = new Polygon();
            ArrayList<GeoPoint> circlePoints = Polygon.pointsAsCircle(startPoint, 10);
            polygon.setPoints(circlePoints);
            polygon.getFillPaint().setColor(getResources().getColor(R.color.colorStartFill));
            polygon.setStrokeColor(getResources().getColor(R.color.colorStart));
            mapView.getOverlays().add(polygon);
        }
    }

    private void drawEndPoint(GeoPoint endPoint){
        if(endPoint != null) {
            Polygon polygon = new Polygon();
            ArrayList<GeoPoint> circlePoints = Polygon.pointsAsCircle(endPoint, 10);
            polygon.setPoints(circlePoints);
            polygon.getFillPaint().setColor(getResources().getColor(R.color.colorEndFill));
            polygon.setStrokeColor(getResources().getColor(R.color.colorEnd));
            mapView.getOverlays().add(polygon);
        }
    }

    private void findRoad(GeoPoint startPoint, GeoPoint endPoint) {
        RoadFinder roadFinder = new RoadFinder(getContext(), mapView);
        roadFinder.setRadius(MapActivity.getRadius());
        roadFinder.setGlobalDistance(MapActivity.getGlobalDistance());
        roadFinder.addWaypoint(startPoint);
        roadFinder.addWaypoint(endPoint);
        roadFinder.execute(getString(R.string.graphhopper_api_key), MapActivity.BEST_ROAD);
    }

    public static void setTvBestDistanceText(String bestDistance) {
        tvBestDistance.setText(bestDistance);
    }

    public static void setTvBestTimeText(String bestTime) {
        tvBestTime.setText(bestTime);
    }

    public static void setTvAltDistanceText1(String altDistance) {
        tvAltDistance1.setText(altDistance);
    }

    public static void setTvAltTimeText1(String altTime) {
        tvAltTime1.setText(altTime);
    }

    public static void setTvAltDistanceText2(String altDistance) {
        tvAltDistance2.setText(altDistance);
    }

    public static void setTvAltTimeText2(String altTime) {
        tvAltTime2.setText(altTime);
    }

    public static void setTvAltSections1(String altSections){
        tvAltSections1.setText(altSections);
    }

    public static void setTvAltSections2(String altSections){
        tvAltSections2.setText(altSections);
    }

    public static void addToListOfRoads(Overlay road){
        listOfRoads.add(road);
    }

    private void bestOnOff(){
        layoutBest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = listOfRoads.size();
                if( size == 3){
                    Overlay bestMap = listOfRoads.get(0);
                    if(bestMap.isEnabled()){
                        bestMap.setEnabled(false);
                        layoutBest.setBackgroundColor(getResources().getColor(R.color.transparent));
                    }
                    else {
                        bestMap.setEnabled(true);
                        layoutBest.setBackgroundColor(getResources().getColor(R.color.colorBestWayFill));
                    }
                }
            }
        });
    }

    private void alt1OnOff(){
        layoutAlt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = listOfRoads.size();
                if( size == 3){
                    Overlay altMap1 = listOfRoads.get(1);
                    if(altMap1.isEnabled()){
                        altMap1.setEnabled(false);
                        layoutAlt1.setBackgroundColor(getResources().getColor(R.color.transparent));
                    }
                    else {
                        altMap1.setEnabled(true);
                        layoutAlt1.setBackgroundColor(getResources().getColor(R.color.colorAlternativeWayFill));
                    }
                }
            }
        });
    }

    private void alt2OnOff(){
        layoutAlt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int size = listOfRoads.size();
                if( size == 3){
                    Overlay altMap2 = listOfRoads.get(2);
                    if(altMap2.isEnabled()){
                        altMap2.setEnabled(false);
                        layoutAlt2.setBackgroundColor(getResources().getColor(R.color.transparent));
                    }
                    else {
                        altMap2.setEnabled(true);
                        layoutAlt2.setBackgroundColor(getResources().getColor(R.color.colorAlternativeWayFill2));
                    }
                }
            }
        });
    }
}