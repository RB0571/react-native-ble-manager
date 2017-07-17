package it.innove;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

/**
 * Created by admin on 2017/7/12.
 */

public class BLEEvent {
    public final static String EVENT_BLEMANAGER_DID_UPDATE_STATE="BleManagerDidUpdateState";
    public final static String EVENT_BLEMANAGER_STOP_SCAN="BleManagerStopScan";
    public final static String EVENT_BLEMANAGER_DID_UPDATE_CHARACTOERISTIC="BleManagerDidUpdateValueForCharacteristic";
    public final static String EVENT_BLEMANAGER_DISCOVER_PERIPHERAL="BleManagerDiscoverPeripheral";
    public final static String EVENT_BLEMANAGER_CONNECT_PERIPHERAL="BleManagerConnectPeripheral";
    public final static String EVENT_BLEMANAGER_DISCONNECT_PERIPHERAL="BleManagerDisconnectPeripheral";
    public ReactApplicationContext reactContext;

    public BLEEvent(ReactApplicationContext context){
        reactContext = context;
    }
    public void sendEvent(String eventName, @Nullable WritableMap params) {
        reactContext.getJSModule(RCTNativeAppEventEmitter.class) .emit(eventName, params);
    }
}
