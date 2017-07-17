package it.innove;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by derrick on 2017/7/11.
 */
public class BackgroundService extends Service {

    private Context context;
    private BleBinder binder = new BleBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        int counter = 0;
        if(intent != null){
            Bundle bundle = intent.getBundleExtra("bundle");
            counter = bundle.getInt("counter");
        }
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
        this.context = this;
    }

    @Override
    public void onDestroy() {
        Log.i("RBService"," = > onDestroy ");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int counter = 0;
        if(intent != null){
            Bundle bundle = intent.getBundleExtra("bundle");
            counter = bundle.getInt("counter");

        }
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
