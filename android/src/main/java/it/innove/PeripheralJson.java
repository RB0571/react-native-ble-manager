package it.innove;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by admin on 2017/7/21.
 */

public class PeripheralJson {
    private static final String TAG ="ReactNativeJS";
    private SharedPreferences userSettings;
    public PeripheralJson(Context context){
        userSettings= context.getSharedPreferences("setting", 0);
    }
    public void put(JSONObject object){
        Log.i(TAG,"PeripheralJson -> put : object = "+object.toString());
        JSONArray peripheralArray = get();
        int length = peripheralArray.length();
        for (int i=0;i<length;i++){
            try{
                JSONObject peripheral = (JSONObject) peripheralArray.get(i);
                if (object.getString(Peripheral.PERIPHERAL_UUID).equals(peripheral.getString(Peripheral.PERIPHERAL_UUID))){
                    return;
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        peripheralArray.put(object);
        save(peripheralArray.toString());
    }
    public void deleteByPeripheralUUID(String peripheralUUID){
        JSONArray results = new JSONArray();
        JSONArray peripheralArray = get();
        int length = peripheralArray.length();
        for (int i=0;i<length;i++){
            try{
                JSONObject peripheral = (JSONObject) peripheralArray.get(i);
                if (peripheralUUID.equals(peripheral.getString(Peripheral.PERIPHERAL_UUID))){
                    return;
                }
                results.put(peripheral);
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        save(results.toString());
    }
    public void clear(){
        remove();
    }
    public JSONObject getByPosition(int position) throws JSONException{
        if (position>=getLength()){
            return new JSONObject();
        }
        return ((JSONObject) get().get(position)) ;
    }
    public int getLength(){
        return get().length();
    }
    public JSONArray get(){
        String result = query();
        if (result==null){
            Log.i(TAG,"PeripheralJson -> get : object = null");
            return new JSONArray();
        }
        try {
            JSONArray peripheralArray = new JSONArray(result);
            Log.i(TAG,"PeripheralJson -> get : object = "+peripheralArray.toString());
            return peripheralArray;
        }catch (JSONException e){
            e.printStackTrace();
        }
        Log.i(TAG,"PeripheralJson -> get : object = null");
        return new JSONArray();
    }

    private void save(String values){
        SharedPreferences.Editor editor = userSettings.edit();
        editor.putString("peripheralArray",values);
        editor.commit();
    }
    private void remove(){
        SharedPreferences.Editor editor = userSettings.edit();
        editor.clear();
        editor.commit();
    }
    private String query(){
        return userSettings.getString("peripheralArray",null);
    }
}
