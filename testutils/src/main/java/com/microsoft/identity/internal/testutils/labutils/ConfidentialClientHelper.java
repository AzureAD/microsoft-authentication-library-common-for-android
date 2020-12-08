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

import android.text.TextUtils;

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.AccountsInOneOrganization;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Configuration;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Set;

abstract class ConfidentialClientHelper {

    private final static String GRANT_TYPE = "client_credentials";
    public static final String LAB_CLIENT_SECRET_FIELD_NAME = "LAB_CLIENT_SECRET";
    private static String sClasspathSecret = null;

    // tenant id where lab api and key vault api is registered
    private final static String TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    private String mAccessToken;

    abstract TokenRequest createTokenRequest()
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchProviderException, IOException;

    /**
     * What's going on here.  Since the BuildConfig values are compile-time generated, and the way
     * this was working was to pass a property to the gradle build which ended up in the build-time
     * configuration of the testUtils package, if we're going to be able to do this from any consumer
     * we have to have a way to search for this value.  This isn't very scientific, but what we
     * can do is look for every class with the name "BuildConfig" in the "com.microsoft.*" namespace,
     * and find the first class with a non-blank static field named "LAB_CLIENT_SECRET" and use it.
     *
     * This is not really what we want to do, but should let us move to a world where this is passed
     * in on client construction without too much disruption.
     *
     * @return the lab secret string, if found, otherwise an empty string.
     */
    protected synchronized static String getBuildConfigSecretFromClasspath() {
        if (sClasspathSecret != null) {
            return sClasspathSecret;
        }
        final Reflections r = new Reflections("com.microsoft");
        final Set<Class<?>> allClasses = r.getSubTypesOf(Object.class);
        for (final Class<?> clazz : allClasses) {
            if ("BuildConfig".equals(clazz.getSimpleName())) {
                try {
                    final Field labField = clazz.getDeclaredField(LAB_CLIENT_SECRET_FIELD_NAME);
                    if (labField != null) {
                        String secret = (String) labField.get(null);
                        if (!TextUtils.isEmpty(secret)) {
                            Logger.info("LabAuthenticationHelper:init", "Found lab secret on class with name " + clazz.getCanonicalName());
                            sClasspathSecret = secret;
                            return secret;
                        }
                    }
                    //Swallow all exceptions.
                } catch (final NoSuchFieldException | IllegalAccessException | ClassCastException e) {
                }
            }
        }
        sClasspathSecret = "";
        return sClasspathSecret;
    }

    abstract void setupApiClientWithAccessToken(String accessToken);

    private String getAccessToken()
            throws UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchProviderException, IOException,
            CertificateException {
        if (mAccessToken == null) {
            mAccessToken = requestAccessTokenForAutomation();
        }

        return mAccessToken;
    }

    /**
     * Yep.  Hardcoding this method to retrieve access token for MSIDLABS
     */
    private String requestAccessTokenForAutomation()
            throws CertificateException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException,
            IOException {
        String accessToken = null;

        final TokenRequest tokenRequest = this.createTokenRequest();
        tokenRequest.setGrantType(GRANT_TYPE);

        final AccountsInOneOrganization aadAudience = new AccountsInOneOrganization(TENANT_ID);
        final AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(aadAudience);

        try {
            final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
            OAuth2Strategy<AccessToken, BaseAccount, AuthorizationRequest, AuthorizationRequest.Builder,
                    AuthorizationStrategy, OAuth2Configuration, OAuth2StrategyParameters,
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
            throw new RuntimeException("Unable to get access token for automation:" + e.getMessage());
        }
    }

}
