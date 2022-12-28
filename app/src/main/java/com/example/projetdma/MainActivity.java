package com.example.projetdma;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String API_KEY ="10aaf940079091d1573b66f8efdacf7e";
    Button BtnSrc;
    EditText NameW;
    ImageView ImgW;
    TextView TVtemp,TVW,affi;
    ArrayList<String> List1;
    ArrayAdapter arrayAdapter;
    private LocationRequest locationRequest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        List1 = new ArrayList<>();
        ListView ls = findViewById(R.id.wliste);
        arrayAdapter = new ArrayAdapter(MainActivity.this,R.layout.item_weather,R.id.tvd,List1);
        ls.setAdapter(arrayAdapter);
        ls.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


            }
        });
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Toast.makeText(MainActivity.this,"ipadress = "+ip,Toast.LENGTH_LONG).show();
        BtnSrc = findViewById(R.id.chercher);
        NameW = findViewById(R.id.Wilaya);
        ImgW = findViewById(R.id.iconWeather);
        TVtemp = findViewById(R.id.tvTemp);
        TVW = findViewById(R.id.tvW);
        affi = findViewById(R.id.afficher);
        //noinspection deprecation
        locationRequest = LocationRequest.create();
        //noinspection deprecation
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //noinspection deprecation
        locationRequest.setInterval(5000);
        //noinspection deprecation
        locationRequest.setFastestInterval(2000);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    if(IsGPSEnabled()){
                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                .requestLocationUpdates(locationRequest, new LocationCallback() {
                                    @Override
                                    public void onLocationResult(@NonNull LocationResult locationResult) {
                                        super.onLocationResult(locationResult);
                                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                                .removeLocationUpdates(this);
                                        if(locationResult != null && locationResult.getLocations().size() >0){
                                                int index = locationResult.getLocations().size() - 1;
                                            double lat = locationResult.getLocations().get(index).getLatitude();
                                            double lon = locationResult.getLocations().get(index).getLongitude();
                                            ChargerMeteoWilaya(lat,lon);
                                        }
                                    }
                                }, Looper.getMainLooper());
                    }else{
                        turnOnGPS();
                    }
            }else{
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }



        BtnSrc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String wilaya = NameW.getText().toString();
                if(wilaya.isEmpty()){
                    Toast.makeText(MainActivity.this,"Svp entrer une wilaya",Toast.LENGTH_LONG).show();
                }else{
                     ChargerMeteoWilaya(wilaya);
                }
            }
        });
    }

    private void turnOnGPS() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {

                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Toast.makeText(MainActivity.this, "GPS is already tured on", Toast.LENGTH_SHORT).show();

                } catch (ApiException e) {

                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException)e;
                                resolvableApiException.startResolutionForResult(MainActivity.this,2);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            //Device does not have location
                            break;
                    }
                }
            }
        });

    }

    private void ChargerMeteoWilaya(String wilaya) {
        Ion.with(this)
                .load("https://api.openweathermap.org/data/2.5/weather?q="+wilaya+"&&units=metric&lang=fr&appid="+API_KEY)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        if (e != null) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Server error", Toast.LENGTH_LONG).show();
                        } else {
                            JsonObject main = result.get("main").getAsJsonObject();
                            Double temp = main.get("temp").getAsDouble();
                            TVtemp.setText(temp + " °C");
                            JsonObject sys = result.get("sys").getAsJsonObject();
                            String pays = sys.get("country").getAsString();
                            String w = result.get("name").getAsString();
                            TVW.setText(w+ " " + pays);
                            JsonArray we = result.get("weather").getAsJsonArray();
                            String img = we.get(0).getAsJsonObject().get("icon").getAsString();
                            ChargerIcon(img);
                            JsonObject coord = result.get("coord").getAsJsonObject();
                            Double la = coord.get("lat").getAsDouble();
                            Double lo = coord.get("lon").getAsDouble();
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url("https://weatherbit-v1-mashape.p.rapidapi.com/forecast/daily?lat="+la+"&lon="+lo+"&lang=fr")
                                    .get()
                                    .addHeader("X-RapidAPI-Key", "2a5cf1a16dmsh9a79e14890050a0p126ffbjsn98fd5dd08466")
                                    .addHeader("X-RapidAPI-Host", "weatherbit-v1-mashape.p.rapidapi.com")
                                    .build();

                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    Toast.makeText(MainActivity.this,"erreur",Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    if(response.isSuccessful()){
                                        String ch = response.body().string();

                                        MainActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                List1.clear();
                                                JSONObject Jobject = null;
                                                try {
                                                    Jobject = new JSONObject(ch);
                                                } catch (JSONException jsonException) {
                                                    jsonException.printStackTrace();
                                                }
                                                JSONArray Jarray = null;
                                                try {
                                                    Jarray = Jobject.getJSONArray("data");
                                                } catch (JSONException jsonException) {
                                                    jsonException.printStackTrace();
                                                }
                                                for (int i = 1; i < 8; i++) {
                                                    try {
                                                        JSONObject  object = Jarray.getJSONObject(i);
                                                        String dt = object.getString("datetime");
                                                        String T = object.getString("temp");
                                                        String all = "Pour le jour "+dt+" la temperature est : "+T+" °C";
                                                        List1.add(all);
                                                    } catch (JSONException jsonException) {
                                                        jsonException.printStackTrace();
                                                    }
                                                }

                                            }
                                        });
                                    }
                                    //Toast.makeText(MainActivity.this,ch,Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
        arrayAdapter.notifyDataSetChanged();
    }
    private void ChargerMeteoWilaya(double lat,double lon) {
        Ion.with(this)
                .load("https://api.openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lon+"&lang=fr&appid="+API_KEY)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        // do stuff with the result or error
                        if (e != null) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Server error", Toast.LENGTH_LONG).show();
                        } else {
                            JsonObject main = result.get("main").getAsJsonObject();
                            Double temp = main.get("temp").getAsDouble();
                            TVtemp.setText(temp + " °C");
                            JsonObject sys = result.get("sys").getAsJsonObject();
                            String pays = sys.get("country").getAsString();
                            String w = result.get("name").getAsString();
                            TVW.setText(w+ " " + pays);
                            JsonArray we = result.get("weather").getAsJsonArray();
                            String img = we.get(0).getAsJsonObject().get("icon").getAsString();
                            ChargerIcon(img);
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url("https://weatherbit-v1-mashape.p.rapidapi.com/forecast/daily?lat="+lat+"&lon="+lon)
                                    .get()
                                    .addHeader("X-RapidAPI-Key", "2a5cf1a16dmsh9a79e14890050a0p126ffbjsn98fd5dd08466")
                                    .addHeader("X-RapidAPI-Host", "weatherbit-v1-mashape.p.rapidapi.com")
                                    .build();

                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(Call call, IOException e) {
                                    Toast.makeText(MainActivity.this,"erreur",Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onResponse(Call call, Response response) throws IOException {
                                    if(response.isSuccessful()){
                                        String ch = response.body().string();

                                        MainActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                List1.clear();
                                                JSONObject Jobject = null;
                                                try {
                                                    Jobject = new JSONObject(ch);
                                                } catch (JSONException jsonException) {
                                                    jsonException.printStackTrace();
                                                }
                                                JSONArray Jarray = null;
                                                try {
                                                    Jarray = Jobject.getJSONArray("data");
                                                } catch (JSONException jsonException) {
                                                    jsonException.printStackTrace();
                                                }
                                                for (int i = 1; i < 8; i++) {
                                                    try {
                                                        JSONObject  object = Jarray.getJSONObject(i);
                                                        String dt = object.getString("datetime");
                                                        String T = object.getString("temp");
                                                        String all = "Pour le jour "+dt+" la temperature est : "+T+" °C";
                                                        List1.add(all);
                                                    } catch (JSONException jsonException) {
                                                        jsonException.printStackTrace();
                                                    }
                                                }


                                            }
                                        });
                                    }
                                    //Toast.makeText(MainActivity.this,ch,Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
               arrayAdapter.notifyDataSetChanged();

    }
    private void ChargerIcon(String img) {
        Ion.with(this)
                .load("https://api.openweathermap.org/img/w/"+img+".png").intoImageView(ImgW);
    }

    private boolean IsGPSEnabled() {
        LocationManager locationManager = null;
        boolean isEnabled = false;
        if(locationManager == null){
            locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        }
        isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isEnabled;
    }
}
