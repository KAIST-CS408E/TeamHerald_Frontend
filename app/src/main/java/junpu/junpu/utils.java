package junpu.junpu;

import org.json.JSONException;
import org.json.JSONObject;

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

}
