package junpu.junpu;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class Lobby extends AppCompatActivity {
    RequestQueue requestQueue;

    JSONObject userData;
    JSONArray friends;
    JSONArray sessions;
    JSONObject battleData;

    AlertDialog addFriendDialog;
    AlertDialog startBattleDialog;


    Button historyButton;

    // TODO: onResume to get user information not onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        requestQueue = Volley.newRequestQueue(this);  // This setups up a new request queue which we will need to make HTTP requests.

        Intent intent = getIntent();
        try {
            JSONObject data = new JSONObject(intent.getStringExtra("junpu.junpu.DATA"));

            userData = data.getJSONObject("userData");
            friends = data.getJSONArray("friends");
            sessions = data.getJSONArray("sessions");
            setupMainScreen();
        } catch (JSONException e){
            e.printStackTrace();
        }

        historyButton = findViewById(R.id.history_button);
        historyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("TAG", "calling bike sesions");

                Intent intent = new Intent(getApplicationContext(), BikeSessions.class);
                intent.putExtra("DATA", sessions.toString());
                startActivity(intent);
            }
        });

        // Create dialog screen for adding friends
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.fragment_dialog_add_user, null))
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        addFriendDialog = builder.create();

        // Create dialog screen for starting battle
        builder = new AlertDialog.Builder(this);
        inflater = this.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_start_battle, null))
                .setPositiveButton("Battle!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        startBattleDialog = builder.create();

        checkConnectionAndStartService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String url = utils.URL + "user_info?id=" + "8f5b7333cca13357";
        //String url = utils.URL + "user_info?id=" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject data) {
                        // Show and setup main screen, or show registration screen
                        try {
                            userData = data.getJSONObject("userData");
                            friends = data.getJSONArray("friends");
                            sessions = data.getJSONArray("sessions");
                            setupMainScreen();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle Errors here
                    }
                });

        requestQueue.add(req);
    }

    // Setup and show main screen
    private void setupMainScreen() {
        createFriendsList();

        // Display user information
        TextView textLevel = (TextView) findViewById(R.id.text_level);
        TextView textUsername = (TextView) findViewById(R.id.text_userid);
        TextView textWins = (TextView) findViewById(R.id.text_wins);
        TextView textLosses = (TextView) findViewById(R.id.text_losses);
        ImageView spaceshipPrimary = (ImageView) findViewById(R.id.img_spaceship_primary);
        ImageView spaceshipSecondary = (ImageView) findViewById(R.id.img_spaceship_secondary);
        ImageView spaceshipTertiary = (ImageView) findViewById(R.id.img_spaceship_tertiary);
        TextView energyBar = (TextView) findViewById(R.id.energy_bar);
        try {
            textLevel.setText("Lvl\n" + userData.getInt("level"));
            textUsername.setText(userData.getString("user_id"));
            textWins.setText(userData.getInt("wins") + "");
            textLosses.setText(userData.getInt("losses") + "");

            utils.setSpaceshipColors(spaceshipPrimary, "primary", userData.getString("color_1"));
            utils.setSpaceshipColors(spaceshipSecondary, "secondary", userData.getString("color_2"));
            utils.setSpaceshipColors(spaceshipTertiary, "tertiary", userData.getString("color_3"));

            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) energyBar.getLayoutParams();
            int energy = userData.getInt("energy");
            params.width = (int) energy * getResources().getDimensionPixelSize(R.dimen.bar_size) / 100;
            if(energy == 0)
                params.width = getResources().getDimensionPixelSize(R.dimen.empty_bar_size);
            else if(energy > 100)
                params.width = getResources().getDimensionPixelSize(R.dimen.bar_size);
            energyBar.setLayoutParams(params);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Handler for hiding friends list
        TextView friendsText = (TextView) findViewById(R.id.text_friends_list);
        friendsText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ScrollView friendsList = (ScrollView) findViewById(R.id.friends_list);
                ObjectAnimator animation1 = ObjectAnimator.ofFloat(friendsList, "translationX", 0);
                animation1.setDuration(500);
                animation1.start();

                TextView textFriends = (TextView) findViewById(R.id.text_friends_list);
                ObjectAnimator animation2 = ObjectAnimator.ofFloat(textFriends, "translationX", 0);
                animation2.setDuration(500);
                animation2.start();
            }
        });

        getBattleInfo();
    }

    // Display all friends data in friends list
    public void createFriendsList() {
        LinearLayout listContainer = (LinearLayout) findViewById(R.id.friends_list_container);
        if(listContainer.getChildCount() > 0)
            listContainer.removeAllViews();

        for (int i = 0; i < friends.length(); i++) {
            try {
                JSONObject friend = friends.getJSONObject(i);

                ConstraintLayout container = new ConstraintLayout(this);
                LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        getResources().getDimensionPixelSize(R.dimen.friend_entry_height));
                container.setLayoutParams(params2);
                container.setBackgroundResource(R.drawable.bottom_border);
                listContainer.addView(container, 0);

                createFriendEntryText(friend.getString("user_id"), true, container);
                createFriendEntryText("Lvl " + friend.getInt("level"), false, container);

                createFriendEntryImage(friend.getString("color_1"), "primary", container);
                createFriendEntryImage(friend.getString("color_2"), "secondary", container);
                createFriendEntryImage(friend.getString("color_3"), "tertiary", container);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Handler for showing friends list
    public void showFriendList(View view) {
        ScrollView friendsList = (ScrollView) findViewById(R.id.friends_list);
        ObjectAnimator animation1 = ObjectAnimator.ofFloat(friendsList, "translationX", -710);
        animation1.setDuration(500);
        animation1.start();

        TextView textFriends = (TextView) findViewById(R.id.text_friends_list);
        ObjectAnimator animation2 = ObjectAnimator.ofFloat(textFriends, "translationX", -710);
        animation2.setDuration(500);
        animation2.start();
    }

    // Helper function for creating each entry in friends list
    private void createFriendEntryText(String text, Boolean isID, ConstraintLayout container) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP,25);
        view.setTextColor(getResources().getColor(R.color.colorPrimary));
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.text_right_margin);
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        if(isID) {
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.text_topbot_margin);
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        } else {
            params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.text_topbot_margin);
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        }

        container.addView(view, params);
    }

    // Handler for creating spaceship image for friend list
    private void createFriendEntryImage(String color, String part, ConstraintLayout container) {
        ImageView view = new ImageView(this);
        utils.setSpaceshipColors(view, part, color);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.friend_spaceship_size),
                getResources().getDimensionPixelSize(R.dimen.friend_spaceship_size));
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.img_friend_spaceship_left);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.img_friend_spaceship_top);
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

        container.addView(view, params);
    }

    // Handler for showing add friend dialog and set up submit button click handler
    public void showFriendDialog(View view) {
        addFriendDialog.show();

        addFriendDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String username = "";
                try {
                    username = userData.getString("user_id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Dialog dialogView = ((Dialog) addFriendDialog);
                String friendUsername = ((EditText) dialogView.findViewById(R.id.friend_username)).getText().toString();

                TextView errMsg = (TextView) dialogView.findViewById(R.id.dialog_err_msg);
                errMsg.setText("");
                // Verify that user ID is not equal to friend ID
                if (friendUsername.equals(username)){
                    errMsg.setText("Invalid friend ID");
                }else{
                    // POST request for adding friend
                    String url = utils.URL + "add_friend";

                    String[] keys = {"user_id", "friend_id"};
                    String[] values = {username, friendUsername};
                    JSONObject data = utils.createJSONObj(keys, values);

                    JsonObjectRequest req = new JsonObjectRequest(url, data ,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        if(response.getBoolean("is_success")) {
                                            // Update friends data
                                            String url = utils.URL + "get_friends?user_id=" + userData.getString("user_id");
                                            JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, (String) null,
                                                    new Response.Listener<JSONArray>() {
                                                        @Override
                                                        public void onResponse(JSONArray list) {
                                                            friends = list;
                                                            createFriendsList();
                                                        }
                                                    },
                                                    new Response.ErrorListener(){
                                                        @Override
                                                        public void onErrorResponse(VolleyError error){
                                                            // TODO: handle exception here
                                                        }
                                                    });
                                            requestQueue.add(req);

                                            ((EditText) ((Dialog) addFriendDialog).findViewById(R.id.friend_username)).setText("");
                                            addFriendDialog.dismiss();
                                        } else {
                                            TextView errMsg = (TextView) ((Dialog) addFriendDialog).findViewById(R.id.dialog_err_msg);
                                            errMsg.setText("Invalid friend ID");
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    //Handle Errors here
                                }
                            });

                    requestQueue.add(req);
                }
            }
        });
    }

    // Get battle information
    private void getBattleInfo() {
        try {
            String url = utils.URL + "get_battle_info?user_id=" + userData.getString("user_id");
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            battleData = response;
                            try {
                                Button battleBtn = (Button) findViewById(R.id.btn_battle);
                                if (battleData.getBoolean("in_battle")) {
                                    battleBtn.setText("Go to Battle");
                                } else {
                                    battleBtn.setText("New Battle");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO: handle exception here
                        }
                    });
            requestQueue.add(req);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void goToOrStartBattle(View view){
        try {
            if (battleData.getBoolean("in_battle")) {
                Intent intent = new Intent(this, Battle.class);
                intent.putExtra("junpu.junpu.USERDATA", userData.toString());
                startActivity(intent);
            } else {
                startBattleDialog.show();

                Dialog dialogView = (Dialog) startBattleDialog;

                List<String> categories = new ArrayList<String>();
                for(int i = 0; i < friends.length(); i++) {
                    JSONObject temp = friends.getJSONObject(i);
                    categories.add(temp.getString("user_id"));
                }

                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                Spinner chooseFriendSpinner = (Spinner) dialogView.findViewById(R.id.choose_friend);
                chooseFriendSpinner.setAdapter(dataAdapter);

                startBattleDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v) {
                        Dialog dialogView = (Dialog) startBattleDialog;
                        String friendUsername = ((Spinner) dialogView.findViewById(R.id.choose_friend)).getSelectedItem().toString();

                        try {
                            String[] keys = {"user_id", "opp_id"};
                            String[] values = {userData.getString("user_id"), friendUsername};
                            JSONObject data = utils.createJSONObj(keys, values);
                            String url = utils.URL + "start_battle";
                            JsonObjectRequest req = new JsonObjectRequest(url, data,
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            TextView text = (TextView) findViewById(R.id.show_username);
                                            try {
                                                if (response.getBoolean("success")) {
                                                    startBattleDialog.dismiss();

                                                    Intent intent = new Intent(Lobby.this, Battle.class);
                                                    intent.putExtra("junpu.junpu.USERDATA", userData.toString());
                                                    startActivity(intent);
                                                }
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            //Handle Errors here
                                        }
                                    });

                            requestQueue.add(req);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }






    /* ------------------ Private methods ------------------ */
    private void checkConnectionAndStartService(){
        if(!haveNetworkConnection()){
            showDialog();//if they press no, app exits inside method.
        }else {
            /* Remind users to turn on location */
            if(!isLocationEnabled(getApplicationContext())){
                displayLocationSettingsRequest();
            }

            /* If both are ok, initialise background service */
            Background background = new Background();
            Intent intent = new Intent(this, Background.class);
            intent.putExtra("KEY", "START");
            if(!isMyServiceRunning(Background.class)){
                startForegroundService(intent);
            }
        }
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private void showDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Must connect to the internet to use the app")
                .setCancelable(false)
                .setPositiveButton("Connect to WIFI", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

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
    private void displayLocationSettingsRequest() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i("TAG", "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i("TAG", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(Lobby.this, 0x1);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i("TAG", "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("TAG", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }
}

