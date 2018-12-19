/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.view.contentcapture;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.app.TaskInfo;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.android.internal.util.Preconditions;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Context associated with a {@link ContentCaptureSession}.
 */
public final class ContentCaptureContext implements Parcelable {

    /*
     * IMPLEMENTATION NOTICE:
     *
     * This object contains both the info that's explicitly added by apps (hence it's public), but
     * it also contains info injected by the server (and are accessible through @SystemApi methods).
     */

    /**
     * Flag used to indicate that the app explicitly disabled content capture for the activity
     * (using
     * {@link android.view.contentcapture.ContentCaptureManager#setContentCaptureEnabled(boolean)}),
     * in which case the service will just receive activity-level events.
     *
     * @hide
     */
    @SystemApi
    public static final int FLAG_DISABLED_BY_APP = 0x1;

    /**
     * Flag used to indicate that the activity's window is tagged with
     * {@link android.view.Display#FLAG_SECURE}, in which case the service will just receive
     * activity-level events.
     *
     * @hide
     */
    @SystemApi
    public static final int FLAG_DISABLED_BY_FLAG_SECURE = 0x2;

    /** @hide */
    @IntDef(flag = true, prefix = { "FLAG_" }, value = {
            FLAG_DISABLED_BY_APP,
            FLAG_DISABLED_BY_FLAG_SECURE
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ContextCreationFlags{}

    /**
     * Flag indicating if this object has the app-provided context (which is set on
     * {@link ContentCaptureManager#createContentCaptureSession(ContentCaptureContext)}).
     */
    private final boolean mHasClientContext;

    // Fields below are set by app on Builder
    private final @Nullable Bundle mExtras;
    private final @Nullable Uri mUri;

    // Fields below are set by server when the session starts
    // TODO(b/111276913): create new object for taskId + componentName / reuse on other places
    private final @Nullable ComponentName mComponentName;
    private final int mTaskId;
    private final int mDisplayId;
    private final int mFlags;

    /** @hide */
    public ContentCaptureContext(@Nullable ContentCaptureContext clientContext,
            @NonNull ComponentName componentName, int taskId, int displayId, int flags) {
        if (clientContext != null) {
            mHasClientContext = true;
            mExtras = clientContext.mExtras;
            mUri = clientContext.mUri;
        } else {
            mHasClientContext = false;
            mExtras = null;
            mUri = null;
        }
        mComponentName = Preconditions.checkNotNull(componentName);
        mTaskId = taskId;
        mDisplayId = displayId;
        mFlags = flags;
    }

    private ContentCaptureContext(@NonNull Builder builder) {
        mHasClientContext = true;
        mExtras = builder.mExtras;
        mUri = builder.mUri;

        mComponentName  = null;
        mTaskId = mFlags = mDisplayId = 0;
    }

    /**
     * Gets the (optional) extras set by the app.
     *
     * <p>It can be used to provide vendor-specific data that can be modified and examined.
     *
     * @hide
     */
    @SystemApi
    @Nullable
    public Bundle getExtras() {
        return mExtras;
    }

    /**
     * Gets the (optional) URI set by the app.
     *
     * @hide
     */
    @SystemApi
    @Nullable
    public Uri getUri() {
        return mUri;
    }

    /**
     * Gets the id of the {@link TaskInfo task} associated with this context.
     *
     * @hide
     */
    @SystemApi
    public int getTaskId() {
        return mTaskId;
    }

    /**
     * Gets the activity associated with this context.
     *
     * @hide
     */
    @SystemApi
    public @NonNull ComponentName getActivityComponent() {
        return mComponentName;
    }

    /**
     * Gets the ID of the display associated with this context, as defined by
     * {G android.hardware.display.DisplayManager#getDisplay(int) DisplayManager.getDisplay()}.
     *
     * @hide
     */
    @SystemApi
    public int getDisplayId() {
        return mDisplayId;
    }

    /**
     * Gets the flags associated with this context.
     *
     * @return any combination of {@link #FLAG_DISABLED_BY_FLAG_SECURE} and
     * {@link #FLAG_DISABLED_BY_APP}.
     *
     * @hide
     */
    @SystemApi
     public @ContextCreationFlags int getFlags() {
        return mFlags;
    }

    /**
     * Builder for {@link ContentCaptureContext} objects.
     */
    public static final class Builder {
        private Bundle mExtras;
        private Uri mUri;

        /**
         * Sets extra options associated with this context.
         *
         * <p>It can be used to provide vendor-specific data that can be modified and examined.
         *
         * @param extras extra options.
         * @return this builder.
         */
        @NonNull
        public Builder setExtras(@NonNull Bundle extras) {
            // TODO(b/111276913): check build just once / throw exception / test / document
            mExtras = Preconditions.checkNotNull(extras);
            return this;
        }

        /**
         * Sets the {@link Uri} associated with this context.
         *
         * <p>See {@link View#setContentCaptureSession(ContentCaptureSession)} for an example.
         *
         * @param uri URI associated with this context.
         * @return this builder.
         */
        @NonNull
        public Builder setUri(@NonNull Uri uri) {
            // TODO(b/111276913): check build just once / throw exception / test / document
            mUri = Preconditions.checkNotNull(uri);
            return this;
        }

        /**
         * Builds the {@link ContentCaptureContext}.
         */
        public ContentCaptureContext build() {
            // TODO(b/111276913): check build just once / throw exception / test / document
            // TODO(b/111276913): make sure it at least one property (uri / extras) / test /
            // throw exception / documment
            return new ContentCaptureContext(this);
        }
    }

    /**
     * @hide
     */
    // TODO(b/111276913): dump to proto as well
    public void dump(PrintWriter pw) {
        pw.print("comp="); pw.print(ComponentName.flattenToShortString(mComponentName));
        pw.print(", taskId="); pw.print(mTaskId);
        pw.print(", displayId="); pw.print(mDisplayId);
        if (mFlags > 0) {
            pw.print(", flags="); pw.print(mFlags);
        }
        if (mExtras != null) {
            // NOTE: cannot dump because it could contain PII
            pw.print(", hasExtras");
        }
        if (mUri != null) {
            // NOTE: cannot dump because it could contain PII
            pw.print(", hasUri");
        }
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Context[act=")
                .append(ComponentName.flattenToShortString(mComponentName))
                .append(", taskId=").append(mTaskId)
                .append(", displayId=").append(mDisplayId)
                .append(", flags=").append(mFlags);
        if (mExtras != null) {
            // NOTE: cannot print because it could contain PII
            builder.append(", hasExtras");
        }
        if (mUri != null) {
            // NOTE: cannot print because it could contain PII
            builder.append(", hasUri");
        }
        return builder.append(']').toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mHasClientContext ? 1 : 0);
        if (mHasClientContext) {
            parcel.writeParcelable(mUri, flags);
            parcel.writeBundle(mExtras);
        }
        parcel.writeParcelable(mComponentName, flags);
        if (mComponentName != null) {
            parcel.writeInt(mTaskId);
            parcel.writeInt(mDisplayId);
            parcel.writeInt(mFlags);
        }
    }

    public static final Parcelable.Creator<ContentCaptureContext> CREATOR =
            new Parcelable.Creator<ContentCaptureContext>() {

        @Override
        public ContentCaptureContext createFromParcel(Parcel parcel) {
            final boolean hasClientContext = parcel.readInt() == 1;

            final ContentCaptureContext clientContext;
            if (hasClientContext) {
                final Builder builder = new Builder();
                final Uri uri = parcel.readParcelable(null);
                final Bundle extras = parcel.readBundle();
                if (uri != null) builder.setUri(uri);
                if (extras != null) builder.setExtras(extras);
                // Must reconstruct the client context using the Builder API
                clientContext = new ContentCaptureContext(builder);
            } else {
                clientContext = null;
            }
            final ComponentName componentName = parcel.readParcelable(null);
            if (componentName == null) {
                // Client-state only
                return clientContext;
            } else {
                final int taskId = parcel.readInt();
                final int displayId = parcel.readInt();
                final int flags = parcel.readInt();
                        return new ContentCaptureContext(clientContext, componentName, taskId,
                                displayId, flags);
                    }
        }

        @Override
        public ContentCaptureContext[] newArray(int size) {
            return new ContentCaptureContext[size];
        }
    };
}