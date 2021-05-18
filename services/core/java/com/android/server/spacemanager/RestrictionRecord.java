package com.android.server.spacemanager;

public class RestrictionRecord {

    private String enforcer;
    private String permission;
    private String appId;

    // constructor
    public RestrictionRecord() {}

    // set enforcer
    public void setEnforcer(String enforcerId) {
        this.enforcer = enforcerId;
    }

    // get enforcer
    public String getEnforcer() {
        return this.enforcer;
    }

    // set permission
    public void setPermission(String permission) {
        this.permission = permission;
    }

    // get permission type
    public String getPermission() {
        return this.permission;
    }

    // set app id
    public void setAppId(String appId) {
        this.appId = appId;
    }

    // get app id
    public String getAppId() {
        return this.appId;
    }

    // toString
    public String toString() {
        return this.enforcer + "-" + this.permission + "-" + this.appId;
    }
}