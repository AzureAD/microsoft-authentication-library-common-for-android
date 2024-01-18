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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.java.authorities.Environment;
import com.microsoft.identity.common.java.cache.HttpCache;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.java.providers.IdentityProvider;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.StringUtil;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.NonNull;

/**
 * Implements the IdentityProvider base class...
 */
public class AzureActiveDirectory
        extends IdentityProvider<AzureActiveDirectoryOAuth2Strategy, AzureActiveDirectoryOAuth2Configuration> {

    private static final String TAG = AzureActiveDirectory.class.getSimpleName();

    // Constants used to parse cloud discovery document metadata
    private static final String TENANT_DISCOVERY_ENDPOINT = "tenant_discovery_endpoint";
    private static final String METADATA = "metadata";
    private static final String AAD_INSTANCE_DISCOVERY_ENDPOINT = "/common/discovery/instance";
    private static final String API_VERSION = "api-version";
    private static final String API_VERSION_VALUE = "1.1";
    private static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    private static final String AUTHORIZATION_ENDPOINT_VALUE = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";

    private static ConcurrentMap<String, AzureActiveDirectoryCloud> sAadClouds = new ConcurrentHashMap<>();
    private static boolean sIsInitialized = false;
    private static Environment sEnvironment = Environment.Production;
    private static final HttpClient httpClient = UrlConnectionHttpClient.getDefaultInstance();

    private static final ReadWriteLock aadLock = new ReentrantReadWriteLock();

    @Override
    public AzureActiveDirectoryOAuth2Strategy createOAuth2Strategy(@NonNull final AzureActiveDirectoryOAuth2Configuration config,
                                                                   @NonNull final IPlatformComponents commonComponents) throws ClientException {
        final OAuth2StrategyParameters parameters = OAuth2StrategyParameters.builder()
                .platformComponents(commonComponents)
                .build();

        return new AzureActiveDirectoryOAuth2Strategy(config, parameters);
    }

    public static synchronized boolean hasCloudHost(@NonNull final URL authorityUrl) {
        return sAadClouds.containsKey(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    static synchronized boolean isValidCloudHost(@NonNull final URL authorityUrl) {
        return hasCloudHost(authorityUrl) && getAzureActiveDirectoryCloud(authorityUrl).isValidated();
    }

    public static boolean isInitialized() {
        aadLock.readLock().lock();
        try {
            return sIsInitialized;
        } finally {
            aadLock.readLock().unlock();
        }
    }

    public static void setEnvironment(@NonNull final Environment environment) {
        aadLock.writeLock().lock();
        try {
            if (environment != sEnvironment) {
                // Environment changed, so mark sIsInitialized to false
                // to make a instance discovery network request for this environment.
                sIsInitialized = false;
                sEnvironment = environment;
            }
        } finally {
            aadLock.writeLock().unlock();
        }

    }

    public static Environment getEnvironment() {
        aadLock.readLock().lock();
        try {
            return sEnvironment;
        } finally {
            aadLock.readLock().unlock();
        }
    }

    /**
     * @param authorityUrl URL
     * @return AzureActiveDirectoryCloud
     */
    public static synchronized AzureActiveDirectoryCloud getAzureActiveDirectoryCloud(@NonNull final URL authorityUrl) {
        return sAadClouds.get(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    /**
     * @param preferredCacheHostName String
     * @return AzureActiveDirectoryCloud
     */
    public static synchronized AzureActiveDirectoryCloud getAzureActiveDirectoryCloudFromHostName(@NonNull final String preferredCacheHostName) {
        return sAadClouds.get(preferredCacheHostName.toLowerCase(Locale.US));
    }

    /**
     * @param host  String
     * @param cloud AzureActiveDirectoryCloud
     */
    public static synchronized void putCloud(@NonNull final String host, final AzureActiveDirectoryCloud cloud) {
        sAadClouds.put(host.toLowerCase(Locale.US), cloud);
    }

    /**
     * Initialize the in-memory cache of validated AAD cloud instances.
     *
     * @param authorityHost     Host of the Authority used to obtain the metadata.
     * @param discoveryResponse The response JSON serialized into a Map.
     * @throws JSONException If a parsing error is encountered.
     */
    public static synchronized void initializeCloudMetadata(@NonNull final String authorityHost,
                                                            @NonNull final Map<String, String> discoveryResponse) throws JSONException {
        final boolean tenantDiscoveryEndpointReturned = discoveryResponse.containsKey(TENANT_DISCOVERY_ENDPOINT);
        final String metadata = discoveryResponse.get(METADATA);

        if (!tenantDiscoveryEndpointReturned) {
            sAadClouds.put(authorityHost, new AzureActiveDirectoryCloud(false));
            return;
        }

        if (StringUtil.isNullOrEmpty(metadata)) {
            sAadClouds.put(authorityHost, new AzureActiveDirectoryCloud(authorityHost, authorityHost));
            return;
        }

        final List<AzureActiveDirectoryCloud> clouds = deserializeClouds(metadata);

        for (final AzureActiveDirectoryCloud cloud : clouds) {
            cloud.setIsValidated(true); // Mark the deserialized Clouds as validated
            for (final String alias : cloud.getHostAliases()) {
                sAadClouds.put(alias.toLowerCase(Locale.US), cloud);
            }
        }

        aadLock.writeLock().lock();
        try {
            sIsInitialized = true;
        } finally {
            aadLock.writeLock().unlock();
        }
    }

    public static String getDefaultCloudUrl() {
        aadLock.readLock().lock();
        try {
            if (sEnvironment == Environment.PreProduction) {
                return AzureActiveDirectoryEnvironment.PREPRODUCTION_CLOUD_URL;
            } else {
                return AzureActiveDirectoryEnvironment.PRODUCTION_CLOUD_URL;
            }
        } finally {
            aadLock.readLock().unlock();
        }
    }

    public static synchronized void performCloudDiscovery()
            throws IOException, URISyntaxException {
        final String methodName = ":performCloudDiscovery";
        final URI instanceDiscoveryRequestUri = new CommonURIBuilder(getDefaultCloudUrl() + AAD_INSTANCE_DISCOVERY_ENDPOINT)
                .setParameter(API_VERSION, API_VERSION_VALUE)
                .setParameter(AUTHORIZATION_ENDPOINT, AUTHORIZATION_ENDPOINT_VALUE)
                .build();

        final HttpResponse response =
                httpClient.get(new URL(instanceDiscoveryRequestUri.toString()),
                        new HashMap<String, String>());

        aadLock.writeLock().lock();
        try {
            if (response.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                Logger.warn(TAG + methodName, "Error getting cloud information");
            } else {
                // Our request was successful. Flush the HTTP cache to disk. Should only happen once
                // per app launch. Instance Discovery Metadata will be cached in-memory
                // until the app is killed.
                HttpCache.flush();

                Logger.info(TAG + methodName, "Parsing response.");
                AzureActiveDirectoryInstanceResponse instanceResponse =
                        ObjectMapper.deserializeJsonStringToObject(
                                response.getBody(),
                                AzureActiveDirectoryInstanceResponse.class
                        );
                Logger.info(TAG + methodName, "Discovered ["
                        + instanceResponse.getClouds().size() + "] clouds.");

                for (final AzureActiveDirectoryCloud cloud : instanceResponse.getClouds()) {
                    cloud.setIsValidated(true); // Mark the deserialized Clouds as validated
                    for (final String alias : cloud.getHostAliases()) {
                        sAadClouds.put(alias.toLowerCase(Locale.US), cloud);
                    }
                }

                sIsInitialized = true;
            }
        } finally {
        aadLock.writeLock().unlock();
        }
    }

    public static synchronized Set<String> getHosts() {
        if (null != sAadClouds) {
            return sAadClouds.keySet();
        }

        return null;
    }

    public static synchronized List<AzureActiveDirectoryCloud> getClouds() {
        if (null != sAadClouds) {
            return new ArrayList<>(sAadClouds.values());
        }

        return new ArrayList<>();
    }

    /**
     * Deserializes the supplied JSONArray of cloud instances into a native List.
     *
     * @param jsonCloudArray The cloud array.
     * @return Native List of clouds.
     * @throws JSONException If a parsing error is encountered.
     */
    private static List<AzureActiveDirectoryCloud> deserializeClouds(final String jsonCloudArray) throws JSONException {
        final Type listType = TypeToken.getParameterized(List.class, AzureActiveDirectoryCloud.class).getType();
        return new Gson().fromJson(jsonCloudArray, listType);
    }

}
