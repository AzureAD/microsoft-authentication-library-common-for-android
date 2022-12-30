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
package com.microsoft.identity.common.java.providers.oauth2;

import com.google.gson.Gson;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.CommonURIBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.microsoft.identity.common.java.exception.ServiceException.OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * A class for pulling the OpenIdConfiguration document from the OpenID Provider server.
 */
public class OpenIdProviderConfigurationClient {

    private static final String TAG = OpenIdProviderConfigurationClient.class.getSimpleName();
    private static final String sWellKnownConfig = "/.well-known/openid-configuration";
    private static final ExecutorService sBackgroundExecutor = Executors.newCachedThreadPool();
    private static final Map<URI, OpenIdProviderConfiguration> sConfigCache = new HashMap<>();
    private static final HttpClient httpClient = UrlConnectionHttpClient.getDefaultInstance();

    public interface OpenIdProviderConfigurationCallback
            extends TaskCompletedCallbackWithError<OpenIdProviderConfiguration, Exception> {
    }

    private final String mIssuer;
    private final Gson mGson = new Gson();

    public OpenIdProviderConfigurationClient(@NonNull final String issuer) throws URISyntaxException {
        mIssuer = new URI(sanitize(issuer)).toString();
    }

    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    public OpenIdProviderConfigurationClient(@NonNull final String authority,
                                             @NonNull final String path) throws URISyntaxException {
        this(authority, path, "");
    }

    public OpenIdProviderConfigurationClient(@NonNull final String authority,
                                             @NonNull final String path, @NonNull final String endpointVersion)
            throws URISyntaxException {
        mIssuer = new CommonURIBuilder()
                .setScheme("https")
                .setHost(authority)
                .setPathSegments(path, endpointVersion)
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
            final URI configUrl = new URI(mIssuer + sWellKnownConfig);

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

            final HttpResponse providerConfigResponse = httpClient.get(configUrl.toURL(),
                    new HashMap<String, String>());

            final int statusCode = providerConfigResponse.getStatusCode();

            if (HttpURLConnection.HTTP_OK != statusCode
                    || StringUtil.isNullOrEmpty(providerConfigResponse.getBody())) {
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
        } catch (IOException | URISyntaxException e) {
            throw new ServiceException(
                    OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD,
                    "IOException while requesting metadata",
                    e
            );
        }
    }

    private void cacheConfiguration(@NonNull final URI configUrl,
                                    @NonNull final OpenIdProviderConfiguration parsedConfig) {
        sConfigCache.put(configUrl, parsedConfig);
    }

    private OpenIdProviderConfiguration parseMetadata(@NonNull final String body) {
        return mGson.fromJson(body, OpenIdProviderConfiguration.class);
    }

}
