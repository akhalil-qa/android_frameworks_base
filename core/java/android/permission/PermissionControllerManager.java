/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.permission;

import static android.permission.PermissionControllerService.SERVICE_INTERFACE;

import static com.android.internal.util.Preconditions.checkCollectionElementsNotNull;
import static com.android.internal.util.Preconditions.checkNotNull;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.TestApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.infra.AbstractMultiplePendingRequestsRemoteService;
import com.android.internal.infra.AbstractRemoteService;
import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Interface for communicating with the permission controller.
 *
 * @hide
 */
@TestApi
@SystemApi
@SystemService(Context.PERMISSION_CONTROLLER_SERVICE)
public final class PermissionControllerManager {
    private static final String TAG = PermissionControllerManager.class.getSimpleName();

    /**
     * The key for retrieving the result from the returned bundle.
     *
     * @hide
     */
    public static final String KEY_RESULT =
            "android.permission.PermissionControllerManager.key.result";

    /** @hide */
    @IntDef(prefix = { "REASON_" }, value = {
            REASON_MALWARE,
            REASON_INSTALLER_POLICY_VIOLATION,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Reason {}

    /** The permissions are revoked because the apps holding the permissions are malware */
    public static final int REASON_MALWARE = 1;

    /**
     * The permissions are revoked because the apps holding the permissions violate a policy of the
     * app that installed it.
     *
     * <p>If this reason is used only permissions of apps that are installed by the caller of the
     * API can be revoked.
     */
    public static final int REASON_INSTALLER_POLICY_VIOLATION = 2;

    /**
     * Callback for delivering the result of {@link #revokeRuntimePermissions}.
     */
    public abstract static class OnRevokeRuntimePermissionsCallback {
        /**
         * The result for {@link #revokeRuntimePermissions}.
         *
         * @param revoked The actually revoked permissions as
         *                {@code Map<packageName, List<permission>>}
         */
        public abstract void onRevokeRuntimePermissions(@NonNull Map<String, List<String>> revoked);
    }

    /**
     * Callback for delivering the result of {@link #getAppPermissions}.
     *
     * @hide
     */
    public interface OnGetAppPermissionResultCallback {
        /**
         * The result for {@link #getAppPermissions(String, OnGetAppPermissionResultCallback,
         * Handler)}.
         *
         * @param permissions The permissions list.
         */
        void onGetAppPermissions(@NonNull List<RuntimePermissionPresentationInfo> permissions);
    }

    /**
     * Callback for delivering the result of {@link #countPermissionApps}.
     *
     * @hide
     */
    public interface OnCountPermissionAppsResultCallback {
        /**
         * The result for {@link #countPermissionApps(List, boolean, boolean,
         * OnCountPermissionAppsResultCallback, Handler)}.
         *
         * @param numApps The number of apps that have one of the permissions
         */
        void onCountPermissionApps(int numApps);
    }

    private final @NonNull Context mContext;
    private final RemoteService mRemoteService;

    /** @hide */
    public PermissionControllerManager(@NonNull Context context) {
        Intent intent = new Intent(SERVICE_INTERFACE);
        intent.setPackage(context.getPackageManager().getPermissionControllerPackageName());
        ResolveInfo serviceInfo = context.getPackageManager().resolveService(intent, 0);

        mContext = context;
        mRemoteService = new RemoteService(context,
                serviceInfo.getComponentInfo().getComponentName());
    }

    /**
     * Revoke a set of runtime permissions for various apps.
     *
     * @param request The permissions to revoke as {@code Map<packageName, List<permission>>}
     * @param doDryRun Compute the permissions that would be revoked, but not actually revoke them
     * @param reason Why the permission should be revoked
     * @param executor Executor on which to invoke the callback
     * @param callback Callback to receive the result
     */
    @RequiresPermission(Manifest.permission.REVOKE_RUNTIME_PERMISSIONS)
    public void revokeRuntimePermissions(@NonNull Map<String, List<String>> request,
            boolean doDryRun, @Reason int reason, @NonNull @CallbackExecutor Executor executor,
            @NonNull OnRevokeRuntimePermissionsCallback callback) {
        // Check input to fail immediately instead of inside the async request
        checkNotNull(executor);
        checkNotNull(callback);
        checkNotNull(request);
        for (Map.Entry<String, List<String>> appRequest : request.entrySet()) {
            checkNotNull(appRequest.getKey());
            checkCollectionElementsNotNull(appRequest.getValue(), "permissions");
        }

        // Check required permission to fail immediately instead of inside the oneway binder call
        if (mContext.checkSelfPermission(Manifest.permission.REVOKE_RUNTIME_PERMISSIONS)
                != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException(Manifest.permission.REVOKE_RUNTIME_PERMISSIONS
                    + " required");
        }

        mRemoteService.scheduleRequest(new PendingRevokeRuntimePermissionRequest(mRemoteService,
                request, doDryRun, reason, mContext.getPackageName(), executor, callback));
    }

    /**
     * Gets the runtime permissions for an app.
     *
     * @param packageName The package for which to query.
     * @param callback Callback to receive the result.
     * @param handler Handler on which to invoke the callback.
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.GET_RUNTIME_PERMISSIONS)
    public void getAppPermissions(@NonNull String packageName,
            @NonNull OnGetAppPermissionResultCallback callback, @Nullable Handler handler) {
        checkNotNull(packageName);
        checkNotNull(callback);

        mRemoteService.scheduleRequest(new PendingGetAppPermissionRequest(mRemoteService,
                packageName, callback, handler == null ? mRemoteService.getHandler() : handler));
    }

    /**
     * Revoke the permission {@code permissionName} for app {@code packageName}
     *
     * @param packageName The package for which to revoke
     * @param permissionName The permission to revoke
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.REVOKE_RUNTIME_PERMISSIONS)
    public void revokeRuntimePermission(@NonNull String packageName,
            @NonNull String permissionName) {
        checkNotNull(packageName);
        checkNotNull(permissionName);

        mRemoteService.scheduleAsyncRequest(new PendingRevokeAppPermissionRequest(packageName,
                permissionName));
    }

    /**
     * Count how many apps have one of a set of permissions.
     *
     * @param permissionNames The permissions the app might have
     * @param countOnlyGranted Count an app only if the permission is granted to the app
     * @param countSystem Also count system apps
     * @param callback Callback to receive the result
     * @param handler Handler on which to invoke the callback
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.GET_RUNTIME_PERMISSIONS)
    public void countPermissionApps(@NonNull List<String> permissionNames,
            boolean countOnlyGranted, boolean countSystem,
            @NonNull OnCountPermissionAppsResultCallback callback, @Nullable Handler handler) {
        checkCollectionElementsNotNull(permissionNames, "permissionNames");
        checkNotNull(callback);

        mRemoteService.scheduleRequest(new PendingCountPermissionAppsRequest(mRemoteService,
                permissionNames, countOnlyGranted, countSystem, callback,
                handler == null ? mRemoteService.getHandler() : handler));
    }

    /**
     * A connection to the remote service
     */
    static final class RemoteService extends
            AbstractMultiplePendingRequestsRemoteService<RemoteService, IPermissionController> {
        private static final long UNBIND_TIMEOUT_MILLIS = 10000;
        private static final long MESSAGE_TIMEOUT_MILLIS = 30000;

        /**
         * Create a connection to the remote service
         *
         * @param context A context to use
         * @param componentName The component of the service to connect to
         */
        RemoteService(@NonNull Context context, @NonNull ComponentName componentName) {
            super(context, SERVICE_INTERFACE, componentName, UserHandle.myUserId(),
                    service -> Log.e(TAG, "RuntimePermPresenterService " + service + " died"),
                    false, false, 1);
        }

        /**
         * @return The default handler used by this service.
         */
        Handler getHandler() {
            return mHandler;
        }

        @Override
        protected @NonNull IPermissionController getServiceInterface(@NonNull IBinder binder) {
            return IPermissionController.Stub.asInterface(binder);
        }

        @Override
        protected long getTimeoutIdleBindMillis() {
            return UNBIND_TIMEOUT_MILLIS;
        }

        @Override
        protected long getRemoteRequestMillis() {
            return MESSAGE_TIMEOUT_MILLIS;
        }

        @Override
        public void scheduleRequest(@NonNull PendingRequest<RemoteService,
                IPermissionController> pendingRequest) {
            super.scheduleRequest(pendingRequest);
        }

        @Override
        public void scheduleAsyncRequest(@NonNull AsyncRequest<IPermissionController> request) {
            super.scheduleAsyncRequest(request);
        }
    }

    /**
     * Request for {@link #revokeRuntimePermissions}
     */
    private static final class PendingRevokeRuntimePermissionRequest extends
            AbstractRemoteService.PendingRequest<RemoteService, IPermissionController> {
        private final @NonNull Map<String, List<String>> mRequest;
        private final boolean mDoDryRun;
        private final int mReason;
        private final @NonNull String mCallingPackage;
        private final @NonNull OnRevokeRuntimePermissionsCallback mCallback;

        private final @NonNull RemoteCallback mRemoteCallback;

        private PendingRevokeRuntimePermissionRequest(@NonNull RemoteService service,
                @NonNull Map<String, List<String>> request, boolean doDryRun,
                @Reason int reason, @NonNull String callingPackage,
                @NonNull @CallbackExecutor Executor executor,
                @NonNull OnRevokeRuntimePermissionsCallback callback) {
            super(service);

            mRequest = request;
            mDoDryRun = doDryRun;
            mReason = reason;
            mCallingPackage = callingPackage;
            mCallback = callback;

            mRemoteCallback = new RemoteCallback(result -> executor.execute(() -> {
                long token = Binder.clearCallingIdentity();
                try {
                    Map<String, List<String>> revoked = new ArrayMap<>();
                    try {
                        Bundle bundleizedRevoked = result.getBundle(KEY_RESULT);

                        for (String packageName : bundleizedRevoked.keySet()) {
                            Preconditions.checkNotNull(packageName);

                            ArrayList<String> permissions =
                                    bundleizedRevoked.getStringArrayList(packageName);
                            Preconditions.checkCollectionElementsNotNull(permissions,
                                    "permissions");

                            revoked.put(packageName, permissions);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read result when revoking runtime permissions", e);
                    }

                    callback.onRevokeRuntimePermissions(revoked);
                } finally {
                    Binder.restoreCallingIdentity(token);

                    finish();
                }
            }), null);
        }

        @Override
        protected void onTimeout(RemoteService remoteService) {
            mCallback.onRevokeRuntimePermissions(Collections.emptyMap());
        }

        @Override
        public void run() {
            Bundle bundledizedRequest = new Bundle();
            for (Map.Entry<String, List<String>> appRequest : mRequest.entrySet()) {
                bundledizedRequest.putStringArrayList(appRequest.getKey(),
                        new ArrayList<>(appRequest.getValue()));
            }

            try {
                getService().getServiceInterface().revokeRuntimePermissions(bundledizedRequest,
                        mDoDryRun, mReason, mCallingPackage, mRemoteCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Error revoking runtime permission", e);
            }
        }
    }

    /**
     * Request for {@link #getAppPermissions}
     */
    private static final class PendingGetAppPermissionRequest extends
            AbstractRemoteService.PendingRequest<RemoteService, IPermissionController> {
        private final @NonNull String mPackageName;
        private final @NonNull OnGetAppPermissionResultCallback mCallback;

        private final @NonNull RemoteCallback mRemoteCallback;

        private PendingGetAppPermissionRequest(@NonNull RemoteService service,
                @NonNull String packageName, @NonNull OnGetAppPermissionResultCallback callback,
                @NonNull Handler handler) {
            super(service);

            mPackageName = packageName;
            mCallback = callback;

            mRemoteCallback = new RemoteCallback(result -> {
                final List<RuntimePermissionPresentationInfo> reportedPermissions;
                List<RuntimePermissionPresentationInfo> permissions = null;
                if (result != null) {
                    permissions = result.getParcelableArrayList(KEY_RESULT);
                }
                if (permissions == null) {
                    permissions = Collections.emptyList();
                }
                reportedPermissions = permissions;

                callback.onGetAppPermissions(reportedPermissions);

                finish();
            }, handler);
        }

        @Override
        protected void onTimeout(RemoteService remoteService) {
            mCallback.onGetAppPermissions(Collections.emptyList());
        }

        @Override
        public void run() {
            try {
                getService().getServiceInterface().getAppPermissions(mPackageName, mRemoteCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Error getting app permission", e);
            }
        }
    }

    /**
     * Request for {@link #revokeRuntimePermission}
     */
    private static final class PendingRevokeAppPermissionRequest
            implements AbstractRemoteService.AsyncRequest<IPermissionController> {
        private final @NonNull String mPackageName;
        private final @NonNull String mPermissionName;

        private PendingRevokeAppPermissionRequest(@NonNull String packageName,
                @NonNull String permissionName) {
            mPackageName = packageName;
            mPermissionName = permissionName;
        }

        @Override
        public void run(IPermissionController remoteInterface) {
            try {
                remoteInterface.revokeRuntimePermission(mPackageName, mPermissionName);
            } catch (RemoteException e) {
                Log.e(TAG, "Error revoking app permission", e);
            }
        }
    }

    /**
     * Request for {@link #countPermissionApps}
     */
    private static final class PendingCountPermissionAppsRequest extends
            AbstractRemoteService.PendingRequest<RemoteService, IPermissionController> {
        private final @NonNull List<String> mPermissionNames;
        private final @NonNull OnCountPermissionAppsResultCallback mCallback;
        private final boolean mCountOnlyGranted;
        private final boolean mCountSystem;

        private final @NonNull RemoteCallback mRemoteCallback;

        private PendingCountPermissionAppsRequest(@NonNull RemoteService service,
                @NonNull List<String> permissionNames, boolean countOnlyGranted,
                boolean countSystem, @NonNull OnCountPermissionAppsResultCallback callback,
                @NonNull Handler handler) {
            super(service);

            mPermissionNames = permissionNames;
            mCountOnlyGranted = countOnlyGranted;
            mCountSystem = countSystem;
            mCallback = callback;

            mRemoteCallback = new RemoteCallback(result -> {
                final int numApps;
                if (result != null) {
                    numApps = result.getInt(KEY_RESULT);
                } else {
                    numApps = 0;
                }

                callback.onCountPermissionApps(numApps);

                finish();
            }, handler);
        }

        @Override
        protected void onTimeout(RemoteService remoteService) {
            mCallback.onCountPermissionApps(0);
        }

        @Override
        public void run() {
            try {
                getService().getServiceInterface().countPermissionApps(mPermissionNames,
                        mCountOnlyGranted, mCountSystem, mRemoteCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Error counting permission apps", e);
            }
        }
    }
}