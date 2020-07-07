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
package com.microsoft.identity.common.internal.cache;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.reflect.Type;

import static com.microsoft.identity.common.internal.dto.AccessTokenRecord.SerializedNames.ACCESS_TOKEN_TYPE;

public class AccessTokenRecordJsonDeserializer implements JsonDeserializer<AccessTokenRecord> {

    private static final String TAG = AccessTokenRecordJsonDeserializer.class.getSimpleName();

    @Override
    public AccessTokenRecord deserialize(@NonNull final JsonElement json,
                                         @NonNull final Type typeOfT,
                                         @NonNull final JsonDeserializationContext context)
            throws JsonParseException {
        final AccessTokenRecord accessTokenRecord = deserializeDefault(json, typeOfT);
        final JsonObject jsonObject = json.getAsJsonObject();

        // Pick up any renamed properties...
        if (jsonObject.has(ACCESS_TOKEN_TYPE)) {
            Logger.info(
                    TAG,
                    "Deserializing legacy format AccessTokenRecord"
            );
            final String accessTokenType = jsonObject.get(ACCESS_TOKEN_TYPE).getAsString();
            accessTokenRecord.setAccessTokenType(accessTokenType);
        }

        return accessTokenRecord;
    }

    private static AccessTokenRecord deserializeDefault(@NonNull final JsonElement json,
                                                        @NonNull final Type typeOfT) {
        return new Gson().fromJson(json, typeOfT);
    }
}
