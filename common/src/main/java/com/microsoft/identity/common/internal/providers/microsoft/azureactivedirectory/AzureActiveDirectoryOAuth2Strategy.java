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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

/**
 * The Azure Active Directory oAuth2 Strategy
 */
public class AzureActiveDirectoryOAuth2Strategy
        extends OAuth2Strategy<
        AzureActiveDirectoryAccessToken,
        AzureActiveDirectoryAccount,
        AzureActiveDirectoryAuthorizationRequest,
        AuthorizationResponse,
        AuthorizationStrategy,
        AzureActiveDirectoryOAuth2Configuration,
        AzureActiveDirectoryRefreshToken,
        AzureActiveDirectoryTokenRequest,
        AzureActiveDirectoryTokenResponse,
        TokenResult> {

    private AzureActiveDirectoryOAuth2Configuration mConfig = null;

    public AzureActiveDirectoryOAuth2Strategy(final AzureActiveDirectoryOAuth2Configuration config) {
        super(config);
        mTokenEndpoint = "https://login.microsoftonline.com/microsoft.com/oauth2/token";
        mConfig = config;
    }

    @Override
    public String getIssuerCacheIdentifier(final AzureActiveDirectoryAuthorizationRequest request) {
        AzureActiveDirectoryAuthorizationRequest authRequest;
        authRequest = request;
        AzureActiveDirectoryCloud cloud = AzureActiveDirectory.getAzureActiveDirectoryCloud(authRequest.getAuthority());

        if (!cloud.isValidated() && this.mConfig.isAuthorityHostValdiationEnabled()) {
            //We have invalid cloud data... and authority host validation is enabled....
            //TODO: Throw an exception in this case... need to see what ADAL does in this case.
        }

        if (!cloud.isValidated() && !this.mConfig.isAuthorityHostValdiationEnabled()) {
            //Authority host validation not specified... but there is no cloud....
            //Hence just return the passed in Authority
            return authRequest.getAuthority().toString();
        }

        Uri authorityUri = Uri.parse(authRequest.getAuthority().toString())
                .buildUpon()
                .authority(cloud.getPreferredCacheHostName())
                .build();

        return authorityUri.toString();
    }

    @Override
    public AzureActiveDirectoryAccessToken getAccessTokenFromResponse(
            @NonNull final AzureActiveDirectoryTokenResponse response) {
        return new AzureActiveDirectoryAccessToken(response);
    }

    @Override
    public AzureActiveDirectoryRefreshToken getRefreshTokenFromResponse(
            @NonNull final AzureActiveDirectoryTokenResponse response) {
        return new AzureActiveDirectoryRefreshToken(response);
    }

    /**
     * Stubbed out for now, but should create a new AzureActiveDirectory account
     * Should accept a parameter (TokenResponse) for producing that user
     *
     * @return
     */
    @Override
    public AzureActiveDirectoryAccount createAccount(
            @NonNull final AzureActiveDirectoryTokenResponse response) {
        IDToken idToken = null;
        ClientInfo clientInfo = null;
        try {
            idToken = new IDToken(response.getIdToken());
            clientInfo = new ClientInfo(response.getClientInfo());
        } catch (ServiceException ccse) {
            // TODO: Add a log here
            // TODO: Should we bail?
        }
        return AzureActiveDirectoryAccount.create(idToken, clientInfo);
    }

    @Override
    protected void validateAuthorizationRequest(final AzureActiveDirectoryAuthorizationRequest request) {
        // TODO
    }

    /**
     * validate the contents of the token request... all the base class is currently abstract
     * some of the validation for required parameters for the protocol could be there...
     *
     * @param request
     */
    @Override
    protected void validateTokenRequest(final AzureActiveDirectoryTokenRequest request) {
        // TODO
    }

    @Override
    protected TokenResult getTokenResultFromHttpResponse(final HttpResponse response) {
        TokenResponse tokenResponse = null;
        TokenErrorResponse tokenErrorResponse = null;

        if (response.getStatusCode() >= 400) {
            //An error occurred
            tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), MicrosoftTokenErrorResponse.class);
        } else {
            tokenResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), AzureActiveDirectoryTokenResponse.class);
        }

        return new TokenResult(tokenResponse, tokenErrorResponse);
    }

}
