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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.providers.IdentityProvider;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implements the IdentityProvider base class...
 */
public class AzureActiveDirectory
        extends IdentityProvider<AzureActiveDirectoryOAuth2Strategy, AzureActiveDirectoryOAuth2Configuration> {

    // Constants used to parse cloud discovery document metadata
    private static final String TENANT_DISCOVERY_ENDPOINT = "tenant_discovery_endpoint";
    private static final String METADATA = "metadata";

    private static ConcurrentMap<String, AzureActiveDirectoryCloud> sAadClouds = new ConcurrentHashMap<>();

    @Override
    public AzureActiveDirectoryOAuth2Strategy createOAuth2Strategy(AzureActiveDirectoryOAuth2Configuration config) {
        return new AzureActiveDirectoryOAuth2Strategy(config);
    }

    static boolean hasCloudHost(final URL authorityUrl) {
        return sAadClouds.containsKey(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    static boolean isValidCloudHost(final URL authorityUrl) {
        return hasCloudHost(authorityUrl) && getAzureActiveDirectoryCloud(authorityUrl).isValidated();
    }

    /**
     * @param authorityUrl URL
     * @return AzureActiveDirectoryCloud
     */
    public static AzureActiveDirectoryCloud getAzureActiveDirectoryCloud(final URL authorityUrl) {
        return sAadClouds.get(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    /**
     * @param host  String
     * @param cloud AzureActiveDirectoryCloud
     */
    public static void putCloud(final String host, final AzureActiveDirectoryCloud cloud) {
        sAadClouds.put(host.toLowerCase(Locale.US), cloud);
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

        final List<AzureActiveDirectoryCloud> clouds = deserializeClouds(metadata);

        for (final AzureActiveDirectoryCloud cloud : clouds) {
            cloud.setIsValidated(true); // Mark the deserialized Clouds as validated
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
    private static List<AzureActiveDirectoryCloud> deserializeClouds(final String jsonCloudArray) throws JSONException {
        Type listType = new TypeToken<List<AzureActiveDirectoryCloud>>() {
        }.getType();
        return new Gson().fromJson(jsonCloudArray, listType);
    }

}
