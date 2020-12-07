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

import android.text.TextUtils;

import androidx.annotation.Nullable;

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
    private final String mKeyVaultSecret;

    private static LabAuthenticationHelper sLabAuthHelper;

    private LabAuthenticationHelper(String keyVaultSecret) {
        mAppId = null;
        mAppSecret = null;
        this.mKeyVaultSecret = keyVaultSecret;
    }

    private LabAuthenticationHelper() {
        mAppId = null;
        mAppSecret = null;
        // Changing all of the classes that use this to be configurable is a large change.  This
        // isn't necessarily the safest way to do this, but doesn't require a rewrite of everything
        // that uses this package.
        String buildConfigSecret = com.microsoft.identity.internal.testutils.BuildConfig.LAB_CLIENT_SECRET;
        if (TextUtils.isEmpty(buildConfigSecret)) {
            buildConfigSecret = getBuildConfigSecretFromClasspath();
        }
        mKeyVaultSecret = buildConfigSecret;
    }

    public static synchronized ConfidentialClientHelper getInstance(String keyVaultSecret) {
        if (sLabAuthHelper == null) {
            sLabAuthHelper = new LabAuthenticationHelper(keyVaultSecret);
        }

        return sLabAuthHelper;
    }

    public static synchronized ConfidentialClientHelper getInstance() {
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
        final String appId = getAppId(mKeyVaultSecret);
        final String appSecret = getAppSecret(mKeyVaultSecret);

        TokenRequest tr = new MicrosoftStsTokenRequest();

        tr.setClientSecret(appSecret);
        tr.setClientId(appId);
        tr.setScope(SCOPE);
        return tr;
    }

    private String getAppId(final String secret) {
        if (mAppId == null) {
            mAppId = getSecretFromKeyVault(SECRET_NAME_LAB_APP_ID, secret);
        }

        return mAppId;
    }

    private String getAppSecret(String secret) {
        if (mAppSecret == null) {
            mAppSecret = getSecretFromKeyVault(SECRET_NAME_LAB_APP_SECRET, secret);
        }

        return mAppSecret;
    }

    private String getSecretFromKeyVault(final String secretName, final @Nullable String secret) {
        KeyVaultAuthHelper.getInstanceWithSecret(secret).setupApiClientWithAccessToken();
        SecretsApi secretsApi = new SecretsApi();
        SecretBundle secretBundle;
        try {
            secretBundle = secretsApi.getSecret(secretName, "", KEY_VAULT_API_VERSION);
            return secretBundle.getValue();
        } catch (ApiException e) {
            throw new IllegalStateException("Unable to get lab secret from KeyVault", e);
        }
    }
}
