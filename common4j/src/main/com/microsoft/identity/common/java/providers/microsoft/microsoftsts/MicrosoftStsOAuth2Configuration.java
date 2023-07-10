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
package com.microsoft.identity.common.java.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Configuration;
import com.microsoft.identity.common.java.util.UrlUtil;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import lombok.NonNull;

public class MicrosoftStsOAuth2Configuration extends AzureActiveDirectoryOAuth2Configuration {

    private static final String TAG = MicrosoftStsOAuth2Configuration.class.getSimpleName();

    private static final String ENDPOINT_SUFFIX = "/oAuth2/v2.0";
    private static final String AUTHORIZE_ENDPOINT_SUFFIX = ENDPOINT_SUFFIX + "/authorize";
    private static final String TOKEN_ENDPOINT_SUFFIX = ENDPOINT_SUFFIX + "/token";
    private static final String DEVICE_AUTHORIZE_ENDPOINT_SUFFIX = ENDPOINT_SUFFIX + "/devicecode";

    /**
     * Get the authorization endpoint to be used for making a authorization request.
     * This must NOT be called from the main thread.
     *
     * @return URL the authorization endpoint
     */
    public URL getAuthorizationEndpoint() {
        return getEndpointUrlFromRootAndSuffix(getAuthorityUrl(), AUTHORIZE_ENDPOINT_SUFFIX);
    }

    /**
     * Return device authorization endpoint to be used in the authorization step of Device Code Flow.
     *
     * @return a URL object for the /devicecode endpoint
     */
    public URL getDeviceAuthorizationEndpoint() {
        return getEndpointUrlFromRootAndSuffix(getAuthorityUrl(), DEVICE_AUTHORIZE_ENDPOINT_SUFFIX);
    }

    /**
     * Get the token endpoint to be used for making a token request.
     * This must NOT be called from the main thread.
     *
     * @return URL the token endpoint
     */
    public URL getTokenEndpoint() {
        return getEndpointUrlFromRootAndSuffix(getAuthorityUrl(), TOKEN_ENDPOINT_SUFFIX);
    }

    private URL getEndpointUrlFromRootAndSuffix(@NonNull URL root, @NonNull String endpointSuffix) {
        final String methodName = ":getEndpointUrlFromRootAndSuffix";
        try {
            return UrlUtil.appendPathToURL(root, endpointSuffix, null);
        } catch (final URISyntaxException | MalformedURLException e) {
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
}
