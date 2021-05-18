package com.android.server.spacemanager;

public class UserGrantedPermission {

    private String permission;
    private String appId;

    // constructor
    public UserGrantedPermission(String permission, String appId) {
    	this.permission = permission;
    	this.appId = appId;
    }
    
    // get permission
    public String getPermission() {
        return this.permission;
    }

    // get app id
    public String getAppId() {
        return this.appId;
    }
}