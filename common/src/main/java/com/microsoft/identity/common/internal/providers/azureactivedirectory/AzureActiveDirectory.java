package com.microsoft.identity.common.internal.providers.azureactivedirectory;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.IdentityProvider;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implements the IdentityProvider base class...
 */
public class AzureActiveDirectory extends IdentityProvider {

    // Constants used to parse cloud discovery document metadata
    private static final String TENANT_DISCOVERY_ENDPOINT = "tenant_discovery_endpoint";
    private static final String METADATA = "metadata";
    private static final String PREFERRED_NETWORK = "preferred_network";
    private static final String PREFERRED_CACHE = "preferred_cache";
    private static final String ALIASES = "aliases";

    private static ConcurrentMap<String, AzureActiveDirectoryCloud> sAadClouds = new ConcurrentHashMap<>();

    public OAuth2Strategy createOAuth2Strategy() {
        return new AzureActiveDirectoryOAuth2Strategy();
    }

    static boolean hasCloudHost(final URL authorityUrl) {
        return sAadClouds.containsKey(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    static boolean isValidCloudHost(final URL authorityUrl) {
        return hasCloudHost(authorityUrl) && getAzureActiveDirectoryCloud(authorityUrl).isValidated();
    }

    static AzureActiveDirectoryCloud getAzureActiveDirectoryCloud(final URL authorityUrl) {
        return sAadClouds.get(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    /**
     * Initialize the in-memory cache of validated AAD cloud instances.
     *
     * @param authorityHost     Host of the Authority used to obtain the metadata.
     * @param discoveryResponse The response JSON serialized into a Map.
     * @throws JSONException If a parsing error is encountered.
     */
    public static void initializeCloudMetadata(final String authorityHost, final Map<String, String> discoveryResponse) throws JSONException {
        final boolean tenantDiscoveryEndpointReturned = discoveryResponse.containsKey(TENANT_DISCOVERY_ENDPOINT);
        final String metadata = discoveryResponse.get(METADATA);

        if (!tenantDiscoveryEndpointReturned) {
            sAadClouds.put(authorityHost, new AzureActiveDirectoryCloud(false));
            return;
        }

        if (StringExtensions.isNullOrBlank(metadata)) {
            sAadClouds.put(authorityHost, new AzureActiveDirectoryCloud(authorityHost, authorityHost));
            return;
        }

        final List<AzureActiveDirectoryCloud> clouds = deserializeClouds(new JSONArray(metadata));

        for (final AzureActiveDirectoryCloud cloud : clouds) {
            for (final String alias : cloud.getHostAliases()) {
                sAadClouds.put(alias.toLowerCase(Locale.US), cloud);
            }
        }
    }

    /**
     * Deserializes the supplied JSONArray of cloud instances into a native List.
     *
     * @param jsonCloudArray The cloud array.
     * @return Native List of clouds.
     * @throws JSONException If a parsing error is encountered.
     */
    private static List<AzureActiveDirectoryCloud> deserializeClouds(final JSONArray jsonCloudArray) throws JSONException {
        return new ArrayList<AzureActiveDirectoryCloud>() {{
            for (int ii = 0; ii < jsonCloudArray.length(); ii++) {
                add(deserializeCloud(jsonCloudArray.getJSONObject(ii)));
            }
        }};
    }

    /**
     * Deserializes the supplied JSONObject into a native representation of a cloud.
     *
     * @param jsonCloud The cloud instance, as JSON.
     * @return The native object representing this cloud instance.
     * @throws JSONException If a parsing error is encountered.
     */
    private static AzureActiveDirectoryCloud deserializeCloud(final JSONObject jsonCloud) throws JSONException {
        return new AzureActiveDirectoryCloud(
                jsonCloud.getString(PREFERRED_NETWORK),
                jsonCloud.getString(PREFERRED_CACHE),
                deserializeAliases(jsonCloud.getJSONArray(ALIASES))
        );
    }

    /**
     * Deserializes the aliases associated with a cloud.
     *
     * @param aliases The array of aliases, as JSON.
     * @return List of Strings containing the aliases.
     * @throws JSONException If a parsing error is encountered.
     */
    private static List<String> deserializeAliases(final JSONArray aliases) throws JSONException {
        return new ArrayList<String>() {{
            for (int ii = 0; ii < aliases.length(); ii++) {
                add(aliases.getString(ii));
            }
        }};
    }
}
