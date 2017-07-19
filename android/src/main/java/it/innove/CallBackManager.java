package it.innove;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

/**
 * Created by admin on 2017/7/12.
 */

public class CallBackManager {
    interface Scaner{
        void onFinded(Peripheral peripheral);
        void onStop();
        void onResult(String text);
    }

    interface PeripheralConnect {
        void onConnect(BluetoothDevice device);
        void onDisconnect(BluetoothDevice device);
        void onResult(String text);
    }

    interface RetrieveServices{
        void onSuccessed(WritableMap map);
        void onFailed(String text);
    }

    interface PeripheralRssiRead{
        void onSuccessed(int rssi);
        void onFailed(String text);
    }

    interface PeripheralRead{
        void onSuccessed(byte[] values);
        void onFailed(String text);
    }
    interface PeripheralWrite {
        void onResult(String text);
    }
    interface PeripheralNotification{
        void onResult(String text);
        void onChanged(WritableMap map);
    }

}
