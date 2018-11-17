package junpu.junpu;

import org.json.JSONException;
import org.json.JSONObject;
import android.widget.ImageView;

public class utils {
    public static String URL = "https://junpu.herokuapp.com/";

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
