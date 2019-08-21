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
package com.microsoft.identity.common.internal.providers.oauth2;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;
import com.microsoft.identity.common.adal.internal.net.IWebRequestHandler;
import com.microsoft.identity.common.adal.internal.net.WebRequestHandler;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import static com.microsoft.identity.common.exception.ServiceException.OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD;

/**
 * A class for pulling the OpenIDConfiguratino document from the OpenID Provider server.
 */
public class OpenIdProviderConfigurationClient {

    private static final String TAG = OpenIdProviderConfigurationClient.class.getSimpleName();
    private static final String sWellKnownConfig = "/.well-known/openid-configuration";

    private final String mIssuer;
    private final Gson mGson = new Gson();
    private final IWebRequestHandler mWebRequestHandler = new WebRequestHandler();

    public OpenIdProviderConfigurationClient(@NonNull final String issuer) {
        mIssuer = sanitize(issuer);
    }

    private String sanitize(@NonNull final String issuer) {
        String sanitizedIssuer = issuer.trim();

        if (issuer.endsWith("/")) { // Remove any trailing slash
            sanitizedIssuer = issuer.substring(0, sanitizedIssuer.length() - 1);
        }

        return sanitizedIssuer;
    }

    /**
     * Get OpenID provider configuration.
     *
     * @return OpenIDProviderConfiguration
     */
    public OpenIDProviderConfiguration getOpenIDProviderConfiguration() throws ServiceException {
        final String methodName = ":getOpenIDProviderConfiguration";

        try {
            final URL configUrl = new URL(mIssuer + sWellKnownConfig);

            Logger.verbose(
                    TAG + methodName,
                    "Config URL is valid."
            );

            Logger.verbosePII(
                    TAG + methodName,
                    "Using request URL: " + configUrl
            );

            final HttpWebResponse providerConfigResponse = mWebRequestHandler.sendGet(
                    configUrl,
                    new HashMap<String, String>()
            );

            final int statusCode = providerConfigResponse.getStatusCode();

            if (HttpURLConnection.HTTP_OK != statusCode) {
                throw new ServiceException(
                        OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD,
                        "OpenId Provider Configuration metadata failed to load with status: "
                                + statusCode,
                        null
                );
            }

            return parseMetadata(providerConfigResponse.getBody());
        } catch (IOException e) {
            throw new ServiceException(
                    OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD,
                    "IOException while requesting metadata",
                    e
            );
        }
    }

    private OpenIDProviderConfiguration parseMetadata(@Nullable final String body) {
        if (TextUtils.isEmpty(body)) {
            // Return an empty config
            return new OpenIDProviderConfiguration();
        }

        return mGson.fromJson(body, OpenIDProviderConfiguration.class);
    }

}
