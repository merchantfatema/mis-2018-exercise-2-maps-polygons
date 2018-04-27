/*
 * Name: Exercise 2
 * Created Date: 19 April, 2018
 * Purpose: Maps & Polygons
 * Student Name: Fatema Merchant
 * Student Id: 119431
 * */
/*
The class contains the following functionality:
1. To add custom message to each marker
2. To add markers to map
3. To create a polygon
4. To remove a polygon
5. To calculate area of polygon
6. To remove all markers
 */

package polygons.mis.example.com.maps;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private EditText markerMessage;
    private SharedPreferences sharedPrefMarkers;
    SharedPreferences.Editor editor;
    Polygon polygon;
    Marker areaMarker;
    Integer markerCount;
    static public final int REQUEST_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        initialise();
    }

    /*
	@Purpose: After map is loaded, it check if there were any existing markers that can be shown on Map.
	            if yes, adds them to the map.
    */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        configureCurrentLocation();
        ArrayList<MarkerDetail> markerDetailArray = getMarkersFromSharedPref( );
        if (markerDetailArray != null) {
            for (MarkerDetail marker : markerDetailArray) {
                showMarker(marker, mMap, BitmapDescriptorFactory.HUE_RED);
            }
        }
        /*
        @Purpose: LongClickListner to detect long press on map & add a marker at that location with the custom
                    message from the text enrty box.
                    It will also save the added markers to sharedPrefernces.
        */
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng latLng) {
                String message = markerMessage.getText().toString();
                if (message.isEmpty()) {
                    showToastMessage("Please enter a custom message!!!");
                } else {
                    showMarker(new MarkerDetail( message,
                                                 String.valueOf(latLng.latitude),
                                                 String.valueOf(latLng.longitude)),
                                mMap,
                                BitmapDescriptorFactory.HUE_RED);
                    markerMessage.setText("");
                    //Saving Markerps on Map
                    saveMarkers(message,latLng);
                    hideInputKeyboard();
                }
            }//End of onMapLongClick

        }); //End of Listener
    }
    /*
    @Purpose: Function called on click of start/end polygon button. Does respective processing
    */
    public void processPolygon(View view){

        Button polygonBtn = findViewById(R.id.polygonBtn);
        if(polygonBtn.getText().equals("Start Polygon")){

            Boolean successfullExecution = createPolygon();
            if(successfullExecution){
                polygonBtn.setText("End Polygon");
            }
        }
        else{
            polygon.remove();
            areaMarker.remove();
            polygonBtn.setText("Start Polygon");
        }
    }

    /*
        @Purpose: Creates a new Polygon with all the markers on map being considered as its vertices
        @Return: A boolean value indicating if the polygon was sucessfully created or not
    */
    public Boolean createPolygon(){

        ArrayList<LatLng> markerLocations = new ArrayList<>();

        ArrayList<MarkerDetail> markerDetailArray = getMarkersFromSharedPref( );
        if (markerDetailArray != null) {
            for (MarkerDetail marker : markerDetailArray) {
                markerLocations.add(new LatLng( Double.valueOf(marker.latitude),
                                                Double.valueOf(marker.longitude)));
            }
        }

        if( markerLocations.size() >= 3 ){
            LatLng[] latLngArray = new LatLng[markerLocations.size()];
            latLngArray = markerLocations.toArray(latLngArray);

            PolygonOptions vertices = new PolygonOptions()
                                     .add(latLngArray)
                                     .strokeWidth(10)
                                     .strokeColor(Color.argb(255,235,139,38))
                                     .fillColor(Color.argb(100,255,181,102));

            polygon =  mMap.addPolygon(vertices);
            Double polygonArea = computeAreaofPolygon(markerLocations);
            String areaUnit;
            if( polygonArea > 1000 ){
                polygonArea = polygonArea/1000;
                areaUnit = String.format("%.2f",polygonArea)  + "km\u00B2" ;
            }
            else{
                areaUnit = String.format("%.2f",polygonArea)  + "m\u00B2" ;
            }
            LatLng centroid = computeCentroid(markerLocations);
            areaMarker = showMarker( new MarkerDetail("Area of Polygon is: "
                                            + areaUnit,
                    String.valueOf(centroid.latitude),
                    String.valueOf(centroid.longitude)) , mMap, BitmapDescriptorFactory.HUE_ORANGE);
            return true;
        }
        else{
            showToastMessage("There should atleat be 3 markers to create & calculate area of Polygon");
            return false;
        }
    }
    /*
    @Purpose: Computes centroid of polygon & returns the Latitude longitude of centroid
    */
    public LatLng computeCentroid( List<LatLng> markerLocations ){
        LatLng centroid;
        Double latitude = 0.0,longitude = 0.0;
        for(LatLng marker : markerLocations){
            latitude += marker.latitude;
            longitude += marker.longitude;
        }
        Integer locSize = markerLocations.size();

        return new LatLng(latitude/locSize , longitude/locSize);
    }
    /*
    @Purpose: Computes area of polygon
    */
    public double computeAreaofPolygon( List<LatLng> markerLocations ){
        //return google.maps.geometry.spherical.computeArea(markerDetailArray);
        return SphericalUtil.computeArea(markerLocations);
    }

    /*
    @Purpose: Calculates the current location & shows it with a blue marker
    */
    public void configureCurrentLocation(){
        LatLng latLng = getCurrentLocation();
        setLocation(latLng, mMap);
        showMarker( new MarkerDetail("Current Location" ,
                String.valueOf(latLng.latitude),
                String.valueOf(latLng.longitude)) , mMap, BitmapDescriptorFactory.HUE_BLUE);
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    /*
    @Purpose: Initialises the app, check permission
    */
    private void initialise() {

        markerMessage = findViewById(R.id.markerMessage);
        String locationName = getString(R.string.Location_Preferences);
        markerCount = 0;
        sharedPrefMarkers = MapsActivity.this.getSharedPreferences(locationName, Context.MODE_PRIVATE);
        editor = this.sharedPrefMarkers.edit();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        checkLocationPermission();
    }

    /*
    @Purpose: Checks if app has permssion to acess location or not, if not asks the same to user
    */
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Check permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    /*
    @Purpose: Finds useres current location
    */
    private LatLng getCurrentLocation() {

        LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                &&  ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return new LatLng(50.979492,11.323544);
        }
        else {
            LocationProvider high=
                    mLocationManager.getProvider(mLocationManager.getBestProvider( createFineCriteria(),true ));

            Location currentLocation = mLocationManager.getLastKnownLocation(high.getName());
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            return new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
    }

    /*
    @Purpose: Criteria to find current location of user
    */
    public static Criteria createFineCriteria() {

        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setSpeedRequired(false);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_HIGH);
        return c;

    }

    /*
    @Purpose: On load of app, show the users current location
    */
    private void setLocation(LatLng latLng, GoogleMap gMap) {
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0f));
        gMap.animateCamera(CameraUpdateFactory.zoomIn());
        gMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f),2000,null);
    }
    /*
        @Purpose: Function to show markers on map, it returns the marker that was added.
                    We are specifically using the returned value when we are ending a polygon
                     to remove the area marker
    */
    private Marker showMarker( MarkerDetail marker, GoogleMap gMap, Float hue ){
        LatLng location = new LatLng(Double.parseDouble(marker.latitude), Double.parseDouble(marker.longitude));
        Marker mapMarker;
        mapMarker = mMap.addMarker(new MarkerOptions() //new MarkerOptions()
                                    .position(location)
                                    .title(marker.message)
                                    .snippet(marker.message)
                                    .flat(true)
                                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        return mapMarker;
    }

    /*
        @Purpose: To show a tost message in case of any errors
    */
    public void showToastMessage(String message){
        Toast toast = Toast.makeText(MapsActivity.this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0 ,0);
        toast.show();
    }

    /*
        @Purpose: To save the added markers to shared preferences
    */
    private void saveMarkers(String message, LatLng latLng) {

        String latitude = String.valueOf(latLng.latitude);
        String longitude = String.valueOf(latLng.longitude);
        Set<String> markerDetailSet = new HashSet<String>();

        markerDetailSet.add("Message:" + message);
        markerDetailSet.add("Latitude:" + latitude);
        markerDetailSet.add("Longitude:" + longitude);

        Map<String, ?> existingMarker = sharedPrefMarkers.getAll();
        Integer oldMarkersListSize;
        if( existingMarker != null ){
            oldMarkersListSize = existingMarker.size();
            markerCount = oldMarkersListSize;
        }
        editor.putStringSet(String.valueOf(markerCount),markerDetailSet);
        markerCount++;
        editor.apply();
    }
    /*
        @Purpose: To get all the markers from shared preferences,
                store it in array of custom class & return that array
    */
    private ArrayList<MarkerDetail> getMarkersFromSharedPref(){
        Map<String, ?> existingMarker = sharedPrefMarkers.getAll();
        Iterator it = existingMarker.entrySet().iterator();
        MarkerDetail marker;
        ArrayList<MarkerDetail> markerDetailArray = new ArrayList<>();
        Map.Entry key;
        Set<String> markerStringSet;
        Iterator<String> markerSetIt;
        while (it.hasNext()) {
            key = (Map.Entry) it.next();
            markerStringSet = (Set<String>) key.getValue();
            markerSetIt = markerStringSet.iterator();
            String message = "", latitude = "", longitude = "";
            marker = new MarkerDetail();
            while(markerSetIt.hasNext()){

                String markerDetail = markerSetIt.next();
                String[] detailsArray = markerDetail.split(":", 2);

                if( detailsArray[0].equals("Message") ){
                    marker.message = detailsArray[1];
                }
                else if( detailsArray[0].equals("Latitude") ){
                    marker.latitude = detailsArray[1];
                }
                else if( detailsArray[0].equals("Longitude") ){
                    marker.longitude = detailsArray[1];
                }
            }
            markerDetailArray.add( marker );
        }
        return markerDetailArray;
    }
    @Override
    public void onMapLongClick(LatLng latLng) {

    }
    /*
        @Purpose: Function to remove all the markers from map
    */
    public void removeAllMarker( View view){

        ArrayList<MarkerDetail> markerDetailArray = getMarkersFromSharedPref( );
        if (markerDetailArray != null && markerDetailArray.size() > 0){
            mMap.clear();
            configureCurrentLocation();
            editor.clear();
            editor.commit();
        }
        else{
            showToastMessage("There are no markers present on map except for current location. Current Location can not be removed!!!");
        }
    }
    public void hideInputKeyboard(){
        View view = getWindow().getDecorView();
        if (view != null) {
            InputMethodManager iMManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            iMManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
    class MarkerDetail{

        String message;
        String latitude;
        String longitude;
        public MarkerDetail(){}
        public MarkerDetail( String message, String latitude, String longitude){
            this.message = message;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

