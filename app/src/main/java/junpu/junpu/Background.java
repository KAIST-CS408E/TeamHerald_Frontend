package junpu.junpu;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Background extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, SensorEventListener {

    //Location stuff
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation= null, lStart = null, lEnd=null;

    //Activity Recognition stuff
    private Intent mIntentService;
    private PendingIntent mPendingIntent;
    private ActivityRecognitionClient mActivityRecognitionClient;

    //Phone unlock/lock
    private PhoneUnlockedReceiver mUnlockReceiver;

    //notifications
    NotificationManager mNotificationManager;

    //Bike state
    public enum STATE{
        NOT_BIKING,BIKING
    }

    //General stats
    public float totalDistance;

    // list of intersections and roads
    List<List<String>> intersections;
    List<List<String>> roads;

    // Constants/multipliers for weather conditions
    final int normalspeed = 7;
    final float rainspeedmultiplier = 0.4f;
    final float intersectionspeedmultiplier = 0.6f;

    // Keeping track of previous locations
    double prevlat, prevlong;

    // Weather stuff
    int currentday = 0;
    int[] condition;
    Boolean todayRain;

    // state checking
    public STATE state = STATE.NOT_BIKING;
    public int atIntersection = 0;
    public int raining = 0;

    //violations
    public boolean phoneViolation = false;
    public boolean speeding = false;
    public boolean intersectionSpeeding = false;
    public boolean rainBiking = false;
    public boolean wrongLane = false;
    public long startTime;


    @Override
    public void onCreate(){
        super.onCreate();
//        Log.d("TAG", "Service: onCreate");

        // create the lists
        intersections = new ArrayList<List<String>>();

        for(String line : Constants.intersections_string)
        {
            String[] linePieces = line.split(",");
            List<String> csvPieces = new ArrayList<String>(linePieces.length);
            for(String piece : linePieces)
            {
                csvPieces.add(piece);
            }
            intersections.add(csvPieces);
        }

        roads = new ArrayList<List<String>>();

        for(String line : Constants.roads_string)
        {
            String[] linePieces = line.split(",");
            List<String> csvPieces = new ArrayList<String>(linePieces.length);
            for(String piece : linePieces)
            {
                csvPieces.add(piece);
            }
            roads.add(csvPieces);
        }

        //notifications
        final NotificationCompat.Builder builder = getNotificationBuilder(this,
                "CHANNEL_ID", // Channel id
                NotificationManagerCompat.IMPORTANCE_LOW);
        builder.setOngoing(true);
        Notification notification = builder.build();
        this.startForeground(1, notification);

        //set up broadcast receiver
        mUnlockReceiver = new PhoneUnlockedReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mUnlockReceiver, filter);

        //set up activity recognition
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        mIntentService = new Intent(this, Background.class);
        mIntentService.putExtra("KEY","ACTIVITY");
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService, PendingIntent.FLAG_UPDATE_CURRENT);
        requestActivities();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
//        Log.d("TAG", "Service: onStartCommand");


        //An activity has arrived
        if(intent != null){//if since android OS calls onstart when app quits.
            String key = intent.getStringExtra("KEY");
            if(key.contentEquals("ACTIVITY")){
                handleActivity(intent);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        if(!haveNetworkConnection() || !isLocationEnabled(this)){
            //Send notification to ask to turn it on
            Log.e("TAG","Invalid location update(no connection)");

            //Destroy the service itself
            stopSelf();
        }
        else{
            Log.e("TAG", "Valid location update");

            if (lStart == null) {
                lStart = location;
                lEnd = location;
            } else {
                lEnd = location;
                float temp = location.distanceTo(mCurrentLocation);
                totalDistance += temp;
            }
            mCurrentLocation = location;

            Log.e("TAG", "Total distance: "+String.valueOf(totalDistance));

            if (state == STATE.BIKING) {
                Log.e("TAG","I'M BIKING!");
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                float speed = location.getSpeed();

                int idx;
                double right1lat, right1long, right2lat, right2long, left1lat, left1long, left2lat, left2long;

                // linear search for wrong lane
                for (idx = 1; idx<roads.size(); idx++) {
                    String whichSideOfRoad = null;
                    String whichWayDriving = null;
                    String orientation = roads.get(idx).get(9);
                    boolean inRoad = false;
                    left1lat = Double.parseDouble(roads.get(idx).get(1));
                    left1long = Double.parseDouble(roads.get(idx).get(2));
                    left2lat = Double.parseDouble(roads.get(idx).get(3));
                    left2long = Double.parseDouble(roads.get(idx).get(4));
                    right1lat = Double.parseDouble(roads.get(idx).get(5));
                    right1long = Double.parseDouble(roads.get(idx).get(6));
                    right2lat = Double.parseDouble(roads.get(idx).get(7));
                    right2long = Double.parseDouble(roads.get(idx).get(8));

                    if (orientation == "V") {
                        if (left1long - left2long == 0) {
                            if (latitude <= left1lat && latitude >= left2lat && longitude >= left1long && longitude <= right1long) {
                                // we inside a road bois
                                Log.e("TAG","ON A ROAD!");
                                inRoad = true;
                                if (prevlat > 0.0d) {
                                    if (latitude - prevlat > 0) {
                                        whichWayDriving = "R";
                                    } else {
                                        whichWayDriving = "L";
                                    }
                                }

                            }
                        } else {
                            double slopeL = (left1long - left2long) / (left1lat - left2lat);
                            double slopeR = (right1long - right2long) / (right1lat - right2lat);
                            if (latitude <= left1lat && latitude >= left2lat && longitude >= slopeL*(latitude-left2lat) + left2long && longitude <= slopeR*(latitude-right2lat) + right2long) {
                                // we inside a road bois
                                Log.e("TAG","ON A ROAD!");
                                inRoad = true;
                                if (prevlat > 0.0d) {
                                    if (latitude - prevlat > 0) {
                                        whichWayDriving = "R";
                                    } else {
                                        whichWayDriving = "L";
                                    }
                                }

                            }
                        }
                    } else {
                        if (left1lat - left2lat == 0) {
                            if (latitude <= left1lat && latitude >= right2lat && longitude <= left1long && longitude >= left2long) {
                                // we inside a road bois
                                Log.e("TAG","ON A ROAD!");
                                inRoad = true;
                                if (prevlong > 0.0d) {
                                    if (longitude - prevlong > 0) {
                                        whichWayDriving = "R";
                                    } else {
                                        whichWayDriving = "L";
                                    }
                                }
                            }
                        } else {
                            double slopeL = (left1lat - left2lat) / (left1long - left2long);
                            double slopeR = (right1lat - right2lat) / (right1long - right2long);
                            if (longitude <= left1long && longitude >= left2long && latitude >= slopeR*(longitude-right2long) + right2lat && latitude <= slopeL*(longitude-left2long) + left2lat) {
                                // we inside a road bois
                                Log.e("TAG","ON A ROAD!");
                                inRoad = true;
                                if (prevlong > 0.0d) {
                                    if (longitude - prevlong > 0) {
                                        whichWayDriving = "R";
                                    } else {
                                        whichWayDriving = "L";
                                    }
                                }
                            }
                        }
                    }

                    if (inRoad) {
                        double disWithRight = Math.abs((right2long - right1long) * latitude - (right2lat - right1lat) * longitude + right2lat * right1long - right2long * right1lat)
                                / Math.sqrt(Math.pow(right2long - right1long, 2) + Math.pow(right2lat - right1lat, 2));
                        double disWithLeft = Math.abs((left2long - left1long) * latitude - (left2lat - left1lat) * longitude + left2lat * left1long - left2long * left1lat)
                                / Math.sqrt(Math.pow(left2long - left1long, 2) + Math.pow(left2lat - left1lat, 2));

                        if (disWithRight < disWithLeft) {
                            // Closer to RIGHT
                            whichSideOfRoad = "R";
                        } else {
                            // Closer to LEFT
                            whichSideOfRoad = "L";
                        }
                        Log.e("TAG","CHECKING WRONG LANE?!?!?!");
                        if (whichWayDriving != null && !whichSideOfRoad.equals(whichWayDriving)) {
                            wrongLane = true;
                            Log.e("TAG","WRONG LANE MOTHER FUCKER");
                        }
                        break;
                    }
                }

                prevlat = latitude;
                prevlong = longitude;

                //linear search for intersection
                for (idx = 1; idx<intersections.size(); idx++){
                    double templat = Double.parseDouble(intersections.get(idx).get(1));
                    if (latitude < templat - 0.0001) {
                        atIntersection = 0;
                        break;
                    }
                    if (latitude <= templat + 0.0001 && latitude >= templat - 0.0001) {
                        double templong = Double.parseDouble(intersections.get(idx).get(2));
                        if (longitude <= templong + 0.0001 && longitude >= templong - 0.0001) {
                            // we in the intersection bois.
                            atIntersection = 1;
                            Log.e("TAG","ON AN INTERSECTION");
                            break;
                        }
                    }

                }
                if (idx == intersections.size()) {
                    atIntersection = 0;
                }

                // speed checking
                float speedLimit = normalspeed*(rainspeedmultiplier+(1-rainspeedmultiplier)*(1-raining))*(intersectionspeedmultiplier+(1-intersectionspeedmultiplier)*(1-atIntersection));
                Log.e("TAG","AM I HERE?!?!?");
                if (speedLimit < speed) {
                    Log.e("TAG","I AM SPEEDING!");
                    speeding = true;
                    if (atIntersection == 1) {
                        intersectionSpeeding = true;
                    }
                    if (raining == 1) {
                        rainBiking = true;
                    }
                }
            }
        }
    }

    //initalise violations and information.(States are not handled here)
    private void beginBikeSession(){

        /* Connect to Google API */
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        Log.e("TAG", "beginBikeSession");
        vibrate();

        //general stats
        totalDistance = 0;
        phoneViolation = false;
        speeding = false;
        intersectionSpeeding = false;
        rainBiking = false;
        wrongLane = false;
        atIntersection = 0;
        raining = 0;
        prevlat = 0;
        prevlong = 0;
        startTime = Calendar.getInstance().getTimeInMillis();
    }

    //check violations and send to server.
    public void endBikeSession(){
        stopLocationUpdates();
        if(mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }
        lStart= null;
        lEnd = null;

        Log.e("TAG", "endBikeSession");
        vibrate();

        //general stats

        //check violations and send to server
        long endTime = Calendar.getInstance().getTimeInMillis();
        long durationMillis = endTime - startTime;
        long durationSeconds = durationMillis/1000;

        Log.e("TAG", "end stats, time: " + Long.toString(durationSeconds) + " distance: "+ Float.toString(totalDistance));
        Log.e("TAG", "phone use: " + Boolean.toString(phoneViolation));

        //Make json object
        JSONObject session = new JSONObject();
        JSONArray penaltyArray = new JSONArray();
        if(phoneViolation){
            penaltyArray.put("phone");
        }
        try {
            session.put("android_id", "8f5b7333cca13357");
            session.put("duration", durationSeconds);
            session.put("distance", totalDistance);
            session.put("phone", penaltyArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Initialise queue
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = utils.URL + "add_session";

        //Request a JSON Response
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, url, session,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(response.getBoolean("success")){
                                Log.e("TAG", "JSON RESPONSE SUCESS");
                            }else{
                                Log.e("TAG", "JSON RESPONSE FAIL");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("TAG", "JSON REQUEST ERROR");
                    }
                });

        //
        queue.add(req);
    }


    private void handleActivity(Intent intent){
        Calendar cal2;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Background.this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        cal2 = Calendar.getInstance();
        int tempday = cal2.get(Calendar.DAY_OF_MONTH);
        if (currentday == 0) {
            currentday = tempday;
        } else if (currentday != tempday){
            if (tempday - currentday == 1) {
                currentday = tempday;
                if (todayRain != null) {
                    editor.putBoolean("rain", todayRain);
                    todayRain = null;
                    editor.apply();
                }
            } else {
                editor.remove("rain");
                editor.apply();
                todayRain = null;
            }
        }

        getWeatherSnapshot();

        if (condition != null) {
            if (Arrays.asList(condition).contains(5) || Arrays.asList(condition).contains(6) || Arrays.asList(condition).contains(7) || Arrays.asList(condition).contains(8)) {
                todayRain = true;
            } else if (todayRain == null) {
                todayRain = false;
            }
        }

        boolean ytdrain = sharedPreferences.getBoolean("rain", false);
        if (todayRain != null) {
            if (ytdrain || todayRain) {
                raining = 1;
            }
        } else {
            if (ytdrain) {
                raining = 1;
            }
        }

//        Log.d("TAG", "Service: handleActivity");
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();
        DetectedActivity mostProbable = null;
        for (DetectedActivity activity : detectedActivities) {
            if(mostProbable == null){
                mostProbable = activity;
            }

            if(activity.getConfidence() >= mostProbable.getConfidence()){
                mostProbable = activity;
            }
        }

        if(mostProbable != null && mostProbable.getConfidence() >= 50){
//            Log.e("TAG", "Detected activity: " + mostProbable.getType() + ", " + mostProbable.getConfidence());
            int type = mostProbable.getType();
            int confidence = mostProbable.getConfidence();


            switch(type){
                //relevant types
                case DetectedActivity.ON_BICYCLE:{
                    if(state != STATE.BIKING){
                        beginBikeSession();
                    }
                    state = STATE.BIKING;
                    break;
                }
                case DetectedActivity.ON_FOOT:{
                    if(state == STATE.BIKING){
                        //bike session ended
                        endBikeSession();
                    }
                    state = STATE.NOT_BIKING;
                    break;
                }
                case DetectedActivity.STILL:{
                    if(state == STATE.BIKING){
                        //bike session ended
                        endBikeSession();
                    }
                    state = STATE.NOT_BIKING;
                    break;
                }
                case DetectedActivity.TILTING:{
                    if(state == STATE.BIKING){
                        //bike session ended
                        endBikeSession();
                    }
                    state = STATE.NOT_BIKING;
                    break;
                }
                case DetectedActivity.WALKING:{
                    if(state == STATE.BIKING){
                        //bike session ended
                        endBikeSession();
                    }
                    state = STATE.NOT_BIKING;
                    break;
                }

                //what to do with these?
                case DetectedActivity.RUNNING:{
                    break;
                }
                case DetectedActivity.IN_VEHICLE:{
                    break;
                }
                case DetectedActivity.UNKNOWN:{
                    break;
                }
            }
        }
        else{
            Log.e("TAG", "Confidence < 50");
        }
    }


    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(500);//1 second interval
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        mLocationRequest.setSmallestDisplacement((float)0);
    }

    private void requestActivities() {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                mPendingIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(getApplicationContext(),
                        "Successfully requested activity updates",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),
                        "Requesting activity updates failed to start",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void vibrate(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(5000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(5000);
        }
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context, String channelId, int importance) {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prepareChannel(context, channelId, importance);
            builder = new NotificationCompat.Builder(context, channelId);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        return builder;
    }

    @TargetApi(26)
    private static void prepareChannel(Context context, String id, int importance) {
        final String appName = context.getString(R.string.app_name);
        String description = "channel description";
        final NotificationManager nm = (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);

        if(nm != null) {
            NotificationChannel nChannel = nm.getNotificationChannel(id);

            if (nChannel == null) {
                nChannel = new NotificationChannel(id, appName, importance);
                nChannel.setDescription(description);
                nm.createNotificationChannel(nChannel);
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.e("TAG","onDestroy");
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("TAG", "Connected");

        //Location initialise
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    @Override
    public void onConnectionSuspended(int i) {

    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //connection checkers
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }
    private boolean haveNetworkConnection(){
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    private void getWeatherSnapshot() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Awareness.getSnapshotClient(this).getWeather()
                    .addOnSuccessListener(new OnSuccessListener<WeatherResponse>() {
                        @Override
                        public void onSuccess(WeatherResponse weatherResponse) {
                            Weather weather = weatherResponse.getWeather();
                            condition = weather.getConditions();
                            //Log.e("TAG","weather" + weather);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("TAG","Could not get weather: " + e);
                        }
                    });
        }
    }
}
