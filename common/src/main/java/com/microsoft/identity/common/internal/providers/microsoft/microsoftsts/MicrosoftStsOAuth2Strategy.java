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

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MicrosoftStsOAuth2Strategy
        extends OAuth2Strategy
        <MicrosoftStsAccessToken,
                MicrosoftStsAccount,
                MicrosoftStsAuthorizationRequest,
                AuthorizationResponse,
                AuthorizationStrategy,
                MicrosoftStsOAuth2Configuration,
                MicrosoftStsRefreshToken,
                TokenRequest,
                MicrosoftStsTokenResponse,
                TokenResult> {

    private MicrosoftStsOAuth2Configuration mConfig;

    public MicrosoftStsOAuth2Strategy(MicrosoftStsOAuth2Configuration config) {
        super(config);
        mConfig = config;
        mTokenEndpoint = "https://login.microsoftonline.com/microsoft.com/oAuth2/v2.0/token";
    }

    @Override
    public String getIssuerCacheIdentifier(MicrosoftStsAuthorizationRequest request) {
        final URL authority = ((MicrosoftStsAuthorizationRequest) request).getAuthority();
        // TODO I don't think this is right... This is probably not the correct authority cache to consult...
        final AzureActiveDirectoryCloud cloudEnv = AzureActiveDirectory.getAzureActiveDirectoryCloud(authority);
        // This map can only be consulted if authority validation is on.
        // If the host has a hardcoded trust, we can just use the hostname.
        if (null != cloudEnv) {
            return cloudEnv.getPreferredNetworkHostName();
        }
        return authority.getHost();
    }

    @Override
    public MicrosoftStsAccessToken getAccessTokenFromResponse(MicrosoftStsTokenResponse response) {
        return new MicrosoftStsAccessToken(response);
    }

    @Override
    public MicrosoftStsRefreshToken getRefreshTokenFromResponse(MicrosoftStsTokenResponse response) {
        return new MicrosoftStsRefreshToken(response);
    }

    @Override
    public MicrosoftStsAccount createAccount(MicrosoftStsTokenResponse response) {
        IDToken idToken = null;
        ClientInfo clientInfo = null;
        try {
            idToken = new IDToken(response.getIdToken());
            clientInfo = new ClientInfo(response.getClientInfo());
        } catch (ServiceException ccse) {
            // TODO: Add a log here
            // TODO: Should we bail?
        }

        return MicrosoftStsAccount.create(idToken, clientInfo);
    }

    @Override
    protected void validateAuthorizationRequest(MicrosoftStsAuthorizationRequest request) {
        // TODO implement

    }

    @Override
    protected void validateTokenRequest(TokenRequest request) {
        // TODO implement
    }

    /**
     * This function is only used to check if the {@link HttpResponse#mResponseBody} is in the right JSON format.
     *
     * There are two kinds of token response in the {@link HttpResponse#mResponseBody} in AAD v2.0,
     * successful response and error response.
     *
     * The successful response is JSON string containing
     *             access_token
     *             token_type
     *             expires_in
     *             scope
     *             refresh_token: only provided if `offline_access` scope was requested.
     *             id_token: an unsigned JSON Web Token (JWT). Only provided if `openid` scope was requested.
     *
     * The token issuance endpoint errors are HTTP error codes, because the client calls the token
     * issuance endpoint directly. In addition to the HTTP status code, the Azure AD token issuance
     * endpoint also returns a JSON document with objects that describe the error.
     *             error
     *             error_description in JSON format
     *             error_codes
     *             timestamp
     *             trace_id
     *             correlation_id
     *
     * @param response
     */
    @Override
    protected void validateTokenResponse(final HttpResponse response) throws ServiceException{
        if (null == response) {
            throw new IllegalArgumentException("The http response is null.");
        }

        if(StringUtil.isEmpty(response.getBody())) {
            throw new IllegalArgumentException("The http response body is null.");
        }

        StringUtil.validateJsonFormat(response.getBody(), null);

        final Map<String, String> responseItems = new HashMap<>();
        StringUtil.extractJsonObjects(responseItems, response.getBody());

        if (responseItems.containsKey(AuthenticationConstants.OAuth2.ERROR)
                && responseItems.containsKey(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION)) {
            StringUtil.validateJsonFormat(responseItems.get(AuthenticationConstants.OAuth2.ERROR_DESCRIPTION), null);
        }

        if (responseItems.containsKey(AuthenticationConstants.OAuth2.ACCESS_TOKEN)
                && responseItems.containsKey(AuthenticationConstants.OAuth2.ID_TOKEN)) {
            StringUtil.validateJWTFormat(responseItems.get(AuthenticationConstants.OAuth2.ID_TOKEN));
        }
    }

    @Override
    protected TokenResult getTokenResultFromHttpResponse(HttpResponse response) {
        TokenResponse tokenResponse = null;
        TokenErrorResponse tokenErrorResponse = null;

        if (response.getStatusCode() >= 400) {
            //An error occurred
            tokenErrorResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), MicrosoftTokenErrorResponse.class);
        } else {
            tokenResponse = ObjectMapper.deserializeJsonStringToObject(response.getBody(), MicrosoftTokenResponse.class);
        }

        return new TokenResult(tokenResponse, tokenErrorResponse);
    }
}
