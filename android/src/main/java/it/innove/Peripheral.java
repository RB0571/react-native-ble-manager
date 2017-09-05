package it.innove;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * Peripheral wraps the BluetoothDevice and provides methods to convert to JSON.
 */
public class Peripheral extends BluetoothGattCallback {
	public static final String PERIPHERAL_UUID = "peripheralUUID";
	public static final String SERVICE_UUID = "serviceUUID";
	public static final String CHARACTERISTIC_UUID = "characteristicUUID";
	public static final String VALUES = "values";
	private static final String CHARACTERISTIC_NOTIFICATION_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	public static final String LOG_TAG = "Peripheral";

	private BluetoothDevice device;
	private byte[] advertisingData;
	private int advertisingRSSI;
	private boolean connected = false;

	private BluetoothGatt gatt;

	private CallBackManager.RetrieveServices retrieveServicesCallback;
	private CallBackManager.PeripheralRead readCallback;
	private CallBackManager.PeripheralRssiRead readRSSICallback;
	private CallBackManager.PeripheralWrite writeCallback;
	private CallBackManager.PeripheralNotification notification;

	private List<byte[]> writeQueue = new ArrayList<>();

	public Peripheral(BluetoothDevice device, int advertisingRSSI, byte[] scanRecord) {
		this.device = device;
		this.advertisingRSSI = advertisingRSSI;
		this.advertisingData = scanRecord;
	}

	public Peripheral(BluetoothDevice device) {
		this.device = device;
	}

	/**
	 * 连接状态改变
	 *
	 * @param gatt
	 * @param status
	 * @param newState
	 */
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
		Log.d(LOG_TAG, "onConnectionStateChange ");
		this.gatt = gatt;
		if (newState == BluetoothGatt.STATE_CONNECTED) {
			connected = true;
			if (peripheralConnect != null) {
				Log.d(LOG_TAG, "Native Connected to: " + device.getAddress());
				//peripheralConnect.onResult(null);
				peripheralConnect.onConnect(device);
				//peripheralConnect = null;
			}
		} else if (newState == BluetoothGatt.STATE_DISCONNECTED){
			if (connected) {
				connected = false;
				if (gatt != null) {
					gatt.disconnect();
					gatt.close();
					this.gatt = null;
				}
			}
			if (peripheralConnect != null) {
				//peripheralConnect.onResult("Connection error");
				peripheralConnect.onDisconnect(device);
				peripheralConnect = null;
			}
		}

	}
	private Context context;
	private CallBackManager.PeripheralConnect peripheralConnect;
	public void connect(Context context,CallBackManager.PeripheralConnect callback) {
		this.context = context;
		if (!connected) {
			BluetoothDevice device = getDevice();
			this.peripheralConnect = callback;
			gatt = device.connectGatt(context, true, this);
		}else{
			if (gatt != null) {
				callback.onResult(null);
			} else
				callback.onResult("BluetoothGatt is null");
		}
	}

	public void disconnect() {
		connected = false;
		if (gatt != null) {
			try {
				gatt.disconnect();
				gatt.close();
				gatt = null;
				Log.d(LOG_TAG, "Disconnect");
				if(peripheralConnect!=null){
					peripheralConnect.onDisconnect(device);
				}
			} catch (Exception e) {
				Log.d(LOG_TAG, "Error on disconnect", e);
				if(peripheralConnect!=null){
					peripheralConnect.onDisconnect(device);
				}
			}
		}else
			Log.d(LOG_TAG, "GATT is null");

		peripheralConnect = null;
	}

	public WritableMap asWritableMap() {

		WritableMap map = Arguments.createMap();

		try {
			map.putString("name", device.getName());
			map.putString("id", device.getAddress()); // mac address
			map.putMap("advertising", byteArrayToWritableMap(advertisingData));
			map.putInt("rssi", advertisingRSSI);
		} catch (Exception e) { // this shouldn't happen
			e.printStackTrace();
		}

		return map;
	}

	public WritableMap asWritableMap(BluetoothGatt gatt) {

		WritableMap map = asWritableMap();
		WritableArray servicesArray = Arguments.createArray();
		WritableArray characteristicsArray = Arguments.createArray();

		if (connected && gatt != null) {
			for (Iterator<BluetoothGattService> it = gatt.getServices().iterator(); it.hasNext(); ) {
				BluetoothGattService service = it.next();
				WritableMap serviceMap = Arguments.createMap();
				String serviceUUID = UUIDHelper.uuidToString(service.getUuid());
				serviceMap.putString("uuid", serviceUUID);

				for (Iterator<BluetoothGattCharacteristic> itCharacteristic = service.getCharacteristics().iterator(); itCharacteristic.hasNext(); ) {
					//  解析Service数据结构，得到characteristic数据
					BluetoothGattCharacteristic characteristic = itCharacteristic.next();

					WritableMap characteristicsMap = Arguments.createMap();
					characteristicsMap.putString("service", serviceUUID);
					characteristicsMap.putString("characteristic", UUIDHelper.uuidToString(characteristic.getUuid()));
					characteristicsMap.putMap("properties", Helper.decodeProperties(characteristic));

					if (characteristic.getPermissions() > 0) {
						characteristicsMap.putMap("permissions", Helper.decodePermissions(characteristic));
					}


					WritableArray descriptorsArray = Arguments.createArray();

					for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
						// 解析 characteristic，得到descriptor数据
						WritableMap descriptorMap = Arguments.createMap();

						descriptorMap.putString("uuid", UUIDHelper.uuidToString(descriptor.getUuid()));
						descriptorMap.putString("value",descriptor.getValue() == null ? null : Base64.encodeToString(descriptor.getValue(), Base64.NO_WRAP));
						if (descriptor.getPermissions() > 0) {
							descriptorMap.putMap("permissions", Helper.decodePermissions(descriptor));
						}

						descriptorsArray.pushMap(descriptorMap);
					}
					if (descriptorsArray.size() > 0) {
						characteristicsMap.putArray("descriptors", descriptorsArray);
					}
					characteristicsArray.pushMap(characteristicsMap);
				}
				servicesArray.pushMap(serviceMap);
			}
			map.putArray("services", servicesArray);
			map.putArray("characteristics", characteristicsArray);
		}

		return map;
	}

	static JSONObject byteArrayToJSON(byte[] bytes) throws JSONException {
		JSONObject object = new JSONObject();
		object.put("CDVType", "ArrayBuffer");
		object.put("data", bytes != null ? Base64.encodeToString(bytes, Base64.NO_WRAP) : null);
		return object;
	}

	static WritableMap byteArrayToWritableMap(byte[] bytes) throws JSONException {
		WritableMap object = Arguments.createMap();
		object.putString("CDVType", "ArrayBuffer");
		object.putString("data", Base64.encodeToString(bytes, Base64.NO_WRAP));
		object.putArray("bytes", BleManager.bytesToWritableArray(bytes));
		return object;
	}

	public boolean isConnected() {
		return connected;
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public Boolean hasService(UUID uuid){
		if(gatt == null){
			return null;
		}
		return gatt.getService(uuid) != null;
	}

	/**
	 * 发现服务
	 *
	 * @param gatt
	 * @param status
	 */
	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		super.onServicesDiscovered(gatt, status);
		Log.i(LOG_TAG, "Peripheral onServicesDiscovered");
		if (retrieveServicesCallback != null) {
			WritableMap map = this.asWritableMap(gatt);
			retrieveServicesCallback.onSuccessed(map);
			retrieveServicesCallback = null;
		}
	}
	public void retrieveServices(CallBackManager.RetrieveServices callback) {
		if (gatt == null) {
			callback.onFailed("BluetoothGatt is null");
			return;
		}
		this.retrieveServicesCallback = callback;

		gatt.discoverServices();
	}




	/**
	 * 修改
	 *
	 * @param gatt
	 * @param descriptor
	 * @param status
	 */
	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
		Log.i(LOG_TAG, "Peripheral onDescriptorWrite");
		super.onDescriptorWrite(gatt, descriptor, status);
		if (notification != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				notification.onResult(null);
			} else {
				notification.onResult("Error writing descriptor stats=" + status);
			}

			//notification = null;
		}
	}

	public void read(UUID serviceUUID, UUID characteristicUUID, CallBackManager.PeripheralRead callback) {

		if (gatt == null) {
			callback.onFailed("BluetoothGatt is null");
			return;
		}

		BluetoothGattService service = gatt.getService(serviceUUID);
		BluetoothGattCharacteristic characteristic = findReadableCharacteristic(service, characteristicUUID);

		if (characteristic == null) {
			callback.onFailed("Characteristic " + characteristicUUID + " not found.");
		} else {
			readCallback = callback;
			if (!gatt.readCharacteristic(characteristic)) {
				readCallback = null;
				callback.onFailed("Read failed");
			}
		}
	}

	/**
	 *
	 *
	 * @param gatt
	 * @param characteristic
	 * @param status
	 */
	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicRead(gatt, characteristic, status);
		Log.d(LOG_TAG, "onCharacteristicRead ");

		if (readCallback != null) {

			if (status == BluetoothGatt.GATT_SUCCESS) {
				byte[] dataValue = characteristic.getValue();

				if (readCallback != null) {
					readCallback.onSuccessed(dataValue);
				}
			} else {
				readCallback.onFailed("Error reading " + characteristic.getUuid() + " status=" + status);
			}

			readCallback = null;

		}

	}

	@Override
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
		super.onReadRemoteRssi(gatt, rssi, status);
		Log.i(LOG_TAG, "Peripheral onReadRemoteRssi");
		if (readRSSICallback != null) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				updateRssi(rssi);
				readRSSICallback.onSuccessed(rssi);
			} else {
				readRSSICallback.onFailed("Error reading RSSI status=" + status);
			}

			readRSSICallback = null;
		}
	}

	public void readRSSI(CallBackManager.PeripheralRssiRead callback) {
		if (gatt == null) {
			callback.onFailed("BluetoothGatt is null");
			return;
		}

		readRSSICallback = callback;

		if (!gatt.readRemoteRssi()) {
			readCallback = null;
			callback.onFailed("Read RSSI failed");
		}
	}

	private void setNotify(UUID serviceUUID, UUID characteristicUUID, Boolean notify, CallBackManager.PeripheralNotification callback){
		Log.d(LOG_TAG, "setNotify");

		if (gatt == null) {
			callback.onResult("BluetoothGatt is null");
			return;
		}

		BluetoothGattService service = gatt.getService(serviceUUID);
		BluetoothGattCharacteristic characteristic = findNotifyCharacteristic(service, characteristicUUID);

		if (characteristic != null) {
			if (gatt.setCharacteristicNotification(characteristic, notify)) {

				BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUIDHelper.uuidFromString(CHARACTERISTIC_NOTIFICATION_CONFIG));
				if (descriptor != null) {
					// Prefer notify over indicate
					if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
						Log.d(LOG_TAG, "Characteristic " + characteristicUUID + " set NOTIFY");
						descriptor.setValue(notify ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
					} else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
						Log.d(LOG_TAG, "Characteristic " + characteristicUUID + " set INDICATE");
						descriptor.setValue(notify ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
					} else {
						Log.d(LOG_TAG, "Characteristic " + characteristicUUID + " does not have NOTIFY or INDICATE property set");
					}

					try {
						if (gatt.writeDescriptor(descriptor)) {
							Log.d(LOG_TAG, "setNotify complete");
							notification = callback;
						} else {
							callback.onResult("Failed to set client characteristic notification for " + characteristicUUID);
						}
					} catch (Exception e) {
						Log.d(LOG_TAG, "Error on setNotify", e);
						callback.onResult("Failed to set client characteristic notification for " + characteristicUUID + ", error: " + e.getMessage());
					}

				} else {
					callback.onResult("Set notification failed for " + characteristicUUID);
				}

			} else {
				callback.onResult("Failed to register notification for " + characteristicUUID);
			}

		} else {
			callback.onResult("Characteristic " + characteristicUUID + " not found");
		}

	}

	public void registerNotify(UUID serviceUUID, UUID characteristicUUID, CallBackManager.PeripheralNotification callback) {
		Log.d(LOG_TAG, "registerNotify");
		this.setNotify(serviceUUID, characteristicUUID, true, callback);
	}

	public void removeNotify(UUID serviceUUID, UUID characteristicUUID, CallBackManager.PeripheralNotification callback) {
		Log.d(LOG_TAG, "removeNotify");
		this.setNotify(serviceUUID, characteristicUUID, false, callback);
	}
	/**
	 * 监听特征值改变
	 *
	 * @param gatt
	 * @param characteristic
	 */
	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		super.onCharacteristicChanged(gatt, characteristic);
		Log.i(LOG_TAG, "Peripheral onCharacteristicChanged");

		byte[] dataValue = characteristic.getValue();
		//Log.d(LOG_TAG, "onCharacteristicChanged: " + dataValue + " from peripheral: " + device.getAddress());
		//sendEvent("BleManagerDidUpdateValueForCharacteristic", map);
		
		notification.peripheralUUID = device.getAddress().toString();
		notification.serviceUUID = characteristic.getService().getUuid().toString();
		notification.characteristicUUID = characteristic.getUuid().toString();
		notification.values = characteristic.getValue();
//
//		if(notification == null){
//			Log.d(LOG_TAG,"notification == null");
//			Intent intent = new Intent("com.roabay.luna.backgroud.action");
//			Bundle bundle = new Bundle();
//			bundle.putString("peripheralUUID",notification.peripheralUUID);
//			bundle.putString("serviceUUID",notification.serviceUUID);
//			bundle.putString("characteristicUUID",notification.characteristicUUID);
//			bundle.putByteArray("values",notification.values);
//			intent.putExtra("data",bundle);
//			context.sendBroadcast(intent);
//		}

		notification.onChanged();
	}
	// Some devices reuse UUIDs across characteristics, so we can't use service.getCharacteristic(characteristicUUID)
	// instead check the UUID and properties for each characteristic in the service until we find the best match
	// This function prefers Notify over Indicate
	@Nullable
	private BluetoothGattCharacteristic findNotifyCharacteristic(BluetoothGattService service, UUID characteristicUUID) {
		BluetoothGattCharacteristic characteristic = null;

		try {
			// Check for Notify first
			List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
			for (BluetoothGattCharacteristic c : characteristics) {
				if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 && characteristicUUID.equals(c.getUuid())) {
					characteristic = c;
					break;
				}
			}

			if (characteristic != null) return characteristic;

			// If there wasn't Notify Characteristic, check for Indicate
			for (BluetoothGattCharacteristic c : characteristics) {
				if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 && characteristicUUID.equals(c.getUuid())) {
					characteristic = c;
					break;
				}
			}

			// As a last resort, try and find ANY characteristic with this UUID, even if it doesn't have the correct properties
			if (characteristic == null) {
				characteristic = service.getCharacteristic(characteristicUUID);
			}

			return characteristic;
		}catch (Exception e) {
			Log.e(LOG_TAG, "Errore su caratteristica " + characteristicUUID ,e);
			return null;
		}
	}


	public void updateRssi(int rssi) {
		advertisingRSSI = rssi;
	}

	public void updateData(byte[] data) {
		advertisingData = data;
	}

	public int unsignedToBytes(byte b) {
		return b & 0xFF;
	}

	// Some peripherals re-use UUIDs for multiple characteristics so we need to check the properties
	// and UUID of all characteristics instead of using service.getCharacteristic(characteristicUUID)
	private BluetoothGattCharacteristic findReadableCharacteristic(BluetoothGattService service, UUID characteristicUUID) {
		BluetoothGattCharacteristic characteristic = null;

		int read = BluetoothGattCharacteristic.PROPERTY_READ;

		List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
		for (BluetoothGattCharacteristic c : characteristics) {
			if ((c.getProperties() & read) != 0 && characteristicUUID.equals(c.getUuid())) {
				characteristic = c;
				break;
			}
		}

		// As a last resort, try and find ANY characteristic with this UUID, even if it doesn't have the correct properties
		if (characteristic == null) {
			characteristic = service.getCharacteristic(characteristicUUID);
		}

		return characteristic;
	}



	public void doWrite(BluetoothGattCharacteristic characteristic, byte[] data) {
		characteristic.setValue(data);

		if (!gatt.writeCharacteristic(characteristic)) {
			Log.d(LOG_TAG, "Error on doWrite");
		}
	}
	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
		super.onCharacteristicWrite(gatt, characteristic, status);
		Log.i(LOG_TAG, "Peripheral onCharacteristicWrite");

		if (writeCallback != null) {

			if (writeQueue.size() > 0){
				byte[] data = writeQueue.get(0);
				writeQueue.remove(0);
				doWrite(characteristic, data);
			} else {

				if (status == BluetoothGatt.GATT_SUCCESS) {
					writeCallback.onResult(null);
				} else {
					Log.e(LOG_TAG, "Error onCharacteristicWrite:" + status);
					writeCallback.onResult("Error writing status: " + status);
				}

				writeCallback = null;
			}
		}else
			Log.e(LOG_TAG, "No callback on write");
	}
	public void write(UUID serviceUUID, UUID characteristicUUID, byte[] data, Integer maxByteSize, Integer queueSleepTime, CallBackManager.PeripheralWrite callback, int writeType) {
		if (gatt == null) {
			callback.onResult("BluetoothGatt is null");
		} else {
			BluetoothGattService service = gatt.getService(serviceUUID);
			BluetoothGattCharacteristic characteristic = findWritableCharacteristic(service, characteristicUUID, writeType);

			if (characteristic == null) {
				callback.onResult("Characteristic " + characteristicUUID + " not found.");
			} else {
				characteristic.setWriteType(writeType);

				if (writeQueue.size() > 0) {
					callback.onResult("You have already an queued message");
				}

				if ( writeCallback != null) {
					callback.onResult("You're already writing");
				}

				if (writeQueue.size() == 0 && writeCallback == null) {

					if (BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT == writeType) {
						writeCallback = callback;
					}

					if (data.length > maxByteSize) {
						int dataLength = data.length;
						int count = 0;
						byte[] firstMessage = null;
						List<byte[]> splittedMessage = new ArrayList<>();

						while (count < dataLength && (dataLength - count > maxByteSize)) {
							if (count == 0) {
								firstMessage = Arrays.copyOfRange(data, count, count + maxByteSize);
							} else {
								byte[] splitMessage = Arrays.copyOfRange(data, count, count + maxByteSize);
								splittedMessage.add(splitMessage);
							}
							count += maxByteSize;
						}
						if (count < dataLength) {
							// Other bytes in queue
							byte[] splitMessage = Arrays.copyOfRange(data, count, data.length);
							splittedMessage.add(splitMessage);
						}

						if (BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT == writeType) {
							writeQueue.addAll(splittedMessage);
							doWrite(characteristic, firstMessage);
						} else {
							try {
								doWrite(characteristic, firstMessage);
								Thread.sleep(queueSleepTime);
								for(byte[] message : splittedMessage) {
									doWrite(characteristic, message);
									Thread.sleep(queueSleepTime);
								}
								callback.onResult(null);
							} catch (InterruptedException e) {
								callback.onResult("Error during writing");
							}
						}
					} else {
						characteristic.setValue(data);

						if (gatt.writeCharacteristic(characteristic)) {
							Log.d(LOG_TAG, "Write completed");
							if (BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE == writeType) {
								callback.onResult(null);
							}
						} else {
							callback.onResult("Write failed");
							writeCallback = null;
						}
					}
				}
			}
		}

	}

	// Some peripherals re-use UUIDs for multiple characteristics so we need to check the properties
	// and UUID of all characteristics instead of using service.getCharacteristic(characteristicUUID)
	private BluetoothGattCharacteristic findWritableCharacteristic(BluetoothGattService service, UUID characteristicUUID, int writeType) {
		try {
			BluetoothGattCharacteristic characteristic = null;

			// get write property
			int writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE;
			if (writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
				writeProperty = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
			}

			List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
			for (BluetoothGattCharacteristic c : characteristics) {
				if ((c.getProperties() & writeProperty) != 0 && characteristicUUID.equals(c.getUuid())) {
					characteristic = c;
					break;
				}
			}

			// As a last resort, try and find ANY characteristic with this UUID, even if it doesn't have the correct properties
			if (characteristic == null) {
				characteristic = service.getCharacteristic(characteristicUUID);
			}

			return characteristic;
		}catch (Exception e) {
			Log.e(LOG_TAG, "Error on findWritableCharacteristic", e);
			return null;
		}
	}

	private String generateHashKey(BluetoothGattCharacteristic characteristic) {
		return generateHashKey(characteristic.getService().getUuid(), characteristic);
	}

	private String generateHashKey(UUID serviceUUID, BluetoothGattCharacteristic characteristic) {
		return String.valueOf(serviceUUID) + "|" + characteristic.getUuid() + "|" + characteristic.getInstanceId();
	}

}
