package com.android.server.spacemanager;

public class Constants {

    // tag for log messages
    public static final String TAG = "TSM";

    // permission naming prefix
    public static final String ANDROID_PERMISSION_PREFIX = "android.permission.";

    // url base of the server
    public static final String BASE_URL = "https://hidden-garden-57594.herokuapp.com";

    // url to get database from the server
    public static final String GET_DATABASE_UPDATES_URL = BASE_URL + "/getDatabaseUpdates";

    // url to get dummy user location
    public static final String GET_DUMMY_USER_LOCATION_URL = BASE_URL + "/debug/getDummyUserLocation";

    // interval to get database updates (in seconds)
    public static final int DATABASE_UPDATE_INTERVAL = 20; //10;

    // interval to apply access control (in seconds)
    public static final int ACCESS_CONTROL_INTERVAL = 10; //5;

    // interval of requesting location update (in seconds)
    public static final int REQUEST_LOCATION_UPDATE_INTERVAL = 1;

    // interval of checking for dummy user location (in seconds)
    public static final int DUMMY_USER_LOCATION_CHECK_INTERVAL = 60; //10;

    // acceptable time threshold to consider the local database copy as a recent copy (in seconds)
    public static final int THRESHOLD_OF_LOCAL_DATABASE_RECENCY = 60; //20;

    // signature algorithm
    // available algorithms: list can be found here https://developer.android.com/reference/java/security/Signature
    public static final String SIGNATURE_ALGORITHM = "SHA512withRSA";

    // character "*" to be used in restriction record
    // when (permission = *): disable an application
    // when (appId = *): disable a permission on all applications
    public static final String ASTERISK = "*";
}