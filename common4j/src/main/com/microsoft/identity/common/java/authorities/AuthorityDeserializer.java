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
package com.microsoft.identity.common.java.authorities;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.CommonURIBuilder;

import net.jcip.annotations.Immutable;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import cz.msebera.android.httpclient.util.TextUtils;

@Immutable
public class AuthorityDeserializer implements JsonDeserializer<Authority> {

    private static final String TAG = AuthorityDeserializer.class.getSimpleName();

    @Override
    public Authority deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final String methodName = ":deserialize";
        JsonObject authorityObject = json.getAsJsonObject();
        JsonElement type = authorityObject.get("type");

        if (type != null) {
            switch (type.getAsString()) {
                case "AAD":
                    Logger.verbose(
                            TAG + methodName,
                            "Type: AAD"
                    );
                    final AzureActiveDirectoryAuthority aadAuthority = context.deserialize(authorityObject, AzureActiveDirectoryAuthority.class);

                    // The developer might supply authority_url instead of audience.
                    // In that case, we'll try our best to map the audience here.
                    if (aadAuthority != null && aadAuthority.mAuthorityUrlString != null) {
                        try {
                            final CommonURIBuilder uri = new CommonURIBuilder(URI.create(aadAuthority.mAuthorityUrlString));
                            final String cloudUrl = uri.getScheme() + "://" + uri.getHost();
                            final String tenant = uri.getLastPathSegment();
                            if (!TextUtils.isEmpty(tenant)) {
                                aadAuthority.mAudience = AzureActiveDirectoryAudience.getAzureActiveDirectoryAudience(cloudUrl, tenant);
                            }
                        } catch (final IllegalArgumentException e) {
                            // Do nothing
                            Logger.error(TAG + methodName, e.getMessage(), e);
                        }
                    }
                    return aadAuthority;
                case "B2C":
                    Logger.verbose(
                            TAG + methodName,
                            "Type: B2C"
                    );
                    return context.deserialize(authorityObject, AzureActiveDirectoryB2CAuthority.class);
                case "ADFS":
                    Logger.verbose(
                            TAG + methodName,
                            "Type: ADFS"
                    );
                    return context.deserialize(authorityObject, ActiveDirectoryFederationServicesAuthority.class);
                default:
                    Logger.verbose(
                            TAG + methodName,
                            "Type: Unknown"
                    );
                    return context.deserialize(authorityObject, UnknownAuthority.class);
            }
        }

        return null;
    }
}
