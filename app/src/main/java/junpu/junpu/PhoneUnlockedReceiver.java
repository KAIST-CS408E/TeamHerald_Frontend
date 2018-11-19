package junpu.junpu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import junpu.junpu.Background;

public class PhoneUnlockedReceiver extends BroadcastReceiver {

    Background background;

    public enum STATE{
        NOT_BIKING,BIKING
    }

    public PhoneUnlockedReceiver(Background background){
        this.background = background;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            Log.e("TAG", "PHONE UNLOCKED");
            //pressed while biking
            if(background.state == Background.STATE.BIKING) {
                background.phoneViolation = true;
                Log.e("TAG", String.valueOf(background.phoneViolation));
            }
            else{
                //it's fine
            }
        }
        else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
            Log.e("TAG", "PHONE LOCKED");
        }
    }

}
