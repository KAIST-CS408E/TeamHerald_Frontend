package junpu.junpu;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import org.w3c.dom.Text;

public class Lobby extends AppCompatActivity {
    RequestQueue requestQueue;

    JSONObject userData;
    JSONArray friends;
    JSONArray sessions;

    AlertDialog dialog;

    // TODO: onResume to get user information not onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    // Setup and show main screen
    private void setupMainScreen() {
        setContentView(R.layout.main);

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
            textLevel.setText("Lvl " + userData.getInt("level"));
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

        // Create dialog screen for adding friends
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.fragment_dialog_add_user, null))
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        dialog = builder.create();
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
    public void showNoticeDialog(View view) {
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String username = "";
                try {
                    username = userData.getString("user_id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Dialog dialogView = ((Dialog) dialog);
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
                                    TextView text = (TextView) findViewById(R.id.show_username);
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

                                            dialog.dismiss();
                                        } else {
                                            TextView errMsg = (TextView) ((Dialog) dialog).findViewById(R.id.dialog_err_msg);
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
}

