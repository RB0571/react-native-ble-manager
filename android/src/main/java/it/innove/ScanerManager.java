package it.innove;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;

import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2017/7/17.
 */

public abstract class ScanerManager {
    public interface IScaner {
        void onResult(BluetoothDevice device, int advertisingRSSI, byte[] scanRecord);
    }
    protected BluetoothAdapter bluetoothAdapter;
    protected IScaner callback;
    ScanerManager(BluetoothAdapter bluetoothAdapter,IScaner callback){
        this.bluetoothAdapter = bluetoothAdapter;
        this.callback = callback;
    }
    public abstract void startScan(List<String> filters, Map<String,Integer> options);
    public abstract void stopScan();
}
