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
package com.microsoft.identity.common.internal.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.identity.common.internal.cache.CacheRecord;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;

import java.lang.reflect.Type;

public class ICacheRecordGsonAdapter implements JsonDeserializer<ICacheRecord> {

    @Override
    public ICacheRecord deserialize(final JsonElement json,
                                    final Type typeOfT,
                                    final JsonDeserializationContext context) throws JsonParseException {
        // TODO See if this can be simplified
        final JsonObject iCacheRecord = json.getAsJsonObject();

        final JsonElement accountRecordJsonElement = iCacheRecord.get(CacheRecord.GsonSerializedNames.PROPERTY_ACCOUNT);
        final JsonElement accessTokenJsonElement = iCacheRecord.get(CacheRecord.GsonSerializedNames.PROPERTY_ACCESS_TOKEN);
        final JsonElement refreshTokenJsonElement = iCacheRecord.get(CacheRecord.GsonSerializedNames.PROPERTY_REFRESH_TOKEN);
        final JsonElement idTokenJsonElement = iCacheRecord.get(CacheRecord.GsonSerializedNames.PROPERTY_ID_TOKEN);
        final JsonElement v1IdTokenJsonElement = iCacheRecord.get(CacheRecord.GsonSerializedNames.PROPERTY_V1_ID_TOKEN);

        // Assemble the native types
        final AccountRecord accountRecord = context.deserialize(accountRecordJsonElement, AccountRecord.class);
        final AccessTokenRecord accessTokenRecord = context.deserialize(accessTokenJsonElement, AccessTokenRecord.class);
        final RefreshTokenRecord refreshTokenRecord = context.deserialize(refreshTokenJsonElement, RefreshTokenRecord.class);
        final IdTokenRecord idTokenRecord = context.deserialize(idTokenJsonElement, IdTokenRecord.class);
        final IdTokenRecord v1IdTokenRecord = context.deserialize(v1IdTokenJsonElement, IdTokenRecord.class);

        // Set them on the result
        final CacheRecord cacheRecord = new CacheRecord();
        cacheRecord.setAccount(accountRecord);
        cacheRecord.setAccessToken(accessTokenRecord);
        cacheRecord.setRefreshToken(refreshTokenRecord);
        cacheRecord.setIdToken(idTokenRecord);
        cacheRecord.setV1IdToken(v1IdTokenRecord);

        return cacheRecord;
    }
}
