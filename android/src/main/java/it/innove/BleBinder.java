package it.innove;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by admin on 2017/7/11.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BleBinder extends Binder {
    private static final String TAG = "BleBinder";
    private BluetoothAdapter bluetoothAdapter;
    private ScanManager scanManager;
    private ReactApplicationContext reactContext;
    private Context context;
    // key is the MAC Address
    public Map<String, Peripheral> peripherals = new LinkedHashMap<>();

    public void setReactContext(ReactApplicationContext context){
        reactContext = context;
        this.context = context;
    }
    public BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = manager.getAdapter();
        }
        return bluetoothAdapter;
    }

    /**
     * 初始化数据
     */
    public void init(){

    }

    /**
     * 检查蓝牙开关状态
     * @return
     */
    public String checkState(){
        String state = "off";
        switch (getBluetoothAdapter().getState()) {
            case BluetoothAdapter.STATE_ON:
                state = "on";
                break;
            case BluetoothAdapter.STATE_OFF:
                state = "off";
        }
        Log.d(TAG, "state:" + state);
        return state;
    }

    /**
     * 开启蓝牙
     */
    public void enable(){

    }

    private CallBackManager.Scaner scanerCallback;
    private AtomicInteger scanSessionID = new AtomicInteger();

    /**
     * 扫描蓝牙设备
     * @param serviceUUIDs
     * @param scanSeconds
     * @param options
     * @param scanerCallback
     */
    public void startScan(ReadableArray serviceUUIDs, final int scanSeconds, ReadableMap options, final CallBackManager.Scaner scanerCallback){
        this.scanerCallback = scanerCallback;
        if (getBluetoothAdapter() == null) {
            scanerCallback.onResult("No bluetooth support");
            return;
        }
        if (!getBluetoothAdapter().isEnabled()) {
            scanerCallback.onResult("bluetooth closed");
            return;
        }
        for (Iterator<Map.Entry<String, Peripheral>> iterator = peripherals.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Peripheral> entry = iterator.next();
            if (!entry.getValue().isConnected()) {
                iterator.remove();
            }
        }

        if(Build.VERSION.SDK_INT >= LOLLIPOP){
            scanLollipop(serviceUUIDs,scanSeconds,options);
        }else{
            scanLegacy();
        }
        if(scanSeconds>0){
            new Thread(new Runnable() {
                private int currentSessionID = scanSessionID.incrementAndGet();
                @Override
                public void run() {
                    if(currentSessionID == scanSessionID.intValue()){
                        try{
                            Thread.sleep(scanSeconds*1000);
                            stopScan(scanerCallback);
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    /**
     * android 5.0 前 扫描回调接口
     */
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            String address = device.getAddress();
            Peripheral peripheral;
            if (!peripherals.containsKey(address)) {
                peripheral = new Peripheral(device, rssi, scanRecord);
                peripherals.put(device.getAddress(), peripheral);
            } else {
                peripheral = peripherals.get(address);
                peripheral.updateRssi(rssi);
                peripheral.updateData(scanRecord);
            }
            scanerCallback.onFinded(peripheral);
        }
    };

    /**
     * android 5.0 前 扫描蓝牙设备
     */
    private void scanLegacy(){
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    /**
     * android 5.0 后 扫描回调接口
     */
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String address = result.getDevice().getAddress();
            Peripheral peripheral = null;

            if (!peripherals.containsKey(address)) {
                peripheral = new Peripheral(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                peripherals.put(address, peripheral);
            } else {
                peripheral = peripherals.get(address);
                peripheral.updateRssi(result.getRssi());
                peripheral.updateData(result.getScanRecord().getBytes());
            }
            scanerCallback.onFinded(peripheral);
        }
    };

    /**
     * android 5.0 后 扫描蓝牙设备
     * @param serviceUUIDs
     * @param scanSeconds
     * @param options
     */
    private void scanLollipop(ReadableArray serviceUUIDs, final int scanSeconds, ReadableMap options) {
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        List<ScanFilter> filters = new ArrayList<>();

        scanSettingsBuilder.setScanMode(options.getInt("scanMode"));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            scanSettingsBuilder.setNumOfMatches(options.getInt("numberOfMatches"));
            scanSettingsBuilder.setMatchMode(options.getInt("matchMode"));
        }

        if (serviceUUIDs.size() > 0) {
            for(int i = 0; i < serviceUUIDs.size(); i++){
                ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUIDHelper.uuidFromString(serviceUUIDs.getString(i)))).build();
                filters.add(filter);
            }
        }

        getBluetoothAdapter().getBluetoothLeScanner().startScan(filters, scanSettingsBuilder.build(), scanCallback);

    }
    /**
     * 停止扫描
     */
    public void stopScan(CallBackManager.Scaner scaner){
        if (getBluetoothAdapter() == null) {
            scaner.onResult("No bluetooth support");
            return;
        }
        if (!getBluetoothAdapter().isEnabled()) {
            scaner.onResult("Bluetooth not enabled");
            return;
        }
        scanSessionID.incrementAndGet();
        if (Build.VERSION.SDK_INT >= LOLLIPOP){
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }else{
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
        scaner.onStop();
    }

    /**
     * 连接蓝牙设备
     *
     * @param context
     * @param peripheralUUID
     * @param callback
     */
    public void connect(Context context,String peripheralUUID, CallBackManager.PeripheralConnect callback){
        Peripheral peripheral = peripherals.get(peripheralUUID);
        if (peripheral == null) {
            if (peripheralUUID != null) {
                peripheralUUID = peripheralUUID.toUpperCase();
            }
            if (BluetoothAdapter.checkBluetoothAddress(peripheralUUID)) {
                BluetoothDevice device = getBluetoothAdapter().getRemoteDevice(peripheralUUID);
                peripheral = new Peripheral(device);
                peripherals.put(peripheralUUID, peripheral);
            } else {
                callback.onResult("Invalid peripheral uuid");
                return;
            }
        }
        //peripheral.connect(callback, context);
        peripheral.connect(context,callback);
    }

    /**
     * 断开设备
     *
     * @param peripheralUUID
     * @param callback
     */
    public void disconnect(String peripheralUUID, CallBackManager.PeripheralConnect callback){
        Peripheral peripheral = peripherals.get(peripheralUUID);
        if (peripheral != null){
            peripheral.disconnect();
            callback.onResult(null);
        } else
            callback.onResult("Peripheral not found");
    }

    /**
     * 获取设备列表
     * @return
     */
    public Map<String, Peripheral> getPeripherals(){
        return peripherals;
    }

    /**
     * 删除设备列表中的数据
     *
     * @param deviceUUID
     * @return
     */
    public String removePeripheral(String deviceUUID){
        Peripheral peripheral = peripherals.get(deviceUUID);
        if (peripheral != null){
            if (peripheral.isConnected()) {
                return "Peripheral can not be removed while connected";
            } else {
                peripherals.remove(deviceUUID);
                return null;
            }
        }
        return "Peripheral not found";
    }

    /**
     * 检索 设备服务列表
     *
     * @param deviceUUID
     * @param retrieveServicesCallback
     */
    public void retrieveServices(String deviceUUID,CallBackManager.RetrieveServices retrieveServicesCallback){
        Peripheral peripheral = peripherals.get(deviceUUID);
        if (peripheral != null){
            peripheral.retrieveServices(retrieveServicesCallback);
        } else
            retrieveServicesCallback.onFailed("Peripheral not found");
    }

    /**
     * 监听特征
     *
     * @param deviceUUID
     * @param serviceUUID
     * @param characteristicUUID
     * @param notificationCallback
     */
    public void startNotification(String deviceUUID, String serviceUUID, String characteristicUUID, CallBackManager.PeripheralNotification notificationCallback){

        Peripheral peripheral = peripherals.get(deviceUUID);
        if (peripheral != null){
            peripheral.registerNotify(UUIDHelper.uuidFromString(serviceUUID), UUIDHelper.uuidFromString(characteristicUUID), notificationCallback);
        } else
            notificationCallback.onResult("Peripheral not found");
    }

    /**
     * 注销监听
     *
     * @param deviceUUID
     * @param serviceUUID
     * @param characteristicUUID
     * @param notificationCallback
     */
    public void stopNotification(String deviceUUID, String serviceUUID, String characteristicUUID, CallBackManager.PeripheralNotification notificationCallback){
        Peripheral peripheral = peripherals.get(deviceUUID);
        if (peripheral != null){
            peripheral.removeNotify(UUIDHelper.uuidFromString(serviceUUID), UUIDHelper.uuidFromString(characteristicUUID), notificationCallback);
        } else
            notificationCallback.onResult("Peripheral not found");
    }

    /**
     * 读取指定特征值
     *
     * @param deviceUUID
     * @param serviceUUID
     * @param characteristicUUID
     * @param readCallback
     */
    public void read(String deviceUUID, String serviceUUID, String characteristicUUID, CallBackManager.PeripheralRead readCallback){
        Peripheral peripheral = peripherals.get(deviceUUID);
        if (peripheral != null){
            peripheral.read(UUIDHelper.uuidFromString(serviceUUID), UUIDHelper.uuidFromString(characteristicUUID), readCallback);
        } else
            readCallback.onFailed("Peripheral not found");

    }

    /**
     * 读取RSSI值
     *
     * @param deviceUUID
     * @param rssiReadCallback
     */
    public void readRssi(String deviceUUID, CallBackManager.PeripheralRssiRead rssiReadCallback){
        Peripheral peripheral = peripherals.get(deviceUUID);
        if (peripheral != null){
            peripheral.readRSSI(rssiReadCallback);
        } else{
            rssiReadCallback.onFailed("Peripheral not found");
        }
    }

    /**
     *
     * 设置指定特征值
     *
     * @param deviceUUID
     * @param serviceUUID
     * @param characteristicUUID
     * @param message
     * @param maxByteSize
     * @param writeCallback
     */
    public void write(String deviceUUID, String serviceUUID, String characteristicUUID, ReadableArray message, Integer maxByteSize, CallBackManager.PeripheralWrite writeCallback){
        Peripheral peripheral = peripherals.get(deviceUUID);
        if (peripheral != null){
            byte[] decoded = new byte[message.size()];
            for (int i = 0; i < message.size(); i++) {
                decoded[i] = new Integer(message.getInt(i)).byteValue();
            }
            peripheral.write(UUIDHelper.uuidFromString(serviceUUID), UUIDHelper.uuidFromString(characteristicUUID), decoded, maxByteSize, null, writeCallback, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else
            writeCallback.onResult("Peripheral not found");
    }

    public void writeWithoutResponse(String deviceUUID, String serviceUUID, String characteristicUUID, ReadableArray message, Integer maxByteSize, Integer queueSleepTime,CallBackManager.PeripheralWrite writeCallback){
        Peripheral peripheral = peripherals.get(deviceUUID);
        if (peripheral != null){
            byte[] decoded = new byte[message.size()];
            for (int i = 0; i < message.size(); i++) {
                decoded[i] = new Integer(message.getInt(i)).byteValue();
            }
            peripheral.write(UUIDHelper.uuidFromString(serviceUUID), UUIDHelper.uuidFromString(characteristicUUID), decoded, maxByteSize, queueSleepTime, writeCallback, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        } else
            writeCallback.onResult("Peripheral not found");
    }
}
