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

    public URL getAuthorizationEndpoint() {
        final OpenIdProviderConfiguration openIdConfig = getOpenIdWellKnownConfigForAuthority();
        if (openIdConfig != null) {
            return getEndpoint(openIdConfig.getAuthorizationEndpoint());
        }
        return getEndpoint(getAuthorityUrl(), FALLBACK_AUTHORIZE_ENDPOINT_SUFFIX);
    }

    public URL getTokenEndpoint() {
        final OpenIdProviderConfiguration openIdConfig = getOpenIdWellKnownConfigForAuthority();
        if (openIdConfig != null && openIdConfig.getTokenEndpoint() != null) {
            return getEndpoint(openIdConfig.getTokenEndpoint());
        }
        return getEndpoint(getAuthorityUrl(), FALLBACK_TOKEN_ENDPOINT_SUFFIX);
    }


    private URL getEndpoint(String endpoint) {
        try {
            return new URL(endpoint);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private URL getEndpoint(URL root, String endpoint) {
        try {
            Uri authorityUri = Uri.parse(root.toString());
            Uri endpointUri = authorityUri.buildUpon()
                    .appendPath(endpoint)
                    .build();
            return new URL(endpointUri.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    private OpenIdProviderConfiguration getOpenIdWellKnownConfigForAuthority() {
        final URL authority = getAuthorityUrl();
        return getOpenIdWellKnownConfig(authority.getHost(), authority.getPath());
    }

    OpenIdProviderConfiguration getOpenIdWellKnownConfig(final String host, final String audience) {
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
