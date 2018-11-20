package junpu.junpu;

import org.json.JSONException;
import org.json.JSONObject;
import android.widget.ImageView;

import java.util.ArrayList;

public class utils {
    public static String URL = "https://junpu.herokuapp.com/";

    public static String[][] achievementsList = {
            {"Victory!", "Get your first win"},
            {"Clutch", "Barely beat your opponent"},
            {"Domination", "Kill the same opponent 3 times"},
            {"Revenge", "Kill an opponent that previously killed you"},
            {"Smooth Riding", "Have a perfectly safe biking session"},
            {"Perfection", "Kill an opponent with only safe sessions"},
            {"Destroyer", "Get 50 wins"},
            {"Hunter", "Kill 5 different opponents"},
            {"Climbing the Ladder", "Get 3 wins"},
            {"Space Pirate", "Get 10 wins"},
            {"Top of the World", "Get 5 wins"},
            {"Leveling Up", "Get to Level 5"},
            {"Hobbyist", "Log 10 biking sessions"},
            {"Better Safe than Sorry", "Log 5 biking safe sessions"},
            {"Safe Landing", "Get 50 consecutive safe sessions"},
            {"Getting There", "Get 10 consecutive safe sessions"},
            {"Space Shooter", "Fire 500 times"},
            {"Easy Peasy", "Kill an opponent with more than half your health remaining"},
            {"Masterful", "Have a win/loss ratio above 60% with more than 10 battles"},
            {"Augmented Intelligence", "Get 10 kills in a row"}
    };


    public static JSONObject createJSONObj(String keys[], String values[]) {
        JSONObject result = new JSONObject();
        for(int i = 0; i < keys.length; i++) {
            try {
                result.put(keys[i], values[i]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void setSpaceshipColors(ImageView spaceshipPart, String part, String color) {
        if(part == "primary") {
            switch(color) {
                case "Gray":
                    spaceshipPart.setImageResource(R.drawable.primary_gray);
                    break;
                case "Red":
                    spaceshipPart.setImageResource(R.drawable.primary_red);
                    break;
                case "Blue":
                    spaceshipPart.setImageResource(R.drawable.primary_navy);
                    break;
                case "Green":
                    spaceshipPart.setImageResource(R.drawable.primary_green);
                    break;
            }
        } else if(part == "secondary") {
            switch(color) {
                case "Gray":
                    spaceshipPart.setImageResource(R.drawable.secondary_gray);
                    break;
                case "Red":
                    spaceshipPart.setImageResource(R.drawable.secondary_red);
                    break;
                case "Blue":
                    spaceshipPart.setImageResource(R.drawable.secondary_navy);
                    break;
                case "Green":
                    spaceshipPart.setImageResource(R.drawable.secondary_green);
                    break;
            }
        } else {
            switch(color) {
                case "Red":
                    spaceshipPart.setImageResource(R.drawable.tertiary_red);
                    break;
                case "Blue":
                    spaceshipPart.setImageResource(R.drawable.tertiary_navy);
                    break;
                case "Green":
                    spaceshipPart.setImageResource(R.drawable.tertiary_green);
                    break;
            }
        }
    }
}
