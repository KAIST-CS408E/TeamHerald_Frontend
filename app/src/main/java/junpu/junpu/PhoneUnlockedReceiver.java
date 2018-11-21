package junpu.junpu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
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

        if(intent.hasExtra(TelephonyManager.EXTRA_STATE)) {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                Log.e("TAG", "answer call");

                //answered while biking
                if(background.state == Background.STATE.BIKING) {
                    background.phoneViolation = true;
                    Log.e("TAG", String.valueOf(background.phoneViolation));
                }
            }
        }

        if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            Log.e("TAG", "PHONE UNLOCKED");
            //pressed while biking
            if(background.state == Background.STATE.BIKING) {
                background.phoneViolation = true;
                Log.e("TAG", String.valueOf(background.phoneViolation));
            }
        }
        else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
            Log.e("TAG", "PHONE LOCKED");
        }
    }

}
