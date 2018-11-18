package junpu.junpu;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;

import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

import android.provider.Settings.Secure;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.json.JSONException;

public class CustomizeShip extends Activity implements OnItemSelectedListener {
    RequestQueue requestQueue;  // This is our requests queue to process our HTTP requests.

    String userID = "";
    String primaryColor = "Red";
    String secondaryColor = "Red";
    String tertiaryColor = "Red";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_ship);

        Intent intent = getIntent();
        userID = intent.getStringExtra("junpu.junpu.USERNAME");

        // set up the spinners
        List<String> categories = new ArrayList<String>();
        categories.add("Red");
        categories.add("Blue");
        categories.add("Green");
        categories.add("Gray");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner_primary = (Spinner) findViewById(R.id.choose_primary);
        spinner_primary.setOnItemSelectedListener(this);
        spinner_primary.setAdapter(dataAdapter);

        Spinner spinner_secondary = (Spinner) findViewById(R.id.choose_secondary);
        spinner_secondary.setOnItemSelectedListener(this);
        spinner_secondary.setAdapter(dataAdapter);

        Spinner spinner_tertiary = (Spinner) findViewById(R.id.choose_tertiary);
        spinner_tertiary.setOnItemSelectedListener(this);
        spinner_tertiary.setAdapter(dataAdapter);

        requestQueue = Volley.newRequestQueue(this);  // This setups up a new request queue which we will need to make HTTP requests.
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String color = parent.getItemAtPosition(position).toString();

        // store the color values and change the ship's colors depending on selection
        if(parent.getId() == R.id.choose_primary) {
            primaryColor = color;
            utils.setSpaceshipColors((ImageView) findViewById(R.id.spaceship_primary), "primary", color);
        } else if(parent.getId() == R.id.choose_secondary) {
            secondaryColor = color;
            utils.setSpaceshipColors((ImageView) findViewById(R.id.spaceship_secondary), "secondary", color);
        } else {
            tertiaryColor = color;
            utils.setSpaceshipColors((ImageView) findViewById(R.id.spaceship_tertiary), "tertiary", color);
        }
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    // TODO: move from ship customziation back to main page
    public void createUser(View v) {
        String url = utils.URL + "add_user";
        String androidID = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        String[] keys = {"android_id", "user_id", "color_1", "color_2", "color_3"};
        String[] values = {androidID, userID, primaryColor, secondaryColor, tertiaryColor};
        JSONObject data = utils.createJSONObj(keys, values);

        JsonObjectRequest req = new JsonObjectRequest(url, data ,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        TextView text = (TextView) findViewById(R.id.show_username);
                        try {
                            if(response.getBoolean("success")) {
                                moveToLobby();
                            } else {
                                text.setText("failure");
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

    private void moveToLobby() {
        String url = utils.URL + "user_info?id=" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Show and setup main screen, or show registration screen
                        if (response.has("userData")) {
                            Intent intent = new Intent(CustomizeShip.this, Lobby.class);
                            intent.putExtra("junpu.junpu.DATA", response.toString());
                            startActivity(intent);
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
}
