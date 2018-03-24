package com.test.hfm.qazkomtest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yerbolat Amangeldi
 */

public class MyService extends Service {

    final String LOG_TAG = "my_service_log";
    int timeHours = 24;
    Context context;
    final String api_url = "https://mobgocard.kkb.kz/abc3/check2?key=FQjfqj1Jl3&version=14";
    final String MY_PREFS_NAME = "atm_info";
    private boolean isRunning = false;

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        someTask();
        return super.onStartCommand(intent, flags, startId);
//        return Service.START_NOT_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    void someTask() {
        new Thread(new Runnable() {
            public void run() {
                while(true){
                    HttpURLConnection urlConnection = null;
                    BufferedReader reader = null;
                    String resultJson = "";
                    try {
                        URL url = new URL(api_url);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.connect();

                        InputStream inputStream = urlConnection.getInputStream();
                        StringBuffer buffer = new StringBuffer();
                        Log.d(LOG_TAG, "downloading...");
                        reader = new BufferedReader(new InputStreamReader(inputStream));

                        String line;
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line);
                        }

                        resultJson = buffer.toString();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    JSONObject dataJsonObj = null;
                    try {
                        dataJsonObj = new JSONObject(resultJson);
                        JSONArray countries = dataJsonObj.getJSONArray("countries");

                        for (int i = 0; i < countries.length(); i++) {
                            JSONObject country = countries.getJSONObject(i);
                            String country_name = country.getString("name");
                            if(country_name.equals("Казахстан")){
                                SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                                editor.putString("raw_country", country.toString());
                                editor.apply();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        TimeUnit.HOURS.sleep(timeHours);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}