//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.internal.testutils.BuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

abstract class ConfidentialClientHelper {

    public static final String LAB_CLIENT_SECRET_FIELD_NAME = "LAB_CLIENT_SECRET";
    private static String sClasspathSecret = null;

    // tenant id where lab api and key vault api is registered
    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    String mAccessToken;

    abstract TokenRequest createTokenRequest()
            throws LabApiException;

    abstract void setupApiClientWithAccessToken(String accessToken);

    String getAccessToken()
            throws LabApiException {
        if (mAccessToken == null) {
            mAccessToken = requestAccessTokenForAutomation();
        }

        return mAccessToken;
    }

    private String requestAccessTokenForAutomation()
            throws LabApiException {
        return (new LabApiAuthenticationClient(BuildConfig.LAB_CLIENT_SECRET)).getAccessToken();
    }

    void setupApiClientWithAccessToken() {
        try {
            setupApiClientWithAccessToken(this.getAccessToken());
        } catch (final Exception e) {
            throw new RuntimeException("Unable to get access token for automation:" + e.getMessage(), e);
        }
    }

}
