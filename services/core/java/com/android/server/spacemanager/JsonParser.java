package com.android.server.spacemanager;

import org.json.JSONException;
import com.android.server.spacemanager.simple.JSONParser;
import com.android.server.spacemanager.simple.JSONArray;
import com.android.server.spacemanager.simple.JSONObject;
import android.util.Log;

public class JsonParser {

	// parse String into JSONObject
	public JSONObject parseFromString(String jsonString) {
		JSONObject jsonObject;
    	JSONParser parser = new JSONParser();
    	try {
    		jsonObject = (JSONObject) parser.parse(jsonString);
    	} catch (Exception e) {
			Log.e(Constants.TAG, "Error: Cannot parse json. Error details: " + e + ". [JsonParser.parseFromString()]");
    		return null;
    	}
    	return jsonObject;
	}
	
	// get JSONArray
	public JSONArray parseToArray(JSONObject jsonObject, String key)  throws JSONException {
		return (JSONArray) jsonObject.get(key);
	}
	
	// get String
	public String parseToString(JSONObject jsonObject, String key)  throws JSONException {
		return (String) jsonObject.get(key);
	}
}
