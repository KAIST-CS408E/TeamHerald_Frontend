package junpu.junpu;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.json.JSONException;
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

        TextView textLevel = (TextView) findViewById(R.id.text_level);
        TextView textUsername = (TextView) findViewById(R.id.text_userid);
        TextView textWins = (TextView) findViewById(R.id.text_wins);
        TextView textLosses = (TextView) findViewById(R.id.text_losses);
        ImageView spaceshipPrimary = (ImageView) findViewById(R.id.img_spaceship_primary);
        ImageView spaceshipSecondary = (ImageView) findViewById(R.id.img_spaceship_secondary);
        ImageView spaceshipTertiary = (ImageView) findViewById(R.id.img_spaceship_tertiary);
        try {
            textLevel.setText("Lvl " + userData.getInt("level"));
            textUsername.setText(userData.getString("user_id"));
            textWins.setText(userData.getInt("wins") + "");
            textLosses.setText(userData.getInt("losses") + "");

            utils.setSpaceshipColors(spaceshipPrimary, "primary", userData.getString("color_1"));
            utils.setSpaceshipColors(spaceshipSecondary, "secondary", userData.getString("color_2"));
            utils.setSpaceshipColors(spaceshipTertiary, "tertiary", userData.getString("color_3"));
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
}

