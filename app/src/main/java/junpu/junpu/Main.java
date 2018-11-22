package junpu.junpu;

import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.json.JSONException;

public class Main extends AppCompatActivity {
    RequestQueue requestQueue;

    EditText inputNewUser;
    Button btnSubmitId;
    TextView errorMsg;

    String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestQueue = Volley.newRequestQueue(this);  // This setups up a new request queue which we will need to make HTTP requests.

        // Check if device has user
        String url = utils.URL + "user_info?id=" + Settings.Secure.ANDROID_ID;
        //String url = utils.URL + "user_info?id=" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Show and setup main screen, or show registration screen
                        if (response.has("userData")) {
                            moveToLobby(response);
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

    @Override
    protected void onResume() {
        super.onResume();
        // This setups up a new request queue which we will need to make HTTP requests.

        // Check if device has user
        String url = utils.URL + "user_info?id=" + Settings.Secure.ANDROID_ID;
        //String url = utils.URL + "user_info?id=" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Show and setup main screen, or show registration screen
                        if (response.has("userData")) {
                            moveToLobby(response);
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

    // Verify user ID is not already in use, and then move to ship customization page
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

    // Move to ship customization page
    private void moveToCustomization() {
        Intent intent = new Intent(this, CustomizeShip.class);
        intent.putExtra("junpu.junpu.USERNAME", userName);
        startActivity(intent);
    }

    private void moveToLobby(JSONObject data) {
        Intent intent = new Intent(this, Lobby.class);
        intent.putExtra("junpu.junpu.DATA", data.toString());
        startActivity(intent);
    }
}