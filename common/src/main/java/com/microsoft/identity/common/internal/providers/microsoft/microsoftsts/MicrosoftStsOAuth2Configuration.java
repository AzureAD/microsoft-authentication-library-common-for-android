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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdProviderConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdProviderConfigurationClient;

import java.net.URL;

public class MicrosoftStsOAuth2Configuration extends AzureActiveDirectoryOAuth2Configuration {

    private static final String TAG = MicrosoftStsOAuth2Configuration.class.getSimpleName();

    private static final String ENDPOINT_VERSION = "v2.0";
    private static final String FALLBACK_ENDPOINT_SUFFIX = "/oAuth2/v2.0";
    private static final String FALLBACK_AUTHORIZE_ENDPOINT_SUFFIX = FALLBACK_ENDPOINT_SUFFIX + "/authorize";
    private static final String FALLBACK_TOKEN_ENDPOINT_SUFFIX = FALLBACK_ENDPOINT_SUFFIX + "/token";

    /**
     * Get the authorization endpoint to be used for making a authorization request.
     * This must NOT be called from the main thread.
     *
     * @return URL the authorization endpoint
     */
    public URL getAuthorizationEndpoint() {
        final OpenIdProviderConfiguration openIdConfig = getOpenIdWellKnownConfigForAuthority();
        if (openIdConfig != null) {
            return getEndpointUrlFromAuthority(openIdConfig.getAuthorizationEndpoint());
        }
        return getEndpointUrlFromRootAndSuffix(getAuthorityUrl(), FALLBACK_AUTHORIZE_ENDPOINT_SUFFIX);
    }

    /**
     * Get the token endpoint to be used for making a token request.
     * This must NOT be called from the main thread.
     *
     * @return URL the token endpoint
     */
    public URL getTokenEndpoint() {
        final OpenIdProviderConfiguration openIdConfig = getOpenIdWellKnownConfigForAuthority();
        if (openIdConfig != null && openIdConfig.getTokenEndpoint() != null) {
            return getEndpointUrlFromAuthority(openIdConfig.getTokenEndpoint());
        }
        return getEndpointUrlFromRootAndSuffix(getAuthorityUrl(), FALLBACK_TOKEN_ENDPOINT_SUFFIX);
    }

    @Nullable
    private URL getEndpointUrlFromAuthority(@NonNull final String authorityUrl) {
        final String methodName = ":getEndpointUrlFromAuthority";
        try {
            return new URL(authorityUrl);
        } catch (Exception e) {
            Logger.error(
                    TAG + methodName,
                    "Unable to create URL from provided authority.",
                    null);
            Logger.errorPII(
                    TAG + methodName,
                    e.getMessage() +
                            " Unable to create URL from provided authority." +
                            " authority = " + authorityUrl,
                    e);
        }

        return null;
    }

    private URL getEndpointUrlFromRootAndSuffix(@NonNull URL root, @NonNull String endpointSuffix) {
        final String methodName = ":getEndpointUrlFromRootAndSuffix";
        try {
            Uri authorityUri = Uri.parse(root.toString());
            Uri endpointUri = authorityUri.buildUpon()
                    .appendPath(endpointSuffix)
                    .build();
            return new URL(endpointUri.toString());
        } catch (Exception e) {
            Logger.error(
                    TAG + methodName,
                    "Unable to create URL from provided root and suffix.",
                    null);
            Logger.errorPII(
                    TAG + methodName,
                    e.getMessage() +
                            " Unable to create URL from provided root and suffix." +
                            " root = " + root.toString() + " suffix = " + endpointSuffix,
                    e);
        }

        return null;

    }

    /**
     * Get the Open Id Provider Configuration from the authority.
     * This operation must NOT be called from the main thread.
     * This method can return null if errors are encountered and the caller should check the result
     * before using it.
     *
     * @return OpenIdProviderConfiguration if available or null
     */
    @Nullable
    private OpenIdProviderConfiguration getOpenIdWellKnownConfigForAuthority() {
        final URL authority = getAuthorityUrl();
        return getOpenIdWellKnownConfig(authority.getHost(), authority.getPath());
    }

    /**
     * Get the Open Id Provider Configuration based on the host and audience.
     * This operation must NOT be called from the main thread.
     * This method can return null if errors are encountered and the caller should check the result
     * before using it.
     *
     * @param host     the host of authority url
     * @param audience the audience (path) of the authority url
     * @return OpenIdProviderConfiguration if available or null
     */
    @Nullable
    OpenIdProviderConfiguration getOpenIdWellKnownConfig(@NonNull final String host, @NonNull final String audience) {
        final String methodName = ":getOpenIdWellKnownConfig";
        final OpenIdProviderConfigurationClient configurationClient = new OpenIdProviderConfigurationClient(
                host,
                audience,
                ENDPOINT_VERSION);

        OpenIdProviderConfiguration openIdConfig = null;

        try {
            openIdConfig = configurationClient.loadOpenIdProviderConfiguration();
        } catch (ServiceException e) {
            Logger.error(TAG + methodName,
                    e.getMessage(),
                    e);
        }

        return openIdConfig;
    }

}
