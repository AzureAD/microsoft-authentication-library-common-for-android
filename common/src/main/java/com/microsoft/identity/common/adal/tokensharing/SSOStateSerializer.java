// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.identity.common.adal.tokensharing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.adal.internal.tokensharing.TokenCacheItemSerializationAdapater;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.adal.internal.cache.ADALTokenCacheItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SSOStateSerializer class is used to hide the details of the serialization and
 * deserialization details. It provides a serializer to serialize the
 * TokenCacheItem and a deserializer to return the TokenCacheItem.
 */
public final class SSOStateSerializer {
    /**
     * The version number of {@link SSOStateSerializer }.
     */
    @SerializedName("version")
    private final int version = 1;

    /**
     * The {@link SSOStateSerializer } stores the FRT tokenCacheItem of the
     * given user.
     */
    @SerializedName("tokenCacheItems")
    private final List<ADALTokenCacheItem> mTokenCacheItems = new ArrayList<>();

    /**
     * To customize the serialize/deserialize process and provide a more
     * lightweight TokenCacheItem, FamilyTokenCacheItemAdapter is used here to
     * register custom serializer.
     */
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ADALTokenCacheItem.class, new TokenCacheItemSerializationAdapater())
            .create();

    /**
     * No args constructor for use in serialization for Gson to prevent usage of sun.misc.Unsafe.
     */
    @SuppressWarnings("unused")
    private SSOStateSerializer() {
    }

    /**
     * constructor with an input item in type TokenCacheItem. We take
     * TokenCacheItem as input and call the constructor to initialize a
     * SSOStateSerializer object.
     *
     * @param item TokenCacheItem to initialize a SSOStateSerializer
     */
    private SSOStateSerializer(final ADALTokenCacheItem item) {
        if (item == null) {
            throw new IllegalArgumentException("tokenItem is null");
        }
        this.mTokenCacheItems.add(item);
    }

    private int getVersion() {
        return version;
    }

    /**
     * Return the token cache item in this blob container object.
     *
     * @return tokenCacheItem
     * @throws ClientException
     */
    @SuppressWarnings("PMD")
    private ADALTokenCacheItem getTokenItem() throws ClientException {
        if (mTokenCacheItems == null || mTokenCacheItems.isEmpty()) {
            throw new ClientException(ClientException.TOKEN_CACHE_ITEM_NOT_FOUND, "There is no token cache item in the SSOStateContainer.");
        }
        return mTokenCacheItems.get(0);
    }

    /**
     * serialize the tokenCacheItem with Adapter.
     *
     * @return String
     */
    private String internalSerialize() {
        return GSON.toJson(this);
    }

    /**
     * Deserialize the serializedBlob.
     * <p>
     * this function covers the details of the deserialization process
     *
     * @param serializedBlob String blob to convert to TokenCacheItem
     * @return TokenCacheItem
     * @throws ClientException
     */
    private ADALTokenCacheItem internalDeserialize(final String serializedBlob) throws ClientException {
        try {
            final JSONObject jsonObject = new JSONObject(serializedBlob);
            if (jsonObject.getInt("version") == this.getVersion()) {
                return GSON.fromJson(serializedBlob, SSOStateSerializer.class).getTokenItem();
            } else {
                throw new ClientException(ClientException.TOKEN_SHARING_DESERIALIZATION_ERROR,
                        "Fail to deserialize because the blob version is incompatible. The version of the serializedBlob is "
                                + jsonObject.getInt("version") + ". And the target class version is "
                                + this.getVersion());
            }
        } catch (final JsonParseException | JSONException exception) {
            throw new ClientException(ClientException.TOKEN_SHARING_DESERIALIZATION_ERROR, exception.getMessage());
        }
    }

    /**
     * In order to provide symmetry and hide the details in the
     * SSOStateSerializer on the serialization, we have this static serialize
     * function which takes the ADALTokenCacheItem object as input and return the
     * serialized json string if successful.
     *
     * @param item ADALTokenCacheItem to convert to serialized json
     * @return String
     */
    public static String serialize(final ADALTokenCacheItem item) {
        SSOStateSerializer ssoStateSerializer = new SSOStateSerializer(item);
        return ssoStateSerializer.internalSerialize();
    }

    /**
     * In order to provide symmetry and hide the details in the
     * SSOStateSerializer on the deserialization, we have this static
     * deserialize function take the serialized string as input and return the
     * deserialized ADALTokenCacheItem if successful.
     *
     * @param serializedBlob string blob to deserialize into ADALTokenCacheItem
     * @return ADALTokenCacheItem
     * @throws ClientException
     */
    public static ADALTokenCacheItem deserialize(final String serializedBlob) throws ClientException {
        SSOStateSerializer ssoStateSerializer = new SSOStateSerializer();
        return ssoStateSerializer.internalDeserialize(serializedBlob);
    }
}