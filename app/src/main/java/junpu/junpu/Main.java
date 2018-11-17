package junpu.junpu;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.provider.Settings.Secure;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.Response;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class Main extends AppCompatActivity {

    EditText inputNewUser;
    Button btnSubmitId;
    TextView textResult;
    RequestQueue requestQueue;  // This is our requests queue to process our HTTP requests.

    String baseUrl = "https://junpu.herokuapp.com/user_info?id=";
    String url;  // This will hold the full URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.inputNewUser = (EditText) findViewById(R.id.input_new_user);
        this.btnSubmitId = (Button) findViewById(R.id.btn_submit_id);
        this.textResult = (TextView) findViewById(R.id.text_result);
        this.textResult.setMovementMethod(new ScrollingMovementMethod());  // This makes our text box scrollable

        requestQueue = Volley.newRequestQueue(this);  // This setups up a new request queue which we will need to make HTTP requests.
    }

    public void moveToCustomization(View view){
        Intent intent = new Intent(this, CustomizeShip.class);
        String userName = this.inputNewUser.getText().toString();
        intent.putExtra("junpu.junpu.USERNAME", userName);
        startActivity(intent);
    }

    /*
    private void setTextView(String str){
        this.textResult.setText(str);
    }

    public void getUserInfo(View v) {
        // MAke the url with the query part
        this.url = this.baseUrl + this.inputNewUser.getText().toString();

        setTextView(this.url);

        // Next, we create a new JsonArrayRequest. This will use Volley to make a HTTP request
        // that expects a JSON Array Response.
        JsonObjectRequest arrReq = new JsonObjectRequest(Request.Method.GET, this.url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String str = response.getString("android_id");
                            str += "\n" + response.getString("user_id");
                            str += "\n" + response.getString("color_1");
                            String androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
                            str += androidId;
                            setTextView(str);
                        }catch(JSONException e){

                        }
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        setTextView("Error loading API");
                        setTextView(error.toString());
                        Log.e("Volley", error.toString());
                    }
                }
        );

        // Add the request we just defined to our request queue.
        // The request queue will automatically handle the request as soon as it can.
        requestQueue.add(arrReq);
    }
    */

}
