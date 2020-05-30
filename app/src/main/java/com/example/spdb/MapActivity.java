package com.example.spdb;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.example.spdb.ui.map.MapFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.example.spdb.ui.map.MapFragment.centerMapOnMyLocation;
import static com.example.spdb.ui.map.MapFragment.getMapView;

public class MapActivity extends AppCompatActivity {

    public static final String BEST_ROAD = "BestRoad";
    public static final String ALTERNATIVE_ROAD = "AlternativeRoad";

    private AppBarConfiguration mAppBarConfiguration;
    Toolbar toolbar;
    DrawerLayout drawer;
    NavigationView navigationView;

    Fragment mapFragment;

    private static MapView mapView;
    private FloatingActionButton fabCenter;

    private static GeoPoint startPoint = null;
    private static GeoPoint endPoint = null;
    private static int radius = 100;
    private static int globalDistance = 1000;

    public static FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        init();

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_properties, R.id.nav_map)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                if(destination.getId()==R.id.nav_properties){
                    fabCenter.setVisibility(View.GONE);
                }
                if(destination.getId()==R.id.nav_map){
                    fabCenter.setVisibility(View.VISIBLE);
                }
            }
        });

        centerOnMyLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        navController.saveState();
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    private void init(){
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        fabCenter = findViewById(R.id.fab_center);

        mapView = getMapView();
    }

    private void centerOnMyLocation(){
        fabCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centerMapOnMyLocation(getApplicationContext(), v);
            }
        });
    }

    public static void setStartPoint(GeoPoint sPoint) {
        startPoint = sPoint;
    }

    public static void setEndPoint(GeoPoint ePoint) {
        endPoint = ePoint;
    }

    public static void setRadius(int r) {
        radius = r;
    }

    public static void setGlobalDistance(int distance) {
        globalDistance = distance;
    }

    public static GeoPoint getStartPoint() {
        return startPoint;
    }

    public static GeoPoint getEndPoint() {
        return endPoint;
    }

    public static int getRadius() {
        return radius;
    }

    public static int getGlobalDistance() {
        return globalDistance;
    }
}
