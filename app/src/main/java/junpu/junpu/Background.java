package junpu.junpu;

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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Calendar;

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
    public STATE state = STATE.NOT_BIKING;

    //General stats
    public float totalDistance;

    //violations
    public boolean phoneViolation = false;
    public long startTime;


    @Override
    public void onCreate(){
        super.onCreate();
//        Log.d("TAG", "Service: onCreate");

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

        //Calling the method below updates the  live values of distance and speed to the TextViews.
        //calculating the speed with getSpeed method it returns speed in m/s so we are converting it into kmph
        float speed = location.getSpeed() * 18 / 5;

        Log.e("TAG", "Speed" + Float.toString(speed));
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
    }


    private void handleActivity(Intent intent){
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
}
