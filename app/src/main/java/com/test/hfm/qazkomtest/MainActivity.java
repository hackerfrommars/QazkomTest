package com.test.hfm.qazkomtest;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    final String DEBUG_TAG = "main_activity_debug";
    final String LOG_TAG = "my_debug";
    final String api_url = "https://mobgocard.kkb.kz/abc3/check2?key=FQjfqj1Jl3&version=14";
    final String MY_PREFS_NAME = "atm_info";
    String TAG_CITY = "";
    float latitude = 0;
    float longitude = 0;
    List<Marker> markers = new ArrayList<>();
    List<Marker> city_markers = new ArrayList<>();
    SharedPreferences sharedPreferences;
    Spinner mSpinner;
    List<String> city_names= new ArrayList<>();
    ArrayAdapter<String> adapter;
    Marker myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSpinner = (Spinner)findViewById(R.id.spinner);


        if(isMyServiceRunning(MyService.class)){
            Log.d(LOG_TAG, "service already running");
        }
        else{
            Log.d("myLogs", "Service is not running ");
            onStart(this);
            Log.d(LOG_TAG, "starting service for downloading json");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        sharedPreferences = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, city_names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setPrompt("Cities");
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                TAG_CITY = city_names.get(position);
                mMap.clear();
                markers.clear();
                try {
                    parseJsonFromSP();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        statusCheck();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            Log.d(LOG_TAG, "getting last location: latitude: " +
                                    location.getLatitude() + "; longitude: " +
                                    location.getLongitude());
                            latitude = (float) location.getLatitude();
                            longitude = (float) location.getLongitude();
                            LatLng sydney = new LatLng(latitude, longitude);
                            myLocation = mMap.addMarker(new MarkerOptions().position(sydney)
                                    .title("My Location").alpha((float) 0.5));
                            getLocation();
                        }
                    }
                });
        if(!sharedPreferences.contains("raw_country")){
            new ParseTask().execute();
        }
        else{
            try {
                parseJsonFromSP();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        for(Marker mar : markers) {
            mar.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mar.setAlpha((float) 0.5);
        }
        marker.setAlpha(1);
        marker.showInfoWindow();
        int mar_size = markers.size();
        LatLng curr_position = marker.getPosition();
        HashMap<Integer, Double> distances = new HashMap<>();
        for (int i = 0; i < mar_size; i++) {
            LatLng temp_position = markers.get(i).getPosition();
            double dist = distanceFrom(curr_position.latitude, curr_position.longitude,
                    temp_position.latitude, temp_position.longitude);
            distances.put(i, dist);
        }
        ValueComparator bvc = new ValueComparator(distances);
        TreeMap<Integer, Double> sorted_map = new TreeMap<Integer, Double>(bvc);
        sorted_map.putAll(distances);
        int limit = 1;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Integer entry : sorted_map.keySet()) {
            Marker curr_marker;
            curr_marker = markers.get(entry);
            builder.include(curr_marker.getPosition());
            curr_marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            curr_marker.setAlpha(1);
            limit += 1;
            if (limit > 10) {
                break;
            }
        }
        LatLngBounds bounds = builder.build();
        int padding = 100;
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        return true;
    }

    public double distanceFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double dist = earthRadius * c;
        int meterConversion = 1609;
        return new Double(dist * meterConversion).floatValue();
    }
    public void getLocation(){
        int city_len = city_markers.size();
        double distFromLocation = 0;
        HashMap<Integer, Double> city_distances = new HashMap<>();
        for(int i = 0; i < city_len; i++){
            Marker city = city_markers.get(i);
            distFromLocation = distanceFrom(latitude,
                    longitude, city.getPosition().latitude,
                    city.getPosition().longitude);
            city_distances.put(i, distFromLocation);
        }
        ValueComparator bvc = new ValueComparator(city_distances);
        TreeMap<Integer, Double> sorted_map = new TreeMap<Integer, Double>(bvc);
        sorted_map.putAll(city_distances);
        int city_id = (int) sorted_map.keySet().toArray()[0];
        mSpinner.setSelection(city_id - 72);
        TAG_CITY = city_markers.get(city_id).getTitle().toString();
    }

    public void parseJsonFromSP() throws JSONException {
        String json = sharedPreferences.getString("raw_country", null);
        if (json != null) {
            JSONObject country = new JSONObject(json);
            JSONArray cities = country.getJSONArray("cities");
            city_names.clear();
            for(int j=0; j<cities.length(); j++){
                JSONObject city = cities.getJSONObject(j);
                String city_name = city.getString("name");
                city_names.add(city_name);
                double city_latitude = city.getDouble("latitude");
                double city_longitude = city.getDouble("longitude");
                LatLng city_coor = new LatLng(city_latitude, city_longitude);
                city_markers.add(mMap.addMarker(new MarkerOptions().position(city_coor).visible(false).title(city.getString("name")).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).alpha((float) 0.2)));
                if(city_name.equals(TAG_CITY) && !TAG_CITY.equals("")){
                    mSpinner.setSelection(j);
                    JSONArray atms = city.getJSONArray("atms");
                    JSONArray branches = city.getJSONArray("branches");
                    for(int k=0; k<atms.length(); k++){
                        JSONObject atm = atms.getJSONObject(k);
                        double atm_latitude = atm.getDouble("latitude");
                        double atm_longitude = atm.getDouble("longitude");
                        String atm_name = atm.getString("name");
                        String atm_address = atm.getString("address");
                        LatLng atm_coor = new LatLng(atm_latitude, atm_longitude);
                        markers.add(mMap.addMarker(new MarkerOptions().position(atm_coor).title(atm_address).alpha((float) 0.5)));
                    }
                    LatLng initial_focus = new LatLng(city_latitude, city_longitude);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initial_focus, 15));
                }
            }
            adapter.notifyDataSetChanged();
        }
    }


    private class ParseTask extends AsyncTask<Void, Void, String> {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";

        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL(api_url);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                resultJson = buffer.toString();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultJson;
        }

        @Override
        protected void onPostExecute(String strJson) {
            super.onPostExecute(strJson);
            JSONObject dataJsonObj = null;
            try {
                dataJsonObj = new JSONObject(strJson);
                JSONArray countries = dataJsonObj.getJSONArray("countries");

                for (int i = 0; i < countries.length(); i++) {
                    JSONObject country = countries.getJSONObject(i);
                    String country_name = country.getString("name");
                    if(country_name.equals("Казахстан")){
                        SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                        editor.putString("raw_country", country.toString());
                        editor.apply();
                        parseJsonFromSP();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    class ValueComparator implements Comparator<Integer> {
        Map<Integer, Double> base;
        public ValueComparator(Map<Integer, Double> base) {
            this.base = base;
        }
        public int compare(Integer a, Integer b) {
            if (base.get(a) <= base.get(b)) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void onStart(Context context) {
        startService(new Intent(context, MyService.class));
    }

    public void onStop(Context context) {
        stopService(new Intent(context, MyService.class));
    }
}
