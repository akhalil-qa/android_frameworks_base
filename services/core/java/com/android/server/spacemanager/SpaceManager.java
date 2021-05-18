package com.android.server.spacemanager;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.android.server.SystemService;
import android.annotation.NonNull;
import android.util.Slog;
import org.json.JSONException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.android.server.spacemanager.simple.JSONObject;
import com.android.server.spacemanager.simple.JSONArray;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.location.Location;
import android.os.Looper;
import android.location.LocationListener;
import java.lang.reflect.Method;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.android.internal.annotations.VisibleForTesting;
import android.os.ISpaceManager;
import android.os.IBinder;
import android.os.ServiceManager;

public final class SpaceManager extends SystemService {

    // context
    private final Context mContext;

    // location manager
    private LocationManager mLocationManager;

    // database of space authorities
    HashMap<String, SpaceAuthority> spaceAuthoritiesDatabase;

    // restriction manager
    private RestrictionManager restrictionManager;

    // restriction records list
    private ArrayList<RestrictionRecord> restrictionRecords;

    // granted permissions by user
    private ArrayList<UserGrantedPermission> userGrantedPermissions;

    // user location
    private Coordinate userLocation;

    // database update timestamp
    private long latestTimestamp;

    // database last update checking timestamp
    private long lasUpdateCheckTimestamp;

    // status of location listening
    private boolean isLocationUpdatesListeningStarted;

    // fail secure state flag
    private boolean failSecureEnabled;

    // dummy user location flag
    private boolean dummyUserLocationUsed;

    // initilize space manager service
    private final SpaceManagerService mSpaceManagerService;
    private static final String REMOTE_SERVICE_NAME = ISpaceManager.class.getName();

    // constructor
    public SpaceManager(@NonNull Context context) {
        super(context);
        this.mContext = context;
        this.mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        this.spaceAuthoritiesDatabase = null;
        this.restrictionManager = new RestrictionManager(context);
        this.userGrantedPermissions = new ArrayList<UserGrantedPermission>();
        this.userLocation = null;
        this.latestTimestamp = 0;
        this.lasUpdateCheckTimestamp = 0;
        this.isLocationUpdatesListeningStarted = false;
        this.failSecureEnabled = false;
        this.dummyUserLocationUsed = false;
        this.mSpaceManagerService = new SpaceManagerService();
    }

    // start the space manager activity
    @Override
    public void onStart() {
        Log.d(Constants.TAG, "Start SpaceManager [SpaceManager.onStart()]");
       
        try {
          ServiceManager.addService(REMOTE_SERVICE_NAME, this.mSpaceManagerService);
        } catch(Exception e) {
            e.printStackTrace();
        }
        // database update task
        TimerTask databaseUpdateTask = new TimerTask() {
            @Override
            public final void run() {

                String time = new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis()));
                Log.d(Constants.TAG, "Running database update task [" + time + "].");

                // if location service is not enabled, enable fail-secure behaviour
                if (!isLocationServiceEnabled()) {
                    failSecureEnabled = true;
                    performFailSecure();
                    return;
                }

                // if if internet connection is not available and the local database is not recent, enable fail-secure behaviour
                if (!isInternetConnectionAvailable() && !isLocalDatabaseCopyRecent()) {
                    failSecureEnabled = true;
                    performFailSecure();
                    return;
                }

                // clear fail secure flag
                failSecureEnabled = false;
                
                // enable all applications (in case they got disabled due to fail secure behaviour)
                Log.d(Constants.TAG, "Enabling all applications. [SpaceManager.run()]");
                restrictionManager.enableAllApplications();

                // get space authorities database updates from the server
                try {
                    getDatabaseUpdates();
                } catch (JSONException e) {
                    Log.e(Constants.TAG, "Error: Cannot get space authorities database. Error details: " + e + " [databaseUpdateTask.run]");
                    e.printStackTrace();
                }

                // start location updates listening
                if (!isLocationUpdatesListeningStarted) {
                    startListeningForLocationUpdates();
                    isLocationUpdatesListeningStarted = true;
                }
            }
        };

        // access control task
        TimerTask accessControlTask = new TimerTask(){
            @Override
            public final void run() {

                String time = new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis()));
                Log.d(Constants.TAG, "Running access control task [" + time + "].");

                // if fail-secure enabled
                if (failSecureEnabled) {
                    Log.d(Constants.TAG, "Fail secure enabeld.  Aborting access control task. [accessControlTask.run()]");
                    return;
                }
        
                // if local database is still not populated
                if (spaceAuthoritiesDatabase == null) {
                    Log.d(Constants.TAG, "Local database is not yet populated. [accessControlTask.run()]");
                    return;
                }

                // if user location is not yet retrieved 
                if (userLocation == null) {
                    Log.d(Constants.TAG, "User location is not yet retrieved. [accessControlTask.run()]");
                    return; 
                }

                Log.d(Constants.TAG, "Current user location is [" + userLocation.getLatitude() + ", " + userLocation.getLongitude() + ", " + userLocation.getAltitude() + "]. [accessControlTask.run()]");
                
                // get restriction records
                restrictionRecords = getRestrictions();

                Log.d(Constants.TAG, "Final Restrictions List:");
                for (RestrictionRecord restrictionRecord : restrictionRecords) {
                    Log.d(Constants.TAG, restrictionRecord.toString());
                }

                // reset restrictions (retrieve user configured permissions)
                resetRestrictions();

                // if user is in a controlled space (value is null if user in a non-controlled space)
                if (restrictionRecords != null) {
                    Log.d(Constants.TAG, "User is within a controlled space. [accessControlTask.run()]");
                    applyRestrictions(restrictionRecords);
                }
            }
        };

        // get dummy user location task
        // used for debug
        TimerTask dummyUserLocationTask = new TimerTask() {
            @Override
            public final void run() {

                // get dummy user location information from the server
                Coordinate dummyLocation = null;
                try {
                    dummyLocation = getDummyUserLocation();
                } catch (JSONException e) {
                    Log.e(Constants.TAG, "Error: Cannot get dummy user location.Error details: " + e + " [dummyUserLocationTask.run]");
                    e.printStackTrace();
                }

                if (dummyLocation == null) {
                    Log.d(Constants.TAG, "Dummy user location is not obtained from the server. [dummyUserLocationTask.run()]");
                    dummyUserLocationUsed = false;
                } else {
                    userLocation = dummyLocation;
                    dummyUserLocationUsed = true;
                    Log.d(Constants.TAG, "Dummy user location is obtained from the server: [" + userLocation.getLatitude() + "," + userLocation.getLongitude() + "," + userLocation.getAltitude() + "]. [dummyUserLocationTask.run()]");
                }
            }
        };
    
        // run tasks
        new Timer().scheduleAtFixedRate(databaseUpdateTask, 0, Constants.DATABASE_UPDATE_INTERVAL*1000);
        new Timer().scheduleAtFixedRate(accessControlTask, 0, Constants.ACCESS_CONTROL_INTERVAL*1000);
        new Timer().scheduleAtFixedRate(dummyUserLocationTask, 0, Constants.DUMMY_USER_LOCATION_CHECK_INTERVAL*1000); // used for debug
    }

    // check if location service is enabled
    private final boolean isLocationServiceEnabled() {
        boolean locationServiceStatus = mLocationManager.isLocationEnabled();
        Log.d(Constants.TAG, "Location service status: " + locationServiceStatus + ". [SpaceManager.isLocationServiceEnabled()");
        return locationServiceStatus;
    }

    // check if internet connection is available (wifi or cellular data)
    private final boolean isInternetConnectionAvailable() {

        // get wifi service status
        WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        boolean wifiStatus = wifi.isWifiEnabled();

        // get cellular data service status
        boolean cellularDataStatus = false;
        try {
            ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            cellularDataStatus = (boolean)method.invoke(cm);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Erorr: Error while checking cellular data service status. Error details: " + e + ". [SpaceManager.isRequiredServicesEnabled()");
        }

        Log.d(Constants.TAG, "Wifi service status: " + wifiStatus + ". [SpaceManager.isInternetConnectionAvailable()");
        Log.d(Constants.TAG, "Cellular data service status: " + cellularDataStatus + ". [SpaceManager.isInternetConnectionAvailable()");

        Log.d(Constants.TAG, "Internet connection status: " + (wifiStatus || cellularDataStatus) + ". [SpaceManager.isInternetConnectionAvailable()");
        return (wifiStatus || cellularDataStatus);
    }

    // check if the local database copy is recent
    private final boolean isLocalDatabaseCopyRecent() {

        // database not yet pulled from the server
        if (lasUpdateCheckTimestamp == 0) {
            Log.d(Constants.TAG, "Database is not yet pulled from the server. [SpaceManager.isLocalDatabaseCopyRecent()]");
            return false;
        }

        long timeNow = System.currentTimeMillis();

        int diffInSeconds = ((int)(timeNow - lasUpdateCheckTimestamp))/1000;
        
        Log.d(Constants.TAG, "Local database copy last update since [" + diffInSeconds + " seconds] ago. [SpaceManager.isLocalDatabaseCopyRecent()]");        
        if (diffInSeconds < Constants.THRESHOLD_OF_LOCAL_DATABASE_RECENCY) {
            Log.d(Constants.TAG, "Local database copy is recent. [SpaceManager.isLocalDatabaseCopyRecent()]");
            return true;
        } else {
            Log.d(Constants.TAG, "Local database copy is old. [SpaceManager.isLocalDatabaseCopyRecent()]");
            return false;
        }
    }

    // perform fail secure behaviour (enable: location, wifi, cirular data)
    private final void performFailSecure() {
        Log.d(Constants.TAG, "Perform fail secure behaviour. [SapceManager.performFailSecure()]");

        // disable all applications
        Log.d(Constants.TAG, "Disabling all applications. [SapceManager.performFailSecure()]");
        restrictionManager.disableAllApplications();

        // force enable location
        Log.d(Constants.TAG, "Force enabling location service. [SapceManager.performFailSecure()]");
        mLocationManager.setLocationEnabledForUser(true, android.os.Process.myUserHandle());

        // force enable wifi
        Log.d(Constants.TAG, "Force enabling wifi service. [SapceManager.performFailSecure()]");
        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        // force enable cellular data
        Log.d(Constants.TAG, "Force enabling cellular data service. [SapceManager.performFailSecure()]");
        TelephonyManager mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mTelephonyManager.setDataEnabled(true);
    }

    // get space authorities database updates from the server
    // updates spaceAuthoritiesDatabas, latestTimestamp, and lasUpdateCheckTimestamp
    private final void getDatabaseUpdates() throws JSONException {
        Log.d(Constants.TAG, "Pull database update from server [" + Constants.GET_DATABASE_UPDATES_URL + "/" + latestTimestamp + "]. [SpaceManager.getDatabaseUpdates()]");
        
        JsonParser jsonParser = new JsonParser();
        
        // send request to server and get back the json response
        String jsonResponse = ApiRequester.sendRequest(Constants.GET_DATABASE_UPDATES_URL+"/"+latestTimestamp);
        if (jsonResponse == null) {
            Log.e(Constants.TAG, "Error: Cannot receive response from the server when requesting for database update. [SpaceManager.getDatabaseUpdates()]");
            return;
        }

        // parse the response as json object
        JSONObject jsonObject = jsonParser.parseFromString(jsonResponse);
        if (jsonObject == null) {
            Log.e(Constants.TAG, "Error: Cannot read server response when requesting for database update. [SpaceManager.getDatabaseUpdates()]");
            return;
        }

        // get the signature of the response
        String signature = jsonParser.parseToString(jsonObject, "signature");
        
        // verify the signature
        // TODO
        //Crypto.verifySignature("message", signature, Constants.SIGNATURE_ALGORITHM, "publicKey");
        JSONArray jsonAuthorities = jsonParser.parseToArray(jsonObject, "authorities");

        // get authorities
        for (int i = 0; i < jsonAuthorities.size(); i++) {
            SpaceAuthority spaceAuthority = new SpaceAuthority();
            JSONObject jsonAuthority = (JSONObject)jsonAuthorities.get(i);

            String spaceAuthorityId = jsonParser.parseToString(jsonAuthority, "id");
            spaceAuthority.setId(spaceAuthorityId);

            long updateTimestamp = Long.parseLong(jsonParser.parseToString(jsonAuthority, "timestamp"));
            if (updateTimestamp > latestTimestamp)
                latestTimestamp = updateTimestamp;

            JSONArray jsonSpaceList = jsonParser.parseToArray(jsonAuthority, "spaceList");

            // get space list records
            for (int j = 0; j < jsonSpaceList.size(); j++) {
                SpaceListRecord spaceListRecord = new SpaceListRecord();
                JSONObject jsonSpaceListRecord = (JSONObject)jsonSpaceList.get(j);

                // get delegation records
                JSONArray jsonDelegationRecords = jsonParser.parseToArray(jsonSpaceListRecord, "delegations");
                for (int k = 0; k < jsonDelegationRecords.size(); k++) {
                    JSONObject jsonDelegationRecord = (JSONObject)jsonDelegationRecords.get(k);

                    Space space = new Space();
                    JSONObject jsonSpace = (JSONObject)jsonDelegationRecord.get("space");

                    String spaceId = jsonParser.parseToString(jsonSpace, "id");
                    
                    space.setId(spaceId);

                    // get boundary coordiantes
                    JSONArray jsonBoundary = jsonParser.parseToArray(jsonSpace, "boundary");
                    for (int m = 0; m < jsonBoundary.size(); m++) {
                        JSONObject jsonCoordinate = (JSONObject)jsonBoundary.get(m);

                        double latitude = Double.parseDouble(jsonParser.parseToString(jsonCoordinate, "latitude"));
                        double longitude = Double.parseDouble(jsonParser.parseToString(jsonCoordinate, "longitude"));
                        double altitude = Double.parseDouble(jsonParser.parseToString(jsonCoordinate, "altitude"));
                        space.addBoundaryCoordinate(latitude, longitude, altitude);
                    }
                    
                    String delegator = jsonParser.parseToString(jsonDelegationRecord, "delegator");
                    spaceListRecord.addDelegationRecord(space, delegator);
                }

                // get restriction records
                JSONArray jsonRestrictionRecords = jsonParser.parseToArray(jsonSpaceListRecord, "restrictions");
                for (int k = 0; k < jsonRestrictionRecords.size(); k++) {
                    JSONObject jsonRestrictionRecord = (JSONObject)jsonRestrictionRecords.get(k);

                    String permission = jsonParser.parseToString(jsonRestrictionRecord, "permission");
                    String appId = jsonParser.parseToString(jsonRestrictionRecord, "appId");
                    spaceListRecord.addRestrctionRecord(spaceAuthorityId, permission, appId);
                }

                Space space = new Space();
                JSONObject jsonSpace = (JSONObject)jsonSpaceListRecord.get("space");

                String spaceId = jsonParser.parseToString(jsonSpace, "id");
                space.setId(spaceId);

                // get boundary coordinates
                JSONArray jsonBoundary = jsonParser.parseToArray(jsonSpace, "boundary");
                for (int k = 0; k < jsonBoundary.size(); k++) {
                    JSONObject jsonCoordinate = (JSONObject)jsonBoundary.get(k);
                    
                    double latitude = Double.parseDouble(jsonParser.parseToString(jsonCoordinate, "latitude"));
                    double longitude = Double.parseDouble(jsonParser.parseToString(jsonCoordinate, "longitude"));
                    double altitude = Double.parseDouble(jsonParser.parseToString(jsonCoordinate, "altitude"));
                    space.addBoundaryCoordinate(latitude, longitude, altitude);
                }
                spaceListRecord.setSpace(space);

                spaceAuthority.addSpaceListRecord(spaceListRecord);
            }

            if (spaceAuthoritiesDatabase == null)
                spaceAuthoritiesDatabase = new HashMap<String, SpaceAuthority>();

            // add space authority to the database
            spaceAuthoritiesDatabase.put(spaceAuthority.getId(), spaceAuthority);
        }

        // updates the time of last update check
        lasUpdateCheckTimestamp = System.currentTimeMillis();
    }

    @Override
    public void onBootPhase(int phase) {}

    // location listener
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            // if dummy user location is being used
            if (dummyUserLocationUsed) {
                Log.d(Constants.TAG, "Dummy user location is being used. [LocationListener.onLocationChanged()]");
                return;
            }

            // initialize userLocation instance
            if (userLocation == null)
                userLocation = new Coordinate();

            // read new location
            userLocation.setLatitude(location.getLatitude());
            userLocation.setLongitude(location.getLongitude());
            userLocation.setAltitude(location.getAltitude());

            Log.d(Constants.TAG, "User location obtained: [" + userLocation.getLatitude() + "," + userLocation.getLongitude() + "," + userLocation.getAltitude() + "]. Accuracy [" + location.getAccuracy() + "]. Provider [" + location.getProvider() + "] [LocationListener.onLocationChanged()]");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    // start listening for location updates
    private void startListeningForLocationUpdates() {
        Log.d(Constants.TAG, "Start listening for location updates. [SpaceManager.startListeningForLocationUpdates()]");

        // set location request parameters
        // quality available options: ACCURACY_BLOCK, ACCURACY_CITY, ACCURACY_FINE, POWER_HIGH, POWER_LOW, POWER_NONE
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setQuality(LocationRequest.ACCURACY_FINE);
        locationRequest.setInterval(Constants.REQUEST_LOCATION_UPDATE_INTERVAL * 1000);
        locationRequest.setFastestInterval(Constants.REQUEST_LOCATION_UPDATE_INTERVAL * 1000);

        // request location updates
        mLocationManager.requestLocationUpdates(locationRequest, mLocationListener, Looper.getMainLooper());
    }

    // get restrictions that should be applied on the user based on userLocation value
    // return null if user is not in a controlled space
    private ArrayList<RestrictionRecord> getRestrictions() {
        // get space list record corresponds to the user location
        SpaceListRecord spaceListRecord = null;
        for (SpaceAuthority spaceAuthority : spaceAuthoritiesDatabase.values()) {
            spaceListRecord = spaceAuthority.getSpaceListRecord(userLocation);
            if (spaceListRecord != null) {
                break;
            }
        }
        if (spaceListRecord == null) {
            return null;
        }

        ArrayList<RestrictionRecord> restrictionRecords = new ArrayList<RestrictionRecord>();
        while (true) {
            // copy all restriction records
            restrictionRecords.addAll(spaceListRecord.getRestrictionRecords());

            // get delegator id
            String delegatorId = spaceListRecord.getDelegator(userLocation);
            if (delegatorId == null) {
                return restrictionRecords;
            }

            // get delegated space authority
            SpaceAuthority delegatedSpaceAuthority = spaceAuthoritiesDatabase.get(delegatorId);

            // get space list record corresponds to the user location
            spaceListRecord = delegatedSpaceAuthority.getSpaceListRecord(userLocation);
        }
    }

    // apply restrictions to the user permissions
    private void applyRestrictions(ArrayList<RestrictionRecord> restrictionRecords) {

        Log.d(Constants.TAG, "Applying all restrictions. [SpaceManager.applyRestrictions()]");

        // loop through all restriction records
        for (RestrictionRecord restrictionRecord : restrictionRecords) {
            String permission = restrictionRecord.getPermission();
            String appId = restrictionRecord.getAppId();

            // if restriction is to disable the application
            if (permission.equals(Constants.ASTERISK)) {
                Log.d(Constants.TAG, "Perform application disable: " + appId);
                restrictionManager.disableApplication(appId);
            }
            // if the restriction is to disable a specific permission on all applications (globally)
            else if (appId.equals(Constants.ASTERISK)) {
                ArrayList<String> applications = restrictionManager.getAllInstalledApplications();
                
                for (String packageName : applications) {
                    if (restrictionManager.isPermissionGranted(permission, packageName)) {
                        Log.d(Constants.TAG, "Permission: " + permission + " is found to be granted to " + packageName);

                        // store the permission in user granted permissions list
                        userGrantedPermissions.add(new UserGrantedPermission(permission, packageName));
                        Log.d(Constants.TAG, "Added [" + permission + ", " + packageName + "] to user granted permissions list.");
                    }

                    Log.d(Constants.TAG, "Perform disable " + permission + " in " + packageName);

                    // revoke permission
                    restrictionManager.applyRestriction(permission, packageName);
                }
            }
            // check if permission is already granted
            else {
                Log.d(Constants.TAG, "Check if permission: " + permission + " is granted to " + appId + " by the user.");
                if (restrictionManager.isPermissionGranted(permission, appId)) {
                    Log.d(Constants.TAG, "Permission: " + permission + " is found to be granted to " + appId);

                    // store the permission in user granted permissions list
                    userGrantedPermissions.add(new UserGrantedPermission(permission, appId));
                    Log.d(Constants.TAG, "Added [" + permission + ", " + appId + "] to user granted permissions list.");
                }

                Log.d(Constants.TAG, "Perform disable " + permission + " in " + appId);

                // revoke permission
                restrictionManager.applyRestriction(permission, appId);
            }
        }
    }
    
    // reset all applied restrictions
    private final void resetRestrictions() {
        
        Log.d(Constants.TAG, "Resetting all applied restrictions. [SpaceManager.resetRestrictions()]");

        // enable all applications
        Log.d(Constants.TAG, "Enabling all applications. [SpaceManager.resetRestrictions()]");
        restrictionManager.enableAllApplications();

        Log.d(Constants.TAG, "Content of user granted permissions list [" + userGrantedPermissions.size() + "]:");
        for (int i = 0; i < userGrantedPermissions.size(); i++) {
            String permission = userGrantedPermissions.get(i).getPermission();
            String appId = userGrantedPermissions.get(i).getAppId();
            Log.d(Constants.TAG, permission + " for " + appId);
        }

        // start resetting applied restrictions by granting back permissions given by user to applications
        for (int i = 0; i < userGrantedPermissions.size(); i++) {

            // get permission type and app id
            String permission = userGrantedPermissions.get(i).getPermission();
            String appId = userGrantedPermissions.get(i).getAppId();

            Log.d(Constants.TAG, "Reset " + permission + " for " + appId);

            // reset permission
            restrictionManager.resetRestriction(permission, appId);
        }
        
        // reset user granted permissions list
        Log.d(Constants.TAG, "Clearing user granted permissions list. [SpaceManager.resetRestrictions()]");
        userGrantedPermissions.clear();
    }

    // get dummy user location from server
    // used for debugging
    private final Coordinate getDummyUserLocation() throws JSONException {
        
        JsonParser jsonParser = new JsonParser();
        
        // send request to server and get back the json response
        String jsonResponse = ApiRequester.sendRequest(Constants.GET_DUMMY_USER_LOCATION_URL);
        if (jsonResponse == null) {
            return null;
        }

        JSONObject jsonObject = jsonParser.parseFromString(jsonResponse);
        String status = jsonParser.parseToString(jsonObject, "status");

        // if dummy user location is inactive (not currently used)
        if (status.equals("inactive")) {
            return null;
        }

        double latitude = Double.parseDouble(jsonParser.parseToString(jsonObject, "latitude"));
        double longitude = Double.parseDouble(jsonParser.parseToString(jsonObject, "longitude"));
        double altitude = Double.parseDouble(jsonParser.parseToString(jsonObject, "altitude"));

        return new Coordinate(latitude, longitude, altitude);
    }

// sub-class to pass data to other apps and services
final class SpaceManagerService extends ISpaceManager.Stub {

        // returns list of restriction records in a string format: enforcer-permission-appId
        // used to pass data to Settings app
        @Override
        public List<String> getRestrictionRecords() {
            List<String> list = new ArrayList<String>();
            for (RestrictionRecord restrictionRecord : restrictionRecords) {
                list.add(restrictionRecord.toString());
            }
            return list;
        }

        // checks if the supplied permission and app is is within the restriction records list
        // used to integrate with grantRuntimePermission() method to prevent granting permissions to already restricted permissions
        @Override
        public boolean isRestricted(String permission, String appId) {

            // remove the permission naming prefix
            permission = permission.replace(Constants.ANDROID_PERMISSION_PREFIX, "");

            // if restriction records list is not yet ready
            if (restrictionRecords == null) {
                Log.d(Constants.TAG, "restrictionRecords is not ready. [SpaceManagerService.isRestricted()]");
                return false;
            }
    
            for (RestrictionRecord restrictionRecord : restrictionRecords) {

                // check if permission is globally restricted in all applications
                if (restrictionRecord.getPermission().equals(permission) && restrictionRecord.getAppId().equals(Constants.ASTERISK)) {
                    Log.d(Constants.TAG, "[" + permission + ", " + appId + "] permission granting denied [reason: permission is globally restricted]. [SpaceManagerService.isRestricted()]");
                    return true;
                }
                // check if application is disabled
                else if (restrictionRecord.getPermission().equals(Constants.ASTERISK) && restrictionRecord.getAppId().equals(appId)) {
                    Log.d(Constants.TAG, "[" + permission + ", " + appId + "] application enabling denied [reason: application is disabled]. [SpaceManagerService.isRestricted()]");
                    return true;
                }
                // if supplied permission is restricted on the supplied application
                else if (restrictionRecord.getPermission().equals(permission) && restrictionRecord.getAppId().equals(appId)) {
                    Log.d(Constants.TAG, "[" + permission + ", " + appId + "] permission granting denied [reason: permission is restricted on this application]. [SpaceManagerService.isRestricted()]");
                    return true;
                }
            }

            return false;
        }
    }
}
