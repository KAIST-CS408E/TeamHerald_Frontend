package junpu.junpu;

import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import android.view.View;
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
        String item = parent.getItemAtPosition(position).toString();

        // store the color values and change the ship's colors depending on selection
        if(parent.getId() == R.id.choose_primary) {
            ImageView primaryImg = (ImageView) findViewById(R.id.spaceship_primary);
            primaryColor = item;
            switch(item) {
                case "Gray":
                    primaryImg.setImageResource(R.drawable.primary_gray);
                    break;
                case "Red":
                    primaryImg.setImageResource(R.drawable.primary_red);
                    break;
                case "Blue":
                    primaryImg.setImageResource(R.drawable.primary_navy);
                    break;
                case "Green":
                    primaryImg.setImageResource(R.drawable.primary_green);
                    break;
            }
        } else if(parent.getId() == R.id.choose_secondary) {
            ImageView secondaryImg = (ImageView) findViewById(R.id.spaceship_secondary);
            secondaryColor = item;
            switch(item) {
                case "Gray":
                    secondaryImg.setImageResource(R.drawable.secondary_gray);
                    break;
                case "Red":
                    secondaryImg.setImageResource(R.drawable.secondary_red);
                    break;
                case "Blue":
                    secondaryImg.setImageResource(R.drawable.secondary_navy);
                    break;
                case "Green":
                    secondaryImg.setImageResource(R.drawable.secondary_green);
                    break;
            }
        } else {
            ImageView secondaryImg = (ImageView) findViewById(R.id.spaceship_tertiary);
            tertiaryColor = item;
            switch(item) {
                case "Red":
                    secondaryImg.setImageResource(R.drawable.tertiary_red);
                    break;
                case "Blue":
                    secondaryImg.setImageResource(R.drawable.tertiary_navy);
                    break;
                case "Green":
                    secondaryImg.setImageResource(R.drawable.tertiary_green);
                    break;
            }
        }
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

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
                                text.setText("succesful");
                            } else {
                                text.setText("failure");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Handle Errors here
            }
        });

        requestQueue.add(req);
    }
}
