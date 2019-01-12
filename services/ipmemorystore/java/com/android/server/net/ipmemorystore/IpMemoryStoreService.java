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

package com.android.server.net.ipmemorystore;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.IIpMemoryStore;
import android.net.ipmemorystore.Blob;
import android.net.ipmemorystore.IOnBlobRetrievedListener;
import android.net.ipmemorystore.IOnL2KeyResponseListener;
import android.net.ipmemorystore.IOnNetworkAttributesRetrieved;
import android.net.ipmemorystore.IOnSameNetworkResponseListener;
import android.net.ipmemorystore.IOnStatusListener;
import android.net.ipmemorystore.NetworkAttributesParcelable;

/**
 * Implementation for the IP memory store.
 * This component offers specialized services for network components to store and retrieve
 * knowledge about networks, and provides intelligence that groups level 2 networks together
 * into level 3 networks.
 *
 * @hide
 */
public class IpMemoryStoreService extends IIpMemoryStore.Stub {
    final Context mContext;

    public IpMemoryStoreService(@NonNull final Context context) {
        mContext = context;
    }

    /**
     * Store network attributes for a given L2 key.
     *
     * @param l2Key The L2 key for the L2 network. Clients that don't know or care about the L2
     *              key and only care about grouping can pass a unique ID here like the ones
     *              generated by {@code java.util.UUID.randomUUID()}, but keep in mind the low
     *              relevance of such a network will lead to it being evicted soon if it's not
     *              refreshed. Use findL2Key to try and find a similar L2Key to these attributes.
     * @param attributes The attributes for this network.
     * @param listener A listener to inform of the completion of this call, or null if the client
     *        is not interested in learning about success/failure.
     * Through the listener, returns the L2 key. This is useful if the L2 key was not specified.
     * If the call failed, the L2 key will be null.
     */
    @Override
    public void storeNetworkAttributes(@NonNull final String l2Key,
            @NonNull final NetworkAttributesParcelable attributes,
            @Nullable final IOnStatusListener listener) {
        // TODO : implement this
    }

    /**
     * Store a binary blob associated with an L2 key and a name.
     *
     * @param l2Key The L2 key for this network.
     * @param clientId The ID of the client.
     * @param name The name of this data.
     * @param data The data to store.
     * @param listener The listener that will be invoked to return the answer, or null if the
     *        is not interested in learning about success/failure.
     * Through the listener, returns a status to indicate success or failure.
     */
    @Override
    public void storeBlob(@NonNull final String l2Key, @NonNull final String clientId,
            @NonNull final String name, @NonNull final Blob data,
            @Nullable final IOnStatusListener listener) {
        // TODO : implement this
    }

    /**
     * Returns the best L2 key associated with the attributes.
     *
     * This will find a record that would be in the same group as the passed attributes. This is
     * useful to choose the key for storing a sample or private data when the L2 key is not known.
     * If multiple records are group-close to these attributes, the closest match is returned.
     * If multiple records have the same closeness, the one with the smaller (unicode codepoint
     * order) L2 key is returned.
     * If no record matches these attributes, null is returned.
     *
     * @param attributes The attributes of the network to find.
     * @param listener The listener that will be invoked to return the answer.
     * Through the listener, returns the L2 key if one matched, or null.
     */
    @Override
    public void findL2Key(@NonNull final NetworkAttributesParcelable attributes,
            @NonNull final IOnL2KeyResponseListener listener) {
        // TODO : implement this
    }

    /**
     * Returns whether, to the best of the store's ability to tell, the two specified L2 keys point
     * to the same L3 network. Group-closeness is used to determine this.
     *
     * @param l2Key1 The key for the first network.
     * @param l2Key2 The key for the second network.
     * @param listener The listener that will be invoked to return the answer.
     * Through the listener, a SameL3NetworkResponse containing the answer and confidence.
     */
    @Override
    public void isSameNetwork(@NonNull final String l2Key1, @NonNull final String l2Key2,
            @NonNull final IOnSameNetworkResponseListener listener) {
        // TODO : implement this
    }

    /**
     * Retrieve the network attributes for a key.
     * If no record is present for this key, this will return null attributes.
     *
     * @param l2Key The key of the network to query.
     * @param listener The listener that will be invoked to return the answer.
     * Through the listener, returns the network attributes and the L2 key associated with
     *         the query.
     */
    @Override
    public void retrieveNetworkAttributes(@NonNull final String l2Key,
            @NonNull final IOnNetworkAttributesRetrieved listener) {
        // TODO : implement this.
    }

    /**
     * Retrieve previously stored private data.
     * If no data was stored for this L2 key and name this will return null.
     *
     * @param l2Key The L2 key.
     * @param clientId The id of the client that stored this data.
     * @param name The name of the data.
     * @param listener The listener that will be invoked to return the answer.
     * Through the listener, returns the private data if any or null if none, with the L2 key
     *         and the name of the data associated with the query.
     */
    @Override
    public void retrieveBlob(@NonNull final String l2Key, @NonNull final String clientId,
            @NonNull final String name, @NonNull final IOnBlobRetrievedListener listener) {
        // TODO : implement this.
    }
}