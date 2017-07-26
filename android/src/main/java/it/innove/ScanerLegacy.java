package it.innove;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2017/7/17.
 */

public class ScanerLegacy extends ScanerManager{
    public ScanerLegacy(BluetoothAdapter bluetoothAdapter, IScaner scaner){
        super(bluetoothAdapter,scaner);
    }
    /**
     * android 5.0 前 扫描回调接口
     */
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.i(TAG,"ScanerLegacy LeScanCallback");
            callback.onResult(device,rssi,scanRecord);
        }
    };

    @Override
    public void startScan(List<String> filters, Map<String,Integer> options) {
        Log.i(TAG,"ScanerLegacy startScan");
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    @Override
    public void stopScan() {
        bluetoothAdapter.stopLeScan(leScanCallback);
    }
}
