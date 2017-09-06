package it.innove;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Parcel;
import android.os.Parcelable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

/**
 * Created by admin on 2017/7/12.
 */

public class CallBackManager {
    public interface Scaner{
        void onFinded(Peripheral peripheral);
        void onStop();
        void onResult(String text);
    }

    public interface PeripheralConnect {
        void onConnect(BluetoothDevice device);
        void onDisconnect(BluetoothDevice device);
        void onResult(String text);
    }

    public interface RetrieveServices{
        void onSuccessed(WritableMap map);
        void onFailed(String text);
    }

    public interface PeripheralRssiRead{
        void onSuccessed(int rssi);
        void onFailed(String text);
    }

    public interface PeripheralRead{
        void onSuccessed(byte[] values);
        void onFailed(String text);
    }
    public interface PeripheralWrite {
        void onSuccessed(byte[] result);
        void onFailed(String text);
    }
    public abstract static class PeripheralNotification implements Parcelable{
        public String peripheralUUID;
        public String serviceUUID;
        public String characteristicUUID;
        public byte[] values;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(peripheralUUID);
            dest.writeString(serviceUUID);
            dest.writeString(characteristicUUID);
            dest.writeByteArray(values);
        }
        abstract void onResult(String text);
        abstract void onChanged();
    }

}
