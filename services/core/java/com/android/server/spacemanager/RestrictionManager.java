package com.android.server.spacemanager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class RestrictionManager {

    // package manager
    private PackageManager packageManager;

    // constructor
    public RestrictionManager(Context context) {
        this.packageManager = context.getPackageManager();
    }

    // apply specific restriction on a specific app
    public void applyRestriction(String permission, String appId) {
        this.packageManager.revokeRuntimePermission(appId, Constants.ANDROID_PERMISSION_PREFIX+permission, android.os.Process.myUserHandle());
    }

    // reset specific restriction on a specific app
    public void resetRestriction(String permission, String appId) {
        this.packageManager.grantRuntimePermission(appId, Constants.ANDROID_PERMISSION_PREFIX+permission, android.os.Process.myUserHandle());
    }

    // check permision status
    public boolean isPermissionGranted(String permission, String appId) {
        if (this.packageManager.checkPermission(Constants.ANDROID_PERMISSION_PREFIX+permission, appId) == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    // disable specifc app
    public void disableApplication(String appId) {
        if (isApplicationInstalled(appId))
            this.packageManager.setApplicationEnabledSetting(appId, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
        else
            Log.d(Constants.TAG, appId + " is not installed. [RestrictionManager.disableApplication()]");
    }

    // enable specifc app
    public void enableApplication(String appId) {
        if (isApplicationInstalled(appId))
            this.packageManager.setApplicationEnabledSetting(appId, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 99);
        else
            Log.d(Constants.TAG, appId + " is not installed. [RestrictionManager.disableApplication()]");
    }

    // disable all applications
    public void disableAllApplications() {
        List<ApplicationInfo> packages = this.packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            // if application is a user installed application and not a system built-in (part of os image) application
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                this.packageManager.setApplicationEnabledSetting(packageInfo.packageName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            }
        }
    }

    // enable all applications
    public void enableAllApplications() {
        List<ApplicationInfo> packages = this.packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            // if application is a user installed application and not a system built-in (part of os image) application
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                this.packageManager.setApplicationEnabledSetting(packageInfo.packageName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 99);
            }
        }
    }

    // check uf specifc application is installed
    public boolean isApplicationInstalled(String appId) {
        try {
            this.packageManager.getPackageInfo(appId, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // get all installed application package names
    public ArrayList<String> getAllInstalledApplications() {
        ArrayList<String> packageNames = new ArrayList<String>();        
        List<ApplicationInfo> packages = this.packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            // if application is a user installed application and not a system built-in (part of os image) application
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                packageNames.add(packageInfo.packageName);
            }
        }
        return packageNames;
    }
}
