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

import static com.microsoft.identity.common.java.exception.ServiceException.OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD;
import static com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud.BLEU_CLOUD_HOST;
import static com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud.CHINA_CLOUD_HOST;
import static com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud.DELOS_CLOUD_HOST;
import static com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud.PPE_CLOUD_HOST;
import static com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud.PUBLIC_CLOUD_HOST;
import static com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST;
import static com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud.US_GOV_CLOUD_HOST;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.Environment;
import com.microsoft.identity.common.java.cache.HttpCache;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.java.providers.IdentityProvider;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfigurationClient;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.UrlUtil;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.NonNull;

import javax.annotation.Nullable;

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

    private static ConcurrentMap<String, AzureActiveDirectoryCloud> sAadClouds = new ConcurrentHashMap<>();
    private static Environment sEnvironment = Environment.Production;
    private static final HttpClient httpClient = UrlConnectionHttpClient.getDefaultInstance();

    // Known cloud hosts that can be used to perform instance discovery.
    private static final Set<String> KNOWN_CLOUD_DISCOVERY_HOSTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            PUBLIC_CLOUD_HOST,
            PPE_CLOUD_HOST,
            CHINA_CLOUD_HOST,
            US_GOV_CLOUD_HOST,
            BLEU_CLOUD_HOST,
            DELOS_CLOUD_HOST,
            SOVSG_CLOUD_HOST
    )));

    static {
        // Pre-seed sAadClouds with sovereign cloud metadata so they are recognized
        // without a network call. This is needed because discovery response
        // does not contain these new clouds yet.
        for (final AzureActiveDirectoryCloud cloud : new AzureActiveDirectoryCloud[]{
                AzureActiveDirectoryCloud.BLEU,
                AzureActiveDirectoryCloud.DELOS,
                AzureActiveDirectoryCloud.SOVSG
        }) {
            sAadClouds.put(
                    cloud.getPreferredNetworkHostName().toLowerCase(Locale.US),
                    cloud
            );
        }
    }

    /**
     * Returns true if the given host is a known cloud host that has its own
     * instance discovery endpoint.
     *
     * @param host The hostname to check.
     * @return true if the host is in the known cloud discovery hosts list.
     */
    public static boolean isKnownCloudDiscoveryHost(@NonNull final String host) {
        return KNOWN_CLOUD_DISCOVERY_HOSTS.contains(host.toLowerCase(Locale.US));
    }

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

    public static synchronized boolean isValidCloudHost(@NonNull final URL authorityUrl) {
        return hasCloudHost(authorityUrl) && getAzureActiveDirectoryCloud(authorityUrl).isValidated();
    }

    public static synchronized void setEnvironment(@NonNull final Environment environment) {
        if (environment != sEnvironment) {
            sEnvironment = environment;
        }
    }

    public static synchronized Environment getEnvironment() {
        return sEnvironment;
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
     * Checks if the passed in authority URL belongs to public cloud (cloud for login.microsoftonline.com).
     */
    public static synchronized boolean isPublicAzureActiveDirectoryCloud(@NonNull final URL authorityUrl) throws ClientException {
        try {
            return Objects.equals(getAzureActiveDirectoryCloud(authorityUrl), getAzureActiveDirectoryCloud(new URL(getDefaultCloudUrl())));
        } catch (final MalformedURLException e) {
            throw new ClientException(ClientException.MALFORMED_URL, e.getMessage(), e);
        }
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
    }

    public static synchronized String getDefaultCloudUrl() {
        if (sEnvironment == Environment.PreProduction) {
            return AzureActiveDirectoryEnvironment.PREPRODUCTION_CLOUD_URL;
        } else if (sEnvironment == Environment.OneBox) {
            return AzureActiveDirectoryEnvironment.ONEBOX_CLOUD_URL;
        } else {
            return AzureActiveDirectoryEnvironment.PRODUCTION_CLOUD_URL;
        }
    }

    public static synchronized void performCloudDiscovery()
            throws ClientException {
        performCloudDiscoveryForCloudUrl(getDefaultCloudUrl());
    }

    /**
     * Performs instance discovery using the specified cloud URL as the discovery endpoint host.
     *
     * @param cloudUrl The base cloud URL to use for the discovery request
     *                 (e.g. "https://login.sovcloud-identity.fr").
     */
        public static synchronized void performCloudDiscoveryForCloudUrl(@NonNull final String cloudUrl)
            throws ClientException {
        final String methodName = ":performCloudDiscoveryForCloudUrl";
        final URI instanceDiscoveryRequestUri;
        try {
            instanceDiscoveryRequestUri = new CommonURIBuilder(cloudUrl + AAD_INSTANCE_DISCOVERY_ENDPOINT)
                    .setParameter(API_VERSION, API_VERSION_VALUE)
                    .setParameter(AUTHORIZATION_ENDPOINT, cloudUrl + "/common/oauth2/v2.0/authorize")
                    .build();
        } catch (URISyntaxException e) {
            throw new ClientException(ClientException.MALFORMED_URL, e.getMessage(), e);
        }

        final HttpResponse response = httpClient.get(
                UrlUtil.makeUrl(instanceDiscoveryRequestUri.toString()),
                new HashMap<>());

        if (response.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            Logger.warn(TAG + methodName, "Error getting cloud information from " + cloudUrl);
        } else {
            // Our request was successful. Flush the HTTP cache to disk. Should only happen once
            // per app launch. Instance Discovery Metadata will be cached in-memory
            // until the app is killed.
            HttpCache.flush();

            final AzureActiveDirectoryInstanceResponse instanceResponse =
                    ObjectMapper.deserializeJsonStringToObject(
                            response.getBody(),
                            AzureActiveDirectoryInstanceResponse.class
                    );

            Logger.verbose(TAG + methodName, "Discovered ["
                    + instanceResponse.getClouds().size() + "] clouds.");

            for (final AzureActiveDirectoryCloud cloud : instanceResponse.getClouds()) {
                cloud.setIsValidated(true); // Mark the deserialized Clouds as validated
                for (final String alias : cloud.getHostAliases()) {
                    sAadClouds.put(alias.toLowerCase(Locale.US), cloud);
                }
            }
        }
    }

    /**
     * Ensures that cloud discovery has been completed using the default global endpoint.
     * Delegates to {@link #ensureCloudDiscoveryForAuthority(URL)} with the default cloud URL.
     */
    public static synchronized void ensureCloudDiscoveryComplete() throws ClientException {
        try {
            ensureCloudDiscoveryForAuthority(new URL(getDefaultCloudUrl()));
        } catch (final MalformedURLException e) {
            throw new ClientException(ClientException.MALFORMED_URL, e.getMessage(), e);
        }
    }

    /**
     * Ensures that cloud discovery has been completed for the given authority.
     * Extracts the authority URL and delegates to {@link #ensureCloudDiscoveryForAuthority(URL)}.
     * If authority is null or has no URL, falls back to the default global endpoint.
     *
     * @param authority The authority whose URL determines the discovery endpoint, or null to use the default.
     */
    public static synchronized void ensureCloudDiscoveryForAuthority(@Nullable final Authority authority)
            throws ClientException {
        ensureCloudDiscoveryForAuthority(
                authority != null ? authority.getAuthorityURL() : null
        );
    }

    /**
     * Ensures that cloud discovery has been completed for the given authority URL.
     * If authorityUrl is null, falls back to the default global endpoint.
     * If the authority's host is already present in the cloud metadata cache, this is a no-op.
     * Otherwise, performs discovery using the appropriate endpoint:
     * <ul>
     *   <li>For known cloud hosts — queries that cloud's own discovery endpoint.</li>
     *   <li>For unknown hosts — falls back to the default global discovery endpoint.</li>
     * </ul>
     *
     * @param authorityUrl The authority URL whose host determines the discovery endpoint, or null to use the default.
     */
    public static synchronized void ensureCloudDiscoveryForAuthority(@Nullable final URL authorityUrl)
            throws ClientException {
        if (authorityUrl == null) {
            ensureCloudDiscoveryComplete();
            return;
        }
        final String host = authorityUrl.getHost();
        if (host == null) {
            return;
        }
        final String hostLower = host.toLowerCase(Locale.US);
        // Already in cache — no discovery needed.
        if (sAadClouds.containsKey(hostLower)) {
            return;
        }
        if (isKnownCloudDiscoveryHost(hostLower)) {
            // Known cloud host not yet in cache — discover from its own endpoint.
            performCloudDiscoveryForCloudUrl(authorityUrl.getProtocol() + "://" + host);
        } else {
            // Unknown host — fall back to global discovery.
            performCloudDiscovery();
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
     * Loads the OpenID Provider Configuration metadata for the specified tenant by first constructing
     * AAD authority URL like https://login.microsoftonline.com/{tenant}/v2.0/.well-known/openid-configuration
     *
     * @param tenant The tenant identifier. Could be tenant id (guid) or tenant name (contoso.onmicrosoft.com).
     * @return The OpenID Provider Configuration for the specified tenant.
     * @throws ServiceException If there is an error loading the configuration.
     * @throws ClientException  If there is a client-side error.
     */
    public static OpenIdProviderConfiguration loadOpenIdProviderConfigurationMetadataForTenant(
            @NonNull final String tenant
    ) throws ServiceException, ClientException {
        final OpenIdProviderConfigurationClient client =
                new OpenIdProviderConfigurationClient();
        try {
            final String tenantedAuthorityUrl = new CommonURIBuilder(getDefaultCloudUrl())
                    .setPathSegments(tenant)
                    .build()
                    .toString();
            return client.loadOpenIdProviderConfigurationFromAuthority(tenantedAuthorityUrl);
        } catch (final URISyntaxException e) {
            throw new ClientException(
                    OPENID_PROVIDER_CONFIGURATION_FAILED_TO_LOAD,
                    "URISyntaxException while requesting metadata",
                    e
            );
        }
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

    /**
     * Builds and validates the authority from the WebApp sender URL.
     *
     * @param senderUrl The WebApp sender URL.
     * @return The normalized authority URL string.
     * @throws ClientException If the URL is malformed or the host is not recognized/validated.
     */
    public static String buildAndValidateAuthorityFromWebAppSender(final String senderUrl) throws ClientException {
        final String methodTag = TAG + ":buildAndValidateAuthorityFromWebAppSender";
        try {
            final URI uri = new URI(senderUrl);
            final String scheme = uri.getScheme();
            if (scheme == null) {
                throw new ClientException(ClientException.MALFORMED_URL, "Missing scheme in sender url");
            }
            final String host = uri.getHost();
            if (host == null) {
                throw new ClientException(ClientException.MALFORMED_URL, "Missing host in sender url");
            }

            final URI normalized = new URI(scheme + "://" + host + "/common");
            final URL authorityUrl = normalized.toURL();

            ensureCloudDiscoveryForAuthority(authorityUrl);

            if (!hasCloudHost(authorityUrl)) {
                Logger.warn(methodTag, "Host not found in known AAD clouds: " + host);
                throw new ClientException(
                        ClientException.MALFORMED_URL,
                        "Unrecognized AAD cloud host: " + host
                );
            }

            if (!isValidCloudHost(authorityUrl)) {
                Logger.warn(methodTag, "Host not validated as AAD cloud: " + host);
                throw new ClientException(
                        ClientException.MALFORMED_URL,
                        "AAD cloud host not validated: " + host
                );
            }

            return normalized.toString();
        } catch (final URISyntaxException e) {
            throw new ClientException(ClientException.MALFORMED_URL, "Invalid sender url syntax", e);
        } catch (final MalformedURLException e) {
            throw new ClientException(ClientException.MALFORMED_URL, "Invalid authority URL formed", e);
        }
    }
}
