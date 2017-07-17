package it.innove;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by admin on 2017/7/11.
 */

public class BackgroundReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i("RBService","BackgroundReceiver : "+action);
        if("com.it.innove.backgroud.action".equals(action)){
            context.startService(new Intent(context,BackgroundService.class));
        }
    }
}
