package it.innove;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.WritableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private PeripheralJson peripheralJson;
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
        peripheralJson = new PeripheralJson(context);
        register();
    }

    private void register(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver,filter);
    }
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("ReactNativeJS","MainActivity : "+action);
            if (Intent.ACTION_SCREEN_OFF.equals(action)){
                context.sendBroadcast(new Intent("com.roabay.luna.backgroud.stop.action"));
            }
        }
    };
    @Override
    public void onDestroy() {
        Log.i(TAG," service  = > onDestroy ");
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int counter = 0;

        Log.i(TAG," service  = > onStartCommand : conter = "+counter);

        if(intent == null){
            isBackgroud = true;
            start();
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

    private CallBackManager.PeripheralNotification notification = new CallBackManager.PeripheralNotification() {
        @Override
        public void onResult(String text) {
            //Log.i(TAG," service  = > PeripheralNotification : onResult => "+text);
        }

        @Override
        public void onChanged() {
            //Log.i(TAG," service  = > PeripheralNotification : onChanged ");
            Intent intent = new Intent("com.roabay.luna.backgroud.action");
//            Log.i(TAG," service : peripheralUUID = "+this.peripheralUUID);
//            Log.i(TAG," service : serviceUUID = "+this.serviceUUID);
//            Log.i(TAG," service : characteristicUUID = "+this.characteristicUUID);
//            Log.i(TAG," service :  values = "+this.values);
            Bundle bundle = new Bundle();
            bundle.putString(Peripheral.PERIPHERAL_UUID,this.peripheralUUID);
            bundle.putString(Peripheral.SERVICE_UUID,this.serviceUUID);
            bundle.putString(Peripheral.CHARACTERISTIC_UUID,this.characteristicUUID);
            bundle.putByteArray(Peripheral.VALUES,this.values);
            intent.putExtra("data",bundle);
            context.sendBroadcast(intent);
        }
    };
    private void connect(final String peripheralUUID,final String serviceUUID,final String characteristicUUID){
        Log.i(TAG," service  = > connect : "+peripheralUUID+" , "+serviceUUID+" , "+characteristicUUID);
        binder.connect(this,peripheralUUID,new CallBackManager.PeripheralConnect(){
            @Override
            public void onConnect(BluetoothDevice device) {
            Log.i(TAG," service  = > PeripheralConnect : onConnect ");
                retrieveServices(peripheralUUID,serviceUUID,characteristicUUID);
            }
            @Override
            public void onDisconnect(BluetoothDevice device) {
            Log.i(TAG," service  = > PeripheralConnect : onDisconnect ");
                if (isBackgroud){
                    connect(peripheralUUID,serviceUUID,characteristicUUID);
                }
            }
            @Override
            public void onResult(String text) {
            Log.i(TAG," service  = > PeripheralConnect : onResult => "+text);

            }
        });
    }
    private void retrieveServices(final String peripheralUUID,final String serviceUUID,final String characteristicUUID){
        binder.retrieveServices(peripheralUUID,new CallBackManager.RetrieveServices() {
            @Override
            public void onSuccessed(WritableMap map) {
                Log.i(TAG," service  = > RetrieveServices : onSuccessed ");
                binder.startNotification(peripheralUUID,serviceUUID,characteristicUUID,notification);
            }

            @Override
            public void onFailed(String text) {
                Log.i(TAG," service  = > RetrieveServices : onFailed => "+text);
            }
        });
    }
    private void start(){
        JSONArray peripheralArray = peripheralJson.get();
        Log.i(TAG," service  = > start : peripheralArray => "+peripheralArray.toString());
        int length = peripheralArray.length();
        for (int i=0;i<length;i++){
            try {
                JSONObject peripheralJson = (JSONObject) peripheralArray.get(i);
                Log.i(TAG," service  = > start : peripheralJson => "+peripheralJson.toString());
                connect(peripheralJson.getString(Peripheral.PERIPHERAL_UUID),peripheralJson.getString(Peripheral.SERVICE_UUID),peripheralJson.getString(Peripheral.CHARACTERISTIC_UUID));
            }catch (JSONException e){
                e.printStackTrace();
                Log.i(TAG," service  = > start : printStackTrace ");
            }
        }
    }
}
