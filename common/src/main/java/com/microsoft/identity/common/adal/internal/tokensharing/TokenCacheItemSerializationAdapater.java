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
package com.microsoft.identity.common.adal.internal.tokensharing;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.OAuth2;
import com.microsoft.identity.common.internal.cache.ADALTokenCacheItem;

import java.lang.reflect.Type;

/**
 * This TokenCacheItemAdapter class is a customized serializer for the family
 * token cache item into GSON where we are trying to keep a lightweight form of
 * tokenCacheItem by parsing the raw idToken, we can get all the claims in it,
 * including userInfo, and tenantId.
 */
public final class TokenCacheItemSerializationAdapater
        implements JsonDeserializer<ADALTokenCacheItem>, JsonSerializer<ADALTokenCacheItem> {

    private static final String TAG = TokenCacheItemSerializationAdapater.class.getSimpleName();

    @Override
    public JsonElement serialize(final ADALTokenCacheItem tokenCacheItem,
                                 final Type type,
                                 final JsonSerializationContext context) {
        JsonObject jsonObj = new JsonObject();
        jsonObj.add(OAuth2.AUTHORITY, new JsonPrimitive(tokenCacheItem.getAuthority()));
        jsonObj.add(OAuth2.REFRESH_TOKEN, new JsonPrimitive(tokenCacheItem.getRefreshToken()));
        jsonObj.add(OAuth2.ID_TOKEN, new JsonPrimitive(tokenCacheItem.getRawIdToken()));
        jsonObj.add(OAuth2.ADAL_CLIENT_FAMILY_ID, new JsonPrimitive(tokenCacheItem.getFamilyClientId()));
        return jsonObj;
    }

    @Override
    public ADALTokenCacheItem deserialize(final JsonElement json,
                                          final Type type,
                                          final JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject srcJsonObj = json.getAsJsonObject();
        throwIfParameterMissing(srcJsonObj, OAuth2.AUTHORITY);
        throwIfParameterMissing(srcJsonObj, OAuth2.ID_TOKEN);
        throwIfParameterMissing(srcJsonObj, OAuth2.ADAL_CLIENT_FAMILY_ID);
        throwIfParameterMissing(srcJsonObj, OAuth2.REFRESH_TOKEN);

        final String rawIdToken = srcJsonObj.get(OAuth2.ID_TOKEN).getAsString();
        final ADALTokenCacheItem tokenCacheItem = new ADALTokenCacheItem();

        tokenCacheItem.setAuthority(srcJsonObj.get(OAuth2.AUTHORITY).getAsString());
        tokenCacheItem.setRawIdToken(rawIdToken);
        tokenCacheItem.setFamilyClientId(srcJsonObj.get(OAuth2.ADAL_CLIENT_FAMILY_ID).getAsString());
        tokenCacheItem.setRefreshToken(srcJsonObj.get(OAuth2.REFRESH_TOKEN).getAsString());
        return tokenCacheItem;
    }

    private void throwIfParameterMissing(JsonObject json, String name) {
        if (!json.has(name)) {
            throw new JsonParseException(TAG + "Attribute " + name + " is missing for deserialization.");
        }
    }
}