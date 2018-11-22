package junpu.junpu;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Background extends Service {

    AudioManager mAudioManager;

    // GPS stuff
    Location mCurrentLocation = null;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    //Transition stuff
    List<ActivityTransition> transitions = new ArrayList<>();
    ActivityTransitionRequest request = null;
    private PendingIntent mPendingIntent;
    private TransitionsReceiver mTransitionsReceiver;
    private final String TRANSITIONS_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";

    //Phone unlock/lock
    private PhoneUnlockedReceiver mUnlockReceiver;

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
    final float normalspeed = 5.0f;
    final float rainspeed = 4.0f;
    final float intersectionspeed = 4.1f;
    final float threshold = 0.00001f;

    // Keeping track of previous locations
    double prevlat, prevlong;
    public boolean firstLocation = true;

    // Weather stuff
    int currentday = 0;
    Integer[] condition;
    Boolean todayRain;

    // state checking
    public STATE state = STATE.NOT_BIKING;
    public int atIntersection = 0;
    public int wrongLaneCount = 0;
    public int raining = 0;

    //violations
    public boolean phoneViolation = false;
    public boolean speeding = false;
    public boolean intersectionSpeeding = false;
    public boolean rainBiking = false;
    public boolean wrongLane = false;
    public long startTime;
    public boolean musicViolation = false;

    @Override
    public void onCreate(){
        super.onCreate();
//        Log.d("TAG", "Service: onCreate");

        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        mTransitionsReceiver = new TransitionsReceiver();
        registerReceiver(mTransitionsReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));

        // create the lists
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_BICYCLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_BICYCLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        request = new ActivityTransitionRequest(transitions);

        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, mPendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i("TAG", "Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("TAG", "Transitions Api could not be registered: " + e);
                    }
                });

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
        filter.addAction("android.intent.action.PHONE_STATE");
        registerReceiver(mUnlockReceiver, filter);

        //audio manager
        mAudioManager= (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.registerAudioPlaybackCallback(new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                super.onPlaybackConfigChanged(configs);
                for(AudioPlaybackConfiguration config: configs){
                    AudioAttributes attribute = config.getAudioAttributes();
                    if(attribute.getContentType() == AudioAttributes.CONTENT_TYPE_MUSIC){
                        if(state == STATE.BIKING){
                            musicViolation = true;
                        }
                    }
                }
            }
        },null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
//        Log.d("TAG", "Service: onStartCommand");
        return START_STICKY;
    }

    //initialize violations and information.(States are not handled here)
    private void beginBikeSession(){

        Log.e("TAG", "beginBikeSession");
        vibrate();

        //general stats
        totalDistance = 0;
        phoneViolation = false;
        musicViolation = false;
        speeding = false;
        intersectionSpeeding = false;
        rainBiking = false;
        wrongLane = false;
        firstLocation = true;
        atIntersection = 0;
        raining = 0;
        prevlat = 0;
        prevlong = 0;
        wrongLaneCount = 0;
        startTime = Calendar.getInstance().getTimeInMillis();

        //carry on phone violation
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isInteractive();
        if(isScreenOn){
            phoneViolation = true;
        }
        //carry on music violation
        if(mAudioManager.isMusicActive())
        {
            musicViolation = true;
        }

        //GPS
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                if(!haveNetworkConnection() || !isLocationEnabled(Background.this)){
                    //Send notification to ask to turn it on
                    Log.e("TAG","Invalid location update(no connection)");

                    //Destroy the service itself
                    //stopSelf();
                } else {
                    for (Location location : locationResult.getLocations()) {
                        // Update UI with location data
                        //Log.e("TAG", "Valid location update");
                        Calendar cal2;
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Background.this);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        cal2 = Calendar.getInstance();
                        int tempday = cal2.get(Calendar.DAY_OF_MONTH);
                        if (currentday == 0) {
                            currentday = tempday;
                        } else if (currentday != tempday) {
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

                        if (state == STATE.BIKING) {
                            if (firstLocation) {
                                firstLocation = false;
                            } else {
                                if (mCurrentLocation != null) {
                                    float temp = location.distanceTo(mCurrentLocation);
                                    totalDistance += temp;
                                }
                                mCurrentLocation = location;

                                //Log.e("TAG", "Total distance: " + String.valueOf(totalDistance));
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                float speed = location.getSpeed();

                                Log.d("TAG", latitude + "," + longitude);

                                int idx;
                                double right1lat, right1long, right2lat, right2long, left1lat, left1long, left2lat, left2long;

                                // linear search for wrong lane
                                for (idx = 1; idx < roads.size(); idx++) {
                                    String whichSideOfRoad;
                                    String whichWayDriving = null;
                                    String orientation = roads.get(idx).get(9);
                                    boolean inRoad = false;
                                    String description = roads.get(idx).get(0);
                                    left1lat = Double.parseDouble(roads.get(idx).get(1));
                                    left1long = Double.parseDouble(roads.get(idx).get(2));
                                    left2lat = Double.parseDouble(roads.get(idx).get(3));
                                    left2long = Double.parseDouble(roads.get(idx).get(4));
                                    right1lat = Double.parseDouble(roads.get(idx).get(5));
                                    right1long = Double.parseDouble(roads.get(idx).get(6));
                                    right2lat = Double.parseDouble(roads.get(idx).get(7));
                                    right2long = Double.parseDouble(roads.get(idx).get(8));

                                    if (orientation.equals("V")) {
                                        if (left1long - left2long == 0) {
                                            if (latitude <= left1lat && latitude >= left2lat && longitude >= left1long && longitude <= right1long) {
                                                // we inside a road bois
                                                Log.e("TAG", "ON A ROAD: " + description);
                                                inRoad = true;
                                                if (prevlat > 0.0d) {
                                                    if (latitude - prevlat > 0 + threshold) {
                                                        whichWayDriving = "R";
                                                    } else if (latitude - prevlat < 0 - threshold){
                                                        whichWayDriving = "L";
                                                    }
                                                }

                                            }
                                        } else {
                                            double slopeL = (left1long - left2long) / (left1lat - left2lat);
                                            double slopeR = (right1long - right2long) / (right1lat - right2lat);
                                            if (latitude <= left1lat && latitude >= left2lat && longitude >= slopeL * (latitude - left2lat) + left2long && longitude <= slopeR * (latitude - right2lat) + right2long) {
                                                // we inside a road bois
                                                Log.e("TAG", "ON A ROAD: " + description);
                                                inRoad = true;
                                                if (prevlat > 0.0d) {
                                                    if (latitude - prevlat > 0 + threshold) {
                                                        whichWayDriving = "R";
                                                    } else if (latitude - prevlat < 0 - threshold){
                                                        whichWayDriving = "L";
                                                    }
                                                }

                                            }
                                        }
                                    } else {
                                        if (left1lat - left2lat == 0) {
                                            if (latitude <= left1lat && latitude >= right2lat && longitude <= left1long && longitude >= left2long) {
                                                // we inside a road bois
                                                Log.e("TAG", "ON A ROAD: " + description);
                                                inRoad = true;
                                                if (prevlong > 0.0d) {
                                                    if (longitude - prevlong > 0 + threshold) {
                                                        whichWayDriving = "R";
                                                    } else if (longitude - prevlong < 0 - threshold) {
                                                        whichWayDriving = "L";
                                                    }
                                                }
                                            }
                                        } else {
                                            double slopeL = (left1lat - left2lat) / (left1long - left2long);
                                            double slopeR = (right1lat - right2lat) / (right1long - right2long);
                                            if (longitude <= left1long && longitude >= left2long && latitude >= slopeR * (longitude - right2long) + right2lat && latitude <= slopeL * (longitude - left2long) + left2lat) {
                                                // we inside a road bois
                                                Log.e("TAG", "ON A ROAD: " + description);
                                                inRoad = true;
                                                if (prevlong > 0.0d) {
                                                    if (longitude - prevlong > 0 + threshold) {
                                                        whichWayDriving = "R";
                                                    } else if (longitude - prevlong < 0 - threshold){
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
                                        if (whichWayDriving != null && !whichSideOfRoad.equals(whichWayDriving)) {
                                            wrongLane = true;
                                            wrongLaneCount++;
                                            Log.e("TAG", "WRONG LANE MOTHER");
                                        }
                                        break;
                                    }
                                }

                                prevlat = latitude;
                                prevlong = longitude;

                                //linear search for intersection
                                for (idx = 1; idx < intersections.size(); idx++) {
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
                                            Log.e("TAG", "ON AN INTERSECTION: " + intersections.get(idx).get(0));
                                            break;
                                        }
                                    }
                                }
                                if (idx == intersections.size()) {
                                    atIntersection = 0;
                                }

                                // speed checking
                                if (speed > normalspeed) {
                                    speeding = true;
                                    Log.d("TAG", "speeding: " + speed);
                                }
                                if (speed > intersectionspeed && atIntersection == 1) {
                                    intersectionSpeeding = true;
                                    Log.d("TAG", "inter: " + speed);
                                }
                                if (speed > rainspeed && raining == 1) {
                                    rainBiking = true;
                                    Log.d("TAG", "rain: " + speed);
                                }
                            }
                        }
                    }
                }
            }
        };
        createLocationRequest();
        startLocationUpdates();
    }

    //check violations and send to server.
    public void endBikeSession(){
        stopLocationUpdates();

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

        Log.d("TAG", "Penalties: " + phoneViolation + speeding + rainBiking + intersectionSpeeding + wrongLaneCount);

        if(phoneViolation){
            penaltyArray.put("phone");
        }
        if(speeding){
            penaltyArray.put("speed");
        }
        if(rainBiking){
            penaltyArray.put("weather");
        }
        if(intersectionSpeeding){
            penaltyArray.put("intersection");
        }
        if(wrongLaneCount < 20){
            penaltyArray.put("lane");
        }
        if(musicViolation){
            penaltyArray.put("music");
        }

        try {
            session.put("android_id", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
            session.put("duration", durationSeconds);
            session.put("distance", totalDistance);
            session.put("penalty", penaltyArray);
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
                                Log.e("TAG", "JSON RESPONSE SUCCESS");
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

    /**
     * A basic BroadcastReceiver to handle intents from from the Transitions API.
     */
    public class TransitionsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {
                Log.e("TAG", "received something weird af");
                return;
            }
            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    // per event
                    switch(event.getTransitionType()) {
                        //relevant types
                        case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                            state = STATE.BIKING;
                            beginBikeSession();
                            break;
                        case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                            state = STATE.NOT_BIKING;
                            endBikeSession();
                            break;
                    }
                }
            }
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(Background.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback,null /* Looper */);
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.LOCATION_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(Constants.FASTEST_LOCATION_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setSmallestDisplacement(0);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

                            int[] temp = weather.getConditions();
                            Integer[] newArray = new Integer[temp.length];
                            for (int i = 0; i < temp.length; i++) {
                                newArray[i] = temp[i];
                            }
                            condition = newArray;
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