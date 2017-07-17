package it.innove;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2017/7/17.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScanerLollipop extends ScanerManager{
    public ScanerLollipop(BluetoothAdapter adapter,IScaner callback){
        super(adapter,callback);
    }
    /**
     * android 5.0 后 扫描回调接口
     */
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            callback.onResult(result.getDevice(),result.getRssi(),result.getScanRecord().getBytes());
        }
    };
    @Override
    public void startScan(List<String> serviceUUIDs, Map<String,Integer> options) {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        List<ScanFilter> filters = new ArrayList<>();

        scanSettingsBuilder.setScanMode(options.get("scanMode"));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            scanSettingsBuilder.setNumOfMatches(options.get("numberOfMatches"));
            scanSettingsBuilder.setMatchMode(options.get("matchMode"));
        }

        if (serviceUUIDs.size() > 0) {
            for(int i = 0; i < serviceUUIDs.size(); i++){
                ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUIDHelper.uuidFromString(serviceUUIDs.get(i)))).build();
                filters.add(filter);
            }
        }
        bluetoothAdapter.getBluetoothLeScanner().startScan(filters, scanSettingsBuilder.build() , scanCallback);
    }

    @Override
    public void stopScan() {
        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
    }
}
