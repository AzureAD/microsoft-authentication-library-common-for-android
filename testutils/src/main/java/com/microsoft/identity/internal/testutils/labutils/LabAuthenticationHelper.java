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

import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.internal.test.keyvault.ApiException;
import com.microsoft.identity.internal.test.keyvault.api.SecretsApi;
import com.microsoft.identity.internal.test.keyvault.model.SecretBundle;
import com.microsoft.identity.internal.test.labapi.Configuration;

class LabAuthenticationHelper extends ConfidentialClientHelper {
    private final static String SECRET_NAME_LAB_APP_ID = "LabVaultAppID";
    private final static String SECRET_NAME_LAB_APP_SECRET = "LabVaultAppSecret";
    private final static String KEY_VAULT_API_VERSION = "2016-10-01";
    private final static String SCOPE = "https://msidlab.com/.default";

    private String mAppId;
    private String mAppSecret;

    private static LabAuthenticationHelper sLabAuthHelper;

    private LabAuthenticationHelper() {
        mAppId = null;
        mAppSecret = null;
    }

    public static ConfidentialClientHelper getInstance() {
        if (sLabAuthHelper == null) {
            sLabAuthHelper = new LabAuthenticationHelper();
        }

        return sLabAuthHelper;
    }

    @Override
    public void setupApiClientWithAccessToken(final String accessToken) {
        Configuration.getDefaultApiClient().setAccessToken(accessToken);
    }

    @Override
    public TokenRequest createTokenRequest() {
        final String appId = getAppId();
        final String appSecret = getAppSecret();

        TokenRequest tr = new MicrosoftStsTokenRequest();

        tr.setClientSecret(appSecret);
        tr.setClientId(appId);
        tr.setScope(SCOPE);
        return tr;
    }

    private String getAppId() {
        if (mAppId == null) {
            mAppId = getSecretFromKeyVault(SECRET_NAME_LAB_APP_ID);
        }

        return mAppId;
    }

    private String getAppSecret() {
        if (mAppSecret == null) {
            mAppSecret = getSecretFromKeyVault(SECRET_NAME_LAB_APP_SECRET);
        }

        return mAppSecret;
    }

    private String getSecretFromKeyVault(final String secretName) {
        KeyVaultAuthHelper.getInstance().setupApiClientWithAccessToken();
        SecretsApi secretsApi = new SecretsApi();
        SecretBundle secretBundle;
        try {
            secretBundle = secretsApi.getSecret(secretName, "", KEY_VAULT_API_VERSION);
        } catch (ApiException e) {
            throw new IllegalStateException("Unable to get lab secret from KeyVault");
        }
        return secretBundle.getValue();
    }
}
