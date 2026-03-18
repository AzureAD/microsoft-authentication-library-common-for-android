//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.request;

import static com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme.SerializedNames.NAME;
import static com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal.SCHEME_BEARER;
import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal.SCHEME_POP;
import static com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeWithClientKeyInternal.SCHEME_POP_WITH_CLIENT_KEY;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeWithClientKeyInternal;
import com.microsoft.identity.common.logging.Logger;

import java.lang.reflect.Type;

/**
 * Gson de/serialization utility class for auth schemes.
 */
public class AuthenticationSchemeTypeAdapter implements
        JsonDeserializer<AbstractAuthenticationScheme>,
        JsonSerializer<AbstractAuthenticationScheme> {

    private static final String TAG = AuthenticationSchemeTypeAdapter.class.getSimpleName();

    private static Gson sRequestAdapterGsonInstance = new GsonBuilder()
            .registerTypeAdapter(
                    AbstractAuthenticationScheme.class,
                    new AuthenticationSchemeTypeAdapter()
            ).create();

    public static Gson getGsonInstance(){
        return sRequestAdapterGsonInstance;
    }

    @Override
    public AbstractAuthenticationScheme deserialize(@NonNull final JsonElement json,
                                                    @NonNull final Type typeOfT,
                                                    @NonNull final JsonDeserializationContext context)
            throws JsonParseException {
        final String methodTag = TAG + ":deserialize";
        final JsonObject authScheme = json.getAsJsonObject();
        final JsonElement schemeName = authScheme.get(NAME);
        final String schemeNameStr = schemeName.getAsString();

        switch (schemeNameStr) {
            case SCHEME_BEARER:
                return context.deserialize(json, BearerAuthenticationSchemeInternal.class);

            case SCHEME_POP:
                return context.deserialize(json, PopAuthenticationSchemeInternal.class);

            case SCHEME_POP_WITH_CLIENT_KEY:
                return context.deserialize(json, PopAuthenticationSchemeWithClientKeyInternal.class);

            default:
                Logger.warn(
                        methodTag,
                        "Unrecognized auth scheme. Deserializing as null."
                );

                return null;
        }
    }

    @Override
    public JsonElement serialize(@NonNull final AbstractAuthenticationScheme src,
                                 @NonNull final Type typeOfSrc,
                                 @NonNull final JsonSerializationContext context) {
        final String methodTag = TAG + ":serialize";

        switch (src.getName()) {
            case SCHEME_BEARER:
                return context.serialize(src, BearerAuthenticationSchemeInternal.class);

            case SCHEME_POP:
                return context.serialize(src, PopAuthenticationSchemeInternal.class);

            case SCHEME_POP_WITH_CLIENT_KEY:
                return context.serialize(src, PopAuthenticationSchemeWithClientKeyInternal.class);

            default:
                Logger.warn(
                        methodTag,
                        "Unrecognized auth scheme. Serializing as null."
                );

                return null;
        }
    }
}
