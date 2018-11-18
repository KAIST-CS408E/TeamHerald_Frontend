package junpu.junpu;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
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

public class Main extends AppCompatActivity {
    RequestQueue requestQueue;

    EditText inputNewUser;
    Button btnSubmitId;
    TextView errorMsg;

    String userName;

    JSONObject userData;

    // TODO: onResume to get user information not onCreate

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestQueue = Volley.newRequestQueue(this);  // This setups up a new request queue which we will need to make HTTP requests.

        String url = utils.URL + "user_info?id=" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response.has("android_id")) {
                            userData = response;
                            setupMainScreen();
                        } else {
                            setContentView(R.layout.activity_main);

                            inputNewUser = (EditText) findViewById(R.id.input_new_user);
                            btnSubmitId = (Button) findViewById(R.id.btn_submit_id);
                            errorMsg = (TextView) findViewById(R.id.error_message);
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

    // intent to move to ship customization page
    public void verifyUserId(View view) {
        userName = this.inputNewUser.getText().toString();
        String url = utils.URL + "verify_user_id?user_id=" + userName;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("is_valid")) {
                                moveToCustomization();
                            } else {
                                errorMsg.setText("Invalid ID or already in use");
                            }
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

    private void moveToCustomization() {
        Intent intent = new Intent(this, CustomizeShip.class);

        intent.putExtra("junpu.junpu.USERNAME", userName);
        startActivity(intent);
    }

    private void setupMainScreen() {
        setContentView(R.layout.main);

        String url = utils.URL + "get_friends?user_id=";
        try {
            url += userData.getString("user_id");
        }catch (JSONException e) {
            e.printStackTrace();
        }

        url = utils.URL + "get_all_users";
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        createFriendList(response);
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        // TODO: handle exception here
                    }
                });

        requestQueue.add(req);

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
    }

    public void createFriendList(JSONArray list) {
        try {
            // TODO: add friend functionality

            LinearLayout listContainer = (LinearLayout) findViewById(R.id.friends_list_container);
            for (int i = 0; i < list.length(); i++) {
                JSONObject friend = list.getJSONObject(i);

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
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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

    public void showNoticeDialog(View view) {
        // Create an instance of the dialog fragment and show it
        AddUserDialogFragment dialog = new AddUserDialogFragment();
        Bundle bundle = new Bundle();
        try {
            bundle.putString("USERNAME", userData.getString("user_id"));
            dialog.setArguments(bundle);
            dialog.show(getSupportFragmentManager(), "AddUserDialogFragment");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

