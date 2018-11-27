package junpu.junpu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.bignerdranch.expandablerecyclerview.Model.ParentObject;
import junpu.junpu.Adapter.MyAdapter;
import junpu.junpu.Models.Child;
import junpu.junpu.Models.TitleCreator;
import junpu.junpu.Models.Parent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BikeSessions extends AppCompatActivity{

    RecyclerView recyclerView;

    JSONObject data;

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
//        ((MyAdapter)recyclerView.getAdapter()).onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bike_session);

        Log.e("TAG", "bike sessions started");

        Intent intent = getIntent();
        JSONArray array = null;
        try {
            data = new JSONObject(intent.getStringExtra("DATA"));
            array = data.getJSONArray("sessions");
            System.out.println(array.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RecyclerView recyclerView;

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

//        MyAdapter adapter = new MyAdapter(this,testingOnly());
//
        MyAdapter adapter = null;
        try {
            Log.e("TAG","fuck");
            adapter = new MyAdapter(this, initData(array));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        adapter.setParentClickableViewAnimationDefaultDuration();
        adapter.setParentAndIconExpandOnClick(true);

        recyclerView.setAdapter(adapter);
    }


    private List<ParentObject> initData(JSONArray array) throws JSONException {

        Log.e("TAG", array.toString(2));

        List<ParentObject> parentObject = new ArrayList<>();

        int numberOfSessions = array.length();
        for(int i =0; i < numberOfSessions; i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            //general information
            String date = jsonObject.getString("datetime");
            String duration = jsonObject.getString("duration");
            String distance = jsonObject.getString("distance");
            String penalty = jsonObject.getString("penalty");
            JSONArray penaltyArr = new JSONArray(penalty);
            int temp = Integer.parseInt(duration)/300 - penaltyArr.length();
            if(temp < 0 ){
                temp = 0;
            }
            String points = String.valueOf(temp);

            //violations
            Boolean speedingCheck;
            Boolean weatherCheck;
            Boolean phoneCheck;
            Boolean laneCheck;
            Boolean intersectionCheck;
            if(penalty.contains("speed")){
                speedingCheck = true;
            }else{
                speedingCheck = false;
            }
            if(penalty.contains("weather")){
                weatherCheck = true;
            }else{
                weatherCheck = false;
            }
            if(penalty.contains("phone")){
                phoneCheck = true;
            }else{
                phoneCheck = false;
            }
            if(penalty.contains("lane")){
                laneCheck = true;
            }else{
                laneCheck = false;
            }
            if(penalty.contains("intersection")){
                intersectionCheck = true;
            }else{
                intersectionCheck = false;
            }

            //create parent
            Parent parent = new Parent(date);

            //each parent must have a list of children(in my case, i only have one child)
            List<Object> childList = new ArrayList<>();
            childList.add(new Child(points, /*distance, duration,*/ speedingCheck, weatherCheck, phoneCheck, laneCheck, intersectionCheck));

            //add to parent
            parent.setChildObjectList(childList);

            //add parent to list of parents.
            parentObject.add(parent);
        }
        return parentObject;
    }

    private List<ParentObject> testingOnly(){
        TitleCreator titleCreator = TitleCreator.get(this);
        List<Parent> titles = titleCreator.getAll();
        List<ParentObject> parentObject = new ArrayList<>();
        int i =0;
        for(Parent title: titles){
            List<Object> childList = new ArrayList<>();
            if(i%2 == 0){
                childList.add(new Child(String.format("Points: %d",i), /*String.format("Distance: %d",i),
                        String.format("Duration: %d",i),*/true,false,true,false,true));
            }else{
                childList.add(new Child(String.format("Points: %d",i), /*String.format("Distance: %d",i),
                        String.format("Duration: %d",i),*/false,true,false,true,false));
            }
            title.setChildObjectList(childList);
            parentObject.add(title);
            i++;
        }
        return parentObject;
    }

    public void goToMain(View view){
        finish();
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    public void goToAchievementsFromHistory(View view) {
        Intent intent = new Intent(getApplicationContext(), Achievements.class);
        intent.putExtra("DATA", data.toString());
        finish();
        startActivity(intent);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }
}