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

import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.providers.oauth2.AccessToken;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static com.microsoft.identity.common.java.providers.oauth2.TokenRequest.GrantTypes.CLIENT_CREDENTIALS;

abstract class ConfidentialClientHelper {

    public static final String LAB_CLIENT_SECRET_FIELD_NAME = "LAB_CLIENT_SECRET";
    private static String sClasspathSecret = null;

    // tenant id where lab api and key vault api is registered
    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private String mAccessToken;

    abstract TokenRequest createTokenRequest()
            throws LabApiException;

    abstract void setupApiClientWithAccessToken(String accessToken);

    private String getAccessToken()
            throws LabApiException, IOException {
        if (mAccessToken == null) {
            mAccessToken = requestAccessTokenForAutomation();
        }

        return mAccessToken;
    }

    /**
     * Yep.  Hardcoding this method to retrieve access token for MSIDLABS
     */
    private String requestAccessTokenForAutomation()
            throws IOException, LabApiException {
        String accessToken = null;

        final TokenRequest tokenRequest = this.createTokenRequest();
        tokenRequest.setGrantType(CLIENT_CREDENTIALS);

        final AccountsInOneOrganization aadAudience = new AccountsInOneOrganization(TENANT_ID);
        final AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(aadAudience);

        try {
            final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder().build();
            OAuth2Strategy<AccessToken, BaseAccount, AuthorizationRequest, AuthorizationRequest.Builder,
                    IAuthorizationStrategy, OAuth2Configuration, OAuth2StrategyParameters,
                    AuthorizationResponse, RefreshToken, TokenRequest, TokenResponse, TokenResult,
                    AuthorizationResult> strategy = authority.createOAuth2Strategy(strategyParameters);
            TokenResult tokenResult = strategy.requestToken(tokenRequest);

            if (tokenResult.getSuccess()) {
                accessToken = tokenResult.getTokenResponse().getAccessToken();
            } else {
                throw new RuntimeException(tokenResult.getErrorResponse().getErrorDescription());
            }
        } catch (final ClientException e) {
            e.printStackTrace();
        }

        return accessToken;
    }

    void setupApiClientWithAccessToken() {
        try {
            setupApiClientWithAccessToken(this.getAccessToken());
        } catch (final Exception e) {
            throw new RuntimeException("Unable to get access token for automation:" + e.getMessage(), e);
        }
    }

}
