package android.os;

import android.Manifest.permission;
import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.TestApi;
import android.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.service.dreams.Sandman;
import android.util.ArrayMap;
import android.util.Log;
import android.util.proto.ProtoOutputStream;

import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import java.util.List;

import android.os.ISpaceManager;


public final class SpaceManager {

    final ISpaceManager mService;
    private static final String REMOTE_SERVICE_NAME = ISpaceManager.class.getName(); 

    private SpaceManager() {
        this.mService = ISpaceManager.Stub.asInterface(ServiceManager.getService(REMOTE_SERVICE_NAME));
        if (this.mService == null) {
          throw new IllegalStateException("Failed to find ISpaceManager by name [" + REMOTE_SERVICE_NAME + "]");
        }
    }

    public static SpaceManager getInstance() {
        return new SpaceManager();
    }

    /**
     * {@hide}
     */
    public List<String> getRestrictionRecords() {
        try {
            return mService.getRestrictionRecords();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * {@hide}
     */
    public boolean isRestricted(String permission, String appId) {
        try {
            return mService.isRestricted(permission, appId);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
