package it.innove;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.WritableMap;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by derrick on 2017/7/11.
 */
public class BackgroundService extends Service {
    private static final String TAG = "ReactNativeJS";
    private Context context;
    private BleBinder binder ;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        int counter = 0;

        Log.i(TAG," service  = > onBind : conter = "+counter);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG," service  = > onUnbind ");
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.i(TAG," service  = > onCreate ");
        super.onCreate();
        context = this;
        binder = new BleBinder(context);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG," service  = > onDestroy ");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int counter = 0;

        Log.i(TAG," service  = > onStartCommand : conter = "+counter);

        if(intent == null){
            isBackgroud = true;
            startNotification();
        } else {
            isBackgroud = false;
            //binder.disconnect(peripheralUUID,null);
        }
        return START_STICKY;//START_REDELIVER_INTENT; START_STICKY
    }
    private boolean isBackgroud = false;
    private String peripheralUUID = "AB:BC:EA:AB:BA:60";
    private String serviceUUID = "feed";
    private String characteristicUUID = "c11b1906-f27c-4874-865e-80a4336f1e97";
    private CallBackManager.PeripheralConnect connect = new CallBackManager.PeripheralConnect(){

        @Override
        public void onConnect(BluetoothDevice device) {
            Log.i(TAG," service  = > PeripheralConnect : onConnect ");
            binder.retrieveServices(peripheralUUID,services);
        }
        @Override
        public void onDisconnect(BluetoothDevice device) {
            Log.i(TAG," service  = > PeripheralConnect : onDisconnect ");
            if (isBackgroud){
                startNotification();
            }
        }
        @Override
        public void onResult(String text) {
            Log.i(TAG," service  = > PeripheralConnect : onResult => "+text);

        }


    };
    private CallBackManager.RetrieveServices services = new CallBackManager.RetrieveServices() {
        @Override
        public void onSuccessed(WritableMap map) {
            Log.i(TAG," service  = > RetrieveServices : onSuccessed ");
            binder.startNotification(peripheralUUID,serviceUUID,characteristicUUID,notification);
        }

        @Override
        public void onFailed(String text) {
            Log.i(TAG," service  = > RetrieveServices : onFailed => "+text);

        }
    };

    private CallBackManager.PeripheralNotification notification = new CallBackManager.PeripheralNotification() {
        @Override
        public void onResult(String text) {

            Log.i(TAG," service  = > PeripheralNotification : onResult => "+text);
        }

        @Override
        public void onChanged() {
            Log.i(TAG," service  = > PeripheralNotification : onChanged ");
            Intent intent = new Intent("com.roabay.luna.backgroud.action");
            Log.i(TAG," service : peripheralUUID = "+this.peripheralUUID);
            Log.i(TAG," service : serviceUUID = "+this.serviceUUID);
            Log.i(TAG," service : characteristicUUID = "+this.characteristicUUID);
            Log.i(TAG," service :  values = "+this.values);
            Bundle bundle = new Bundle();
            bundle.putString("peripheralUUID",this.peripheralUUID);
            bundle.putString("serviceUUID",this.serviceUUID);
            bundle.putString("characteristicUUID",this.characteristicUUID);
            bundle.putByteArray("values",this.values);
            intent.putExtra("data",bundle);
            context.sendBroadcast(intent);
        }
    };
    private void startNotification(){
        Log.i(TAG," service  = > startNotification : start ");
        binder.connect(this,peripheralUUID,connect);
    }
}
