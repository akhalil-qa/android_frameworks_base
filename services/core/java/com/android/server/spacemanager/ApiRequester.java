package com.android.server.spacemanager;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import android.util.Log;

public class ApiRequester {

	// send a request to API and get the response
	public static String sendRequest(String urlString) {
		
    	String response = "";
    	try {
    		// send GET request to the space authority
    		URL url = new URL(urlString);
	        URLConnection connection = url.openConnection();
	        InputStream stream = connection.getInputStream();
	        
	        // read the response
	        int i;
	        while((i = stream.read()) != -1) {
				response += (char) i;
			}
    	} catch (Exception e) {
			Log.e(Constants.TAG, "Error: Cannot connect to url. Error details: " + e.getMessage() + "] [ApiRequester.sendRequest()]");
    		return null;
    	}
    	
    	return response;
	}
}
