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
package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.util.ported.ObjectUtils;
import com.microsoft.identity.internal.test.labapi.Configuration;
import com.microsoft.identity.internal.testutils.BuildConfig;

class LabAuthenticationHelper extends ConfidentialClientHelper {
    private final static String LAB_APP_ID = "f62c5ae3-bf3a-4af5-afa8-a68b800396e9";
    private final static String SCOPE = "https://default.msidlab.com/.default";
    private String mLabAppSecret = BuildConfig.LAB_CLIENT_SECRET;
    private static LabAuthenticationHelper sLabAuthHelper;

    private LabAuthenticationHelper(final String labAppSecret) {
        mLabAppSecret = labAppSecret;
    }
    private LabAuthenticationHelper() {

    }

    public static synchronized ConfidentialClientHelper getInstance() {
        if (sLabAuthHelper == null) {
            sLabAuthHelper = new LabAuthenticationHelper();
        }

        return sLabAuthHelper;
    }

    public static synchronized ConfidentialClientHelper getInstance(String labAppSecret) {
        if (sLabAuthHelper == null || !ObjectUtils.equals(sLabAuthHelper.mLabAppSecret, labAppSecret)) {
            sLabAuthHelper = new LabAuthenticationHelper(labAppSecret);
        }

        return sLabAuthHelper;
    }

    @Override
    public void setupApiClientWithAccessToken(final String accessToken) {
        Configuration.getDefaultApiClient().setAccessToken(accessToken);
    }

    @Override
    public TokenRequest createTokenRequest() {
        TokenRequest tr = new MicrosoftStsTokenRequest();
        tr.setClientSecret(mLabAppSecret);
        tr.setClientId(LAB_APP_ID);
        tr.setScope(SCOPE);
        return tr;
    }
}
