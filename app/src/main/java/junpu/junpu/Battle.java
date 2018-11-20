package junpu.junpu;

import android.app.Dialog;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class Battle extends AppCompatActivity {
    RequestQueue requestQueue;

    JSONObject userData;
    JSONObject battleData;

    TextView oppUsername;
    TextView oppLevel;
    TextView oppHealthbar;
    ImageView oppPrimary;
    ImageView oppSecondary;
    ImageView oppTertiary;

    TextView userUsername;
    TextView userLevel;
    TextView userHealthbar;
    TextView userEnergybar;
    ImageView userPrimary;
    ImageView userSecondary;
    ImageView userTertiary;
    ImageView laser1;
    ImageView laser2;

    Button btnFire;

    ConstraintLayout container;
    TransitionSet set;
    Transition transition;

    int remainingEnergy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);

        oppUsername = (TextView) findViewById(R.id.opponent_username);
        oppLevel = (TextView) findViewById(R.id.opponent_level);
        oppHealthbar = (TextView) findViewById(R.id.opponent_healthbar);
        oppPrimary = (ImageView) findViewById(R.id.opp_primary);
        oppSecondary = (ImageView) findViewById(R.id.opp_secondary);
        oppTertiary = (ImageView) findViewById(R.id.opp_tertiary);

        userUsername = (TextView) findViewById(R.id.user_username);
        userLevel = (TextView) findViewById(R.id.user_level);
        userHealthbar = (TextView) findViewById(R.id.user_healthbar);
        userEnergybar = (TextView) findViewById(R.id.user_energybar);
        userPrimary = (ImageView) findViewById(R.id.user_primary);
        userSecondary = (ImageView) findViewById(R.id.user_secondary);
        userTertiary = (ImageView) findViewById(R.id.user_tertiary);
        laser1 = findViewById(R.id.img_laser_1);
        laser2 = findViewById(R.id.img_laser_2);

        btnFire = findViewById(R.id.btn_fire);

        container = (ConstraintLayout) findViewById(R.id.battle_scene);
        set = new TransitionSet();
        transition = new ChangeBounds();
        transition.addTarget(userEnergybar);
        set.addTransition(transition);
        transition = new ChangeBounds();
        transition.addTarget(oppHealthbar).setStartDelay(200);
        set.addTransition(transition);
        transition = new ChangeBounds();
        transition.addTarget(laser1);
        set.addTransition(transition);
        transition = new ChangeBounds();
        transition.addTarget(laser2);
        set.addTransition(transition);
        ImageView explosion = findViewById(R.id.img_explosion);
        transition = new ChangeBounds();
        transition.addTarget(explosion).setStartDelay(200);
        set.addTransition(transition);

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
        try {
            remainingEnergy = userData.getInt("energy");
            if(remainingEnergy < 10){
                TextView msg = findViewById(R.id.msg_no_energy);
                btnFire.setVisibility(View.INVISIBLE);
                msg.setVisibility(View.VISIBLE);
            }

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

    public void fireLaser(View view) {
        try {
            if(remainingEnergy < 10)
                return;

            String[] keys = {"user_id", "opp_id"};
            String[] values = {userData.getString("user_id"), battleData.getString("opp_id")};
            JSONObject data = utils.createJSONObj(keys, values);

            TransitionManager.endTransitions(container);

            ConstraintLayout.LayoutParams paramsLaser1 = (ConstraintLayout.LayoutParams) laser1.getLayoutParams();
            paramsLaser1.topMargin = getResources().getDimensionPixelSize(R.dimen.laser_top_top_margin);
            laser1.setLayoutParams(paramsLaser1);
            ConstraintLayout.LayoutParams paramsLaser2 = (ConstraintLayout.LayoutParams) laser2.getLayoutParams();
            paramsLaser2.topMargin = getResources().getDimensionPixelSize(R.dimen.laser_top_top_margin);
            laser2.setLayoutParams(paramsLaser2);

            String url = utils.URL + "fire_laser";
            JsonObjectRequest req = new JsonObjectRequest(url, data,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                TransitionManager.beginDelayedTransition(container, set);

                                int remainingHp = response.getInt("remaining_hp");
                                if(response.getBoolean("won_battle"))
                                    remainingHp = 0;

                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) oppHealthbar.getLayoutParams();
                                params.width = (int) remainingHp * getResources().getDimensionPixelSize(R.dimen.bar_size) / 100;
                                if(remainingHp == 0)
                                    params.width = getResources().getDimensionPixelSize(R.dimen.empty_bar_size);
                                oppHealthbar.setLayoutParams(params);

                                remainingEnergy = response.getInt("remaining_energy");
                                if(remainingEnergy < 10){
                                    TextView msg = findViewById(R.id.msg_no_energy);
                                    btnFire.setVisibility(View.INVISIBLE);
                                    msg.setVisibility(View.VISIBLE);
                                }
                                params = (ConstraintLayout.LayoutParams) userEnergybar.getLayoutParams();
                                params.width = (int) remainingEnergy * getResources().getDimensionPixelSize(R.dimen.bar_size) / 100;
                                if(remainingEnergy == 0)
                                    params.width = getResources().getDimensionPixelSize(R.dimen.empty_bar_size);
                                else if(remainingEnergy > 100)
                                    params.width = getResources().getDimensionPixelSize(R.dimen.bar_size);
                                userEnergybar.setLayoutParams(params);

                                ConstraintLayout.LayoutParams paramsLaser1 = (ConstraintLayout.LayoutParams) laser1.getLayoutParams();
                                paramsLaser1.topMargin = getResources().getDimensionPixelSize(R.dimen.laser_top_margin);
                                laser1.setLayoutParams(paramsLaser1);
                                ConstraintLayout.LayoutParams paramsLaser2 = (ConstraintLayout.LayoutParams) laser2.getLayoutParams();
                                paramsLaser2.topMargin = getResources().getDimensionPixelSize(R.dimen.laser_top_margin);
                                laser2.setLayoutParams(paramsLaser2);

                                if(response.getBoolean("won_battle")) {
                                    ImageView explosion = findViewById(R.id.img_explosion);
                                    ConstraintLayout.LayoutParams layout = (ConstraintLayout.LayoutParams) explosion.getLayoutParams();
                                    layout.width = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                                    layout.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                                    explosion.setLayoutParams(layout);

                                    btnFire.setVisibility(View.INVISIBLE);
                                    TextView msg = findViewById(R.id.msg_victory);
                                    msg.setVisibility(View.VISIBLE);
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

    public void goToLobby(View view) {
        finish();
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
    }
}
