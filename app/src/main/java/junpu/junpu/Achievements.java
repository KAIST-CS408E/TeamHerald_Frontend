package junpu.junpu;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Achievements extends AppCompatActivity{

    JSONObject data;
    JSONArray list;

    ArrayList<Integer> collected = new ArrayList<Integer>();

    AlertDialog achievementDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.achievements);

        // Create dialog screen for adding friends
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(R.layout.achievement_popup);

        achievementDialog = builder.create();
        achievementDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Intent intent = getIntent();
        try {
            data = new JSONObject(intent.getStringExtra("DATA"));
            list = new JSONArray(data.getJSONObject("user_data").getString("achievements_list"));
            for(int i = 0; i < list.length(); i++) {
                collected.add(list.getInt(i));
            }

            for(int i = 1; i < 21; i++) {
                if(i == 6)
                    continue;

                int viewId = getResources().getIdentifier("imageView" + i, "id", getPackageName());
                if(viewId != 0) {
                    ImageView view = findViewById(viewId);

                    if(collected.contains(i))
                        view.setBackgroundResource(R.drawable.achievement_card_yes);

                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            achievementDialog.show();

                            String name = getResources().getResourceEntryName(view.getId());
                            int achievementId = Integer.parseInt(name.replace("imageView", ""));
                            ImageView imgView = achievementDialog.findViewById(R.id.achievement_img);
                            int backgroundId = getResources().getIdentifier("a" + achievementId, "drawable", getPackageName());
                            imgView.setImageResource(backgroundId);

                            TextView achievementName = achievementDialog.findViewById(R.id.achievement_name);
                            achievementName.setText(utils.achievementsList[achievementId - 1][0]);
                            TextView achievementDescription = achievementDialog.findViewById(R.id.achievement_description);
                            achievementDescription.setText(utils.achievementsList[achievementId - 1][1]);

                            if(collected.contains(achievementId))
                                imgView.setBackgroundResource(R.drawable.achievement_card_yes);
                            else
                                imgView.setBackgroundResource(R.drawable.achievement_card_no);
                        }
                    });
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void goToLobbyFromAchievements(View view) {
        finish();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    public void goToHistoryFromAchievements(View view) {
        Intent intent = new Intent(getApplicationContext(), BikeSessions.class);
        intent.putExtra("DATA", data.toString());
        finish();
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }
}
