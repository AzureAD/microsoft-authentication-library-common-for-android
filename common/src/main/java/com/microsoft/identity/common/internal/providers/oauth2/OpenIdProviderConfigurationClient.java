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

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.HttpResponse;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.microsoft.identity.common.exception.ServiceException.OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD;

/**
 * A class for pulling the OpenIdConfiguration document from the OpenID Provider server.
 */
public class OpenIdProviderConfigurationClient {

    private static final String TAG = OpenIdProviderConfigurationClient.class.getSimpleName();
    private static final String sWellKnownConfig = "/.well-known/openid-configuration";
    private static final ExecutorService sBackgroundExecutor = Executors.newCachedThreadPool();
    private static final Map<URL, OpenIdProviderConfiguration> sConfigCache = new HashMap<>();

    public interface OpenIdProviderConfigurationCallback
            extends TaskCompletedCallbackWithError<OpenIdProviderConfiguration, Exception> {
    }

    private final String mIssuer;
    private final Gson mGson = new Gson();

    public OpenIdProviderConfigurationClient(@NonNull final String issuer) {
        mIssuer = Uri.parse(sanitize(issuer)).toString();
    }

    public OpenIdProviderConfigurationClient(@NonNull final String authority,
                                             @NonNull final String path) {
        this(authority, path, "");
    }

    public OpenIdProviderConfigurationClient(@NonNull final String authority,
                                             @NonNull final String path, @NonNull final String endpointVersion) {
        mIssuer = new Uri.Builder()
                .scheme("https")
                .authority(authority)
                .appendPath(path)
                .appendPath(endpointVersion)
                .build().toString();
    }

    private String sanitize(@NonNull final String issuer) {
        String sanitizedIssuer = issuer.trim();

        if (issuer.endsWith("/")) { // Remove any trailing slash
            sanitizedIssuer = issuer.substring(0, sanitizedIssuer.length() - 1);
        }

        return sanitizedIssuer;
    }

    public void loadOpenIdProviderConfiguration(
            @NonNull final OpenIdProviderConfigurationCallback callback) {
        sBackgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onTaskCompleted(loadOpenIdProviderConfiguration());
                } catch (ServiceException e) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Get OpenID provider configuration.
     *
     * @return OpenIdProviderConfiguration
     */
    public synchronized OpenIdProviderConfiguration loadOpenIdProviderConfiguration()
            throws ServiceException {
        final String methodName = ":loadOpenIdProviderConfiguration";

        try {
            final URL configUrl = new URL(mIssuer + sWellKnownConfig);

            // Check first for a cached copy...
            final OpenIdProviderConfiguration cacheResult = sConfigCache.get(configUrl);

            // If we found a result, return it...
            if (null != cacheResult) {
                Logger.info(
                        TAG + methodName,
                        "Using cached metadata result."
                );
                return cacheResult;
            }

            Logger.verbose(
                    TAG + methodName,
                    "Config URL is valid."
            );

            Logger.verbosePII(
                    TAG + methodName,
                    "Using request URL: " + configUrl
            );

            final HttpResponse providerConfigResponse = HttpRequest.sendGet(configUrl, new HashMap<String, String>());

            final int statusCode = providerConfigResponse.getStatusCode();

            if (HttpURLConnection.HTTP_OK != statusCode
                    || TextUtils.isEmpty(providerConfigResponse.getBody())) {
                throw new ServiceException(
                        OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD,
                        "OpenId Provider Configuration metadata failed to load with status: "
                                + statusCode,
                        null
                );
            }

            final OpenIdProviderConfiguration parsedConfig = parseMetadata(
                    providerConfigResponse.getBody()
            );

            // Cache our config in memory for later
            cacheConfiguration(configUrl, parsedConfig);

            return parsedConfig;
        } catch (IOException e) {
            throw new ServiceException(
                    OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD,
                    "IOException while requesting metadata",
                    e
            );
        }
    }

    private void cacheConfiguration(@NonNull final URL configUrl,
                                    @NonNull final OpenIdProviderConfiguration parsedConfig) {
        sConfigCache.put(configUrl, parsedConfig);
    }

    private OpenIdProviderConfiguration parseMetadata(@NonNull final String body) {
        return mGson.fromJson(body, OpenIdProviderConfiguration.class);
    }

}
