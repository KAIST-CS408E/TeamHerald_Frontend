package junpu.junpu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Achievements extends AppCompatActivity{

    JSONObject data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.achievements);

        Intent intent = getIntent();
        try {
            data = new JSONObject(intent.getStringExtra("DATA"));
            ((TextView) findViewById(R.id.textView17)).setText(data.getJSONObject("user_data").getString("user_id"));
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
