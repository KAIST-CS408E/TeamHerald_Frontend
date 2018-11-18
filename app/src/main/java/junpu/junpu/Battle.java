package junpu.junpu;

import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class Battle extends AppCompatActivity {
    RequestQueue requestQueue;

    JSONObject userData;
    JSONObject battleData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        requestQueue = Volley.newRequestQueue(this);

        Intent intent = getIntent();
        try {
            userData = new JSONObject(intent.getStringExtra("junpu.junpu.USERDATA"));

            String url = utils.URL + "get_battle_info?user_id=" + userData.getString("user_id");
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            battleData = response;
                            try {
                                if (battleData.getBoolean("in_battle")) {
                                    setupBattleScreen();
                                }
                                // TODO: go back if not in battle
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

    private void setupBattleScreen() {
        TextView oppUsername = (TextView) findViewById(R.id.opponent_username);
        TextView oppLevel = (TextView) findViewById(R.id.opponent_level);
        TextView oppHealthbar = (TextView) findViewById(R.id.opponent_healthbar);
        ImageView oppPrimary = (ImageView) findViewById(R.id.opp_primary);
        ImageView oppSecondary = (ImageView) findViewById(R.id.opp_secondary);
        ImageView oppTertiary = (ImageView) findViewById(R.id.opp_tertiary);

        TextView userUsername = (TextView) findViewById(R.id.user_username);
        TextView userLevel = (TextView) findViewById(R.id.user_level);
        TextView userHealthbar = (TextView) findViewById(R.id.user_healthbar);
        TextView userEnergybar = (TextView) findViewById(R.id.user_energybar);
        ImageView userPrimary = (ImageView) findViewById(R.id.user_primary);
        ImageView userSecondary = (ImageView) findViewById(R.id.user_secondary);
        ImageView userTertiary = (ImageView) findViewById(R.id.user_tertiary);

        try {
            oppUsername.setText(battleData.getString("opp_id"));
            oppLevel.setText("Level " + battleData.getInt("opp_level"));
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) oppHealthbar.getLayoutParams();
            int oppHealth = battleData.getInt("opp_hp");
            params.width = (int) oppHealth * getResources().getDimensionPixelSize(R.dimen.bar_size) / 100;
            if(oppHealth == 0)
                params.width = getResources().getDimensionPixelSize(R.dimen.empty_bar_size);
            oppHealthbar.setLayoutParams(params);

            utils.setSpaceshipColors(oppPrimary, "primary", battleData.getString("opp_color_1"));
            utils.setSpaceshipColors(oppSecondary, "secondary", battleData.getString("opp_color_2"));
            utils.setSpaceshipColors(oppTertiary, "tertiary", battleData.getString("opp_color_3"));

            userUsername.setText(userData.getString("user_id"));
            userLevel.setText("Level " + userData.getInt("level"));
            params = (ConstraintLayout.LayoutParams) userHealthbar.getLayoutParams();
            int userHealth = battleData.getInt("user_hp");
            params.width = (int) userHealth * getResources().getDimensionPixelSize(R.dimen.bar_size) / 100;
            if(userHealth == 0)
                params.width = getResources().getDimensionPixelSize(R.dimen.empty_bar_size);
            userHealthbar.setLayoutParams(params);

            params = (ConstraintLayout.LayoutParams) userEnergybar.getLayoutParams();
            int userEnergy = userData.getInt("energy");
            params.width = (int) userEnergy * getResources().getDimensionPixelSize(R.dimen.bar_size) / 100;
            if(userEnergy == 0)
                params.width = getResources().getDimensionPixelSize(R.dimen.empty_bar_size);
            else if(userEnergy > 100)
                params.width = getResources().getDimensionPixelSize(R.dimen.bar_size);
            userEnergybar.setLayoutParams(params);

            utils.setSpaceshipColors(userPrimary, "primary", userData.getString("color_1"));
            utils.setSpaceshipColors(userSecondary, "secondary", userData.getString("color_2"));
            utils.setSpaceshipColors(userTertiary, "tertiary", userData.getString("color_3"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
