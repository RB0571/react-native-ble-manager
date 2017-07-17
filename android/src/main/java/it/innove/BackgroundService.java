package it.innove;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;


/**
 * Created by derrick on 2017/7/11.
 */
public class BackgroundService extends Service {

    private Context context;
    private BleBinder binder ;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        int counter = 0;

        Log.i("RBService"," = > onBind : conter = "+counter);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("RBService"," = > onUnbind ");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.i("RBService"," = > onCreate ");
        super.onCreate();
        context = this;
        binder = new BleBinder(context);
    }

    @Override
    public void onDestroy() {
        Log.i("RBService"," = > onDestroy ");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int counter = 0;

        Log.i("RBService"," = > onStartCommand : conter = "+counter);

        if(intent == null){
            new Thread(new Runnable() {
                @Override
                public void run() {
//                    binder.init();
//                    binder.startScan();
//                    binder.connect();
//                    binder.startNotification();
                }
            }).start();
        }
        return START_STICKY;//START_REDELIVER_INTENT; START_STICKY
    }
}
