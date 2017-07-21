package it.innove;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static android.os.Build.VERSION_CODES.LOLLIPOP;

/**
 * Created by admin on 2017/7/11.
 */

public class BleBinder extends Binder {
    private static final String TAG = "BleBinder";
    private PeripheralJson peripheralJson;
    private BluetoothAdapter bluetoothAdapter;
    private ScanerManager scanerManager;
    private ReactApplicationContext reactContext;
    private Context context;
    // key is the MAC Address
    public Map<String, Peripheral> peripherals = new LinkedHashMap<>();


    public BleBinder(Context context){
        this.context = context;
        peripheralJson = new PeripheralJson(context);
        if(Build.VERSION.SDK_INT >= LOLLIPOP){
            scanerManager = new ScanerLollipop(getBluetoothAdapter(),scaner);
        }else{
            scanerManager = new ScanerLegacy(getBluetoothAdapter(),scaner);
        }
    }
    public BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = manager.getAdapter();
        }
        return bluetoothAdapter;
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
    private ScanerManager.IScaner scaner = new ScanerManager.IScaner() {
        @Override
        public void onResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String address = device.getAddress();
            Peripheral peripheral;
            if (!peripherals.containsKey(address)) {
                peripheral = new Peripheral(device, rssi, scanRecord);
                //peripherals.put(device.getAddress(), peripheral);
            } else {
                peripheral = peripherals.get(address);
                peripheral.updateRssi(rssi);
                peripheral.updateData(scanRecord);
            }
            scanerCallback.onFinded(peripheral);
        }
    };
    /**
     * 扫描蓝牙设备
     * @param serviceUUIDs
     * @param scanSeconds
     * @param options
     * @param scanerCallback
     */
    public void startScan(List<String> serviceUUIDs, final int scanSeconds, Map<String,Integer> options, final CallBackManager.Scaner scanerCallback){
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
        scanerManager.startScan(serviceUUIDs,options);
        if(scanSeconds>0){
            new Thread(new Runnable() {
                private int currentSessionID = scanSessionID.incrementAndGet();
                @Override
                public void run() {
                    if(currentSessionID == scanSessionID.intValue()){
                        try{
                            Thread.sleep(scanSeconds*1000);
                            scanerManager.stopScan();
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }


    /**
     * 停止扫描
     */
    public void stopScan(CallBackManager.Scaner scanerCallback){
        if (getBluetoothAdapter() == null) {
            scanerCallback.onResult("No bluetooth support");
            return;
        }
        if (!getBluetoothAdapter().isEnabled()) {
            scanerCallback.onResult("Bluetooth not enabled");
            return;
        }
        scanSessionID.incrementAndGet();
        scanerManager.stopScan();
        scanerCallback.onStop();
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

        JSONObject peripheralObject = new JSONObject();
        try{
            peripheralObject.put(Peripheral.PERIPHERAL_UUID,deviceUUID);
            peripheralObject.put(Peripheral.SERVICE_UUID,serviceUUID);
            peripheralObject.put(Peripheral.CHARACTERISTIC_UUID,characteristicUUID);
        }catch (JSONException e){
            e.printStackTrace();
        }
        peripheralJson.put(peripheralObject);
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
