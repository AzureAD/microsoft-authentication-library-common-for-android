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
package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.dto.RefreshToken;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.common.internal.providers.oauth2.IDToken.PREFERRED_USERNAME;

public class MicrosoftStsAccountCredentialAdapter
        implements IAccountCredentialAdapter
        <MicrosoftStsOAuth2Strategy,
                MicrosoftStsAuthorizationRequest,
                MicrosoftStsTokenResponse,
                MicrosoftAccount,
                com.microsoft.identity.common.internal.providers.oauth2.RefreshToken> {

    private static final String TAG = MicrosoftStsAccountCredentialAdapter.class.getSimpleName();

    // TODO move me!
    private static final String BEARER = "Bearer";
    private static final String FOCI_PREFIX = "foci-";

    @Override
    public Account createAccount(
            final MicrosoftStsOAuth2Strategy strategy,
            final MicrosoftStsAuthorizationRequest request,
            final MicrosoftStsTokenResponse response) {
        final String methodName = "createAccount";
        Logger.entering(TAG, methodName, strategy, request, response);

        final Account account = new Account(strategy.createAccount(response));

        Logger.exiting(TAG, methodName, account);

        return account;
    }

    @Override
    public AccessToken createAccessToken(
            final MicrosoftStsOAuth2Strategy strategy,
            final MicrosoftStsAuthorizationRequest request,
            final MicrosoftStsTokenResponse response) {
        final String methodName = "createAccessToken";
        Logger.entering(TAG, methodName, strategy, request, response);

        try {
            final long cachedAt = getCachedAt();
            final long expiresOn = getExpiresOn(response);
            final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());
            final ClientInfo clientInfo = new ClientInfo(response.getClientInfo());

            final AccessToken accessToken = new AccessToken();
            // Required fields
            accessToken.setCredentialType(CredentialType.AccessToken.name());
            accessToken.setHomeAccountId(SchemaUtil.getHomeAccountId(clientInfo));
            accessToken.setRealm(getRealm(strategy, response));
            accessToken.setEnvironment(SchemaUtil.getEnvironment(msIdToken));
            accessToken.setClientId(request.getClientId());
            accessToken.setTarget(StringUtil.convertSetToString(request.getScope(), " "));
            accessToken.setCachedAt(String.valueOf(cachedAt)); // generated @ client side
            accessToken.setExpiresOn(String.valueOf(expiresOn));
            accessToken.setSecret(response.getAccessToken());

            // Optional fields
            accessToken.setExtendedExpiresOn(getExtendedExpiresOn(strategy, response));
            accessToken.setAuthority(request.getAuthority().toString());
            accessToken.setClientInfo(response.getClientInfo());
            accessToken.setAccessTokenType(BEARER); // TODO Don't hardcode this value.

            Logger.exiting(TAG, methodName, accessToken);

            return accessToken;
        } catch (ServiceException e) {
            // TODO handle this properly
            throw new RuntimeException(e);
        }
    }

    @Override
    public RefreshToken createRefreshToken(
            final MicrosoftStsOAuth2Strategy strategy,
            final MicrosoftStsAuthorizationRequest request,
            final MicrosoftStsTokenResponse response) {
        final String methodName = "createRefreshToken";
        Logger.entering(TAG, methodName, strategy, request, response);

        try {
            final long cachedAt = getCachedAt();
            final long expiresOn = getExpiresOn(response);
            final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());
            final ClientInfo clientInfo = new ClientInfo(response.getClientInfo());

            final RefreshToken refreshToken = new RefreshToken();
            // Required
            refreshToken.setCredentialType(CredentialType.RefreshToken.name());
            refreshToken.setEnvironment(SchemaUtil.getEnvironment(msIdToken));
            refreshToken.setHomeAccountId(SchemaUtil.getHomeAccountId(clientInfo));
            refreshToken.setClientId(response.getClientId());
            refreshToken.setSecret(response.getRefreshToken());

            // Optional
            refreshToken.setFamilyId(response.getFamilyId());
            refreshToken.setUsername(getUsername(response));
            //TODO the scope in MSAL is a set of String.
            refreshToken.setTarget(StringUtil.convertSetToString(request.getScope(), " "));
            refreshToken.setClientInfo(response.getClientInfo());

            // TODO are these needed? Expected?
            refreshToken.setCachedAt(String.valueOf(cachedAt)); // generated @ client side
            refreshToken.setExpiresOn(String.valueOf(expiresOn)); // derived from expires_in

            Logger.exiting(TAG, methodName, refreshToken);

            return refreshToken;
        } catch (ServiceException e) {
            // TODO handle this properly
            throw new RuntimeException(e);
        }
    }

    @Override
    public IdToken createIdToken(
            final MicrosoftStsOAuth2Strategy strategy,
            final MicrosoftStsAuthorizationRequest request,
            final MicrosoftStsTokenResponse response) {
        final String methodName = "createIdToken";
        Logger.entering(TAG, methodName, strategy, request, response);

        try {
            final ClientInfo clientInfo = new ClientInfo(response.getClientInfo());
            final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());

            final IdToken idToken = new IdToken();
            // Required fields
            idToken.setHomeAccountId(SchemaUtil.getHomeAccountId(clientInfo));
            idToken.setEnvironment(SchemaUtil.getEnvironment(msIdToken));
            idToken.setRealm(getRealm(strategy, response));
            idToken.setCredentialType(CredentialType.IdToken.name());
            idToken.setClientId(request.getClientId());
            idToken.setSecret(response.getIdToken());

            // Optional fields
            idToken.setAuthority(request.getAuthority().toString());

            Logger.exiting(TAG, methodName, idToken);

            return idToken;
        } catch (ServiceException e) {
            // TODO handle this properly
            throw new RuntimeException(e);
        }
    }

    @Override
    public RefreshToken asRefreshToken(com.microsoft.identity.common.internal.providers.oauth2.RefreshToken refreshTokenIn) {
        final String methodName = "asRefreshToken";
        Logger.entering(TAG, methodName, refreshTokenIn);

        final RefreshToken refreshTokenOut = new RefreshToken();

        // Required fields
        refreshTokenOut.setHomeAccountId(refreshTokenIn.getHomeAccountId());
        refreshTokenOut.setEnvironment(refreshTokenIn.getEnvironment());
        refreshTokenOut.setCredentialType(CredentialType.RefreshToken.name());
        refreshTokenOut.setClientId(refreshTokenIn.getClientId());
        refreshTokenOut.setSecret(refreshTokenIn.getSecret());

        // Optional fields
        refreshTokenOut.setTarget(refreshTokenIn.getTarget());
        refreshTokenOut.setCachedAt(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        refreshTokenOut.setExpiresOn(refreshTokenIn.getExpiresOn());
        //refreshTokenOut.setClientInfo(""); TODO OK to drop?
        refreshTokenOut.setFamilyId(refreshTokenIn.getFamilyId());
        //refreshTokenOut.setUsername(""); TODO OK to drop?

        if (!StringExtensions.isNullOrBlank(refreshTokenIn.getFamilyId())) {
            String familyId = refreshTokenIn.getFamilyId();
            // It is a foci token, replace the client and [possibly] prepend "foci-"
            if (!familyId.startsWith(FOCI_PREFIX)) {
                familyId = FOCI_PREFIX + familyId;
            }

            refreshTokenOut.setClientId(familyId);
        }

        Logger.exiting(TAG, methodName, refreshTokenOut);

        return refreshTokenOut;
    }

    @Override
    public Account asAccount(MicrosoftAccount account) {
        final String methodName = "asAccount";
        Logger.entering(TAG, methodName, account);

        Account acct = new Account(account);

        Logger.exiting(TAG, methodName, acct);

        return acct;
    }

    @Override
    public IdToken asIdToken(MicrosoftAccount msAccount, com.microsoft.identity.common.internal.providers.oauth2.RefreshToken refreshToken) {
        final String methodName = "asIdToken";
        Logger.entering(TAG, methodName, msAccount, refreshToken);

        final long cachedAt = getCachedAt();
        IDToken msIdToken = msAccount.getIDToken();

        final IdToken idToken = new IdToken();
        // Required fields
        idToken.setHomeAccountId(refreshToken.getHomeAccountId());
        idToken.setEnvironment(refreshToken.getEnvironment());
        idToken.setRealm(msAccount.getRealm());
        idToken.setCredentialType(CredentialType.IdToken.name());
        idToken.setClientId(refreshToken.getClientId());
        idToken.setSecret(msIdToken.getRawIDToken());
        idToken.setCachedAt(String.valueOf(cachedAt));

        // Optional fields
        idToken.setAuthority(SchemaUtil.getAuthority(msIdToken));

        Logger.exiting(TAG, methodName, idToken);

        return idToken;
    }

    private String getExtendedExpiresOn(final OAuth2Strategy strategy, final TokenResponse response) {
        final String methodName = "getExtendedExpiresOn";
        Logger.entering(TAG, methodName, strategy, response);

        // TODO It doesn't look like the v2 endpoint supports extended_expires_on claims
        // Is this true?
        String result = null;

        Logger.exiting(TAG, methodName, result);

        return result;
    }

    private String getRealm(final MicrosoftStsOAuth2Strategy msStrategy, final MicrosoftStsTokenResponse msTokenResponse) {
        final String methodName = "getRealm";
        Logger.entering(TAG, methodName, msStrategy, msTokenResponse);

        final MicrosoftStsAccount msAccount = msStrategy.createAccount(msTokenResponse);

        Logger.exiting(TAG, methodName, msAccount.getRealm());

        return msAccount.getRealm();
    }

    private String getUsername(final TokenResponse response) {
        final String methodName = "getUsername";
        Logger.entering(TAG, methodName, response);

        try {
            final MicrosoftIdToken msIdToken = new MicrosoftIdToken(response.getIdToken());
            final Map<String, String> tokenClaims = msIdToken.getTokenClaims();
            final String username = tokenClaims.get(PREFERRED_USERNAME);

            Logger.exiting(TAG, methodName, username);

            return username;
        } catch (ServiceException e) {
            // TODO handle this properly
            throw new RuntimeException(e);
        }
    }

    private long getCachedAt() {
        final String methodName = "getCachedAt";
        Logger.entering(TAG, methodName);

        final long currentTimeMillis = System.currentTimeMillis();
        final long cachedAt = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis);

        Logger.exiting(TAG, methodName, cachedAt);

        return cachedAt;
    }

    private long getExpiresOn(final MicrosoftStsTokenResponse msTokenResponse) {
        final String methodName = "getExpiresOn";
        Logger.entering(TAG, methodName, msTokenResponse);

        // Should be seconds since 1970
        final long millisSince1970 = msTokenResponse.getExpiresOn().getTime();
        final long secondsSince1970 = TimeUnit.MILLISECONDS.toSeconds(millisSince1970);

        Logger.exiting(TAG, methodName, secondsSince1970);

        return secondsSince1970;
    }

}
