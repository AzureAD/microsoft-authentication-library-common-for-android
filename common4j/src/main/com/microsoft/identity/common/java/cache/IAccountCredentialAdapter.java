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
package com.microsoft.identity.common.java.cache;

import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.request.SdkType;

/**
 * Provides Adapters to the MsalOAuth2TokenCache.
 */
// Suppressing rawtype warnings due to the generic type OAuth2Strategy and AuthorizationRequest
@SuppressWarnings(WarningType.rawtype_warning)
public interface IAccountCredentialAdapter
        <T extends OAuth2Strategy,
                U extends AuthorizationRequest,
                V extends TokenResponse,
                W extends BaseAccount,
                X extends RefreshToken> {

    /**
     * Constructs an Account.
     *
     * @param strategy The strategy used to make the OAuth2 request.
     * @param request  The OAuth2 request.
     * @param response The authz response.
     * @return The derived Account.
     */
    AccountRecord createAccount(T strategy, U request, V response);

    /**
     * Constructs an AccessToken.
     *
     * @param strategy The strategy used to make the OAuth2 request.
     * @param request  The OAuth2 request.
     * @param response The authz response.
     * @return The derived AccessToken.
     */
    AccessTokenRecord createAccessToken(T strategy, U request, V response);

    /**
     * Constructs a RefreshToken.
     *
     * @param strategy The strategy used to make the OAuth2 request.
     * @param request  The OAuth2 request.
     * @param response The authz response.
     * @return The derived RefreshToken.
     */
    RefreshTokenRecord createRefreshToken(T strategy, U request, V response);

    /**
     * Constructs an IdToken.
     *
     * @param strategy The strategy used to make the OAuth2 request.
     * @param request  The OAuth2 request.
     * @param response The authz response.
     * @return The derived IdToken.
     */
    IdTokenRecord createIdToken(T strategy, U request, V response);

    /**
     * Adapter method to turn
     * {@link RefreshToken} instances into
     * {@link RefreshTokenRecord}.
     *
     * @param refreshToken The RefreshToken to adapt.
     * @return The adapted RefreshToken.
     */
    RefreshTokenRecord asRefreshToken(X refreshToken);

    /**
     * Adapter method to turn {@link BaseAccount} instances into
     * {@link AccountRecord} instances.
     *
     * @param account The Account to adapt.
     * @return The adapted Account.
     */
    AccountRecord asAccount(W account);

    /**
     * Constructs IdToken instances from {@link BaseAccount} and
     * {@link RefreshToken} instances.
     *
     * @param account      The Account to read.
     * @param refreshToken The RefreshToken to read.
     * @return The newly constructed IdToken.
     */
    IdTokenRecord asIdToken(W account, X refreshToken);

    /**
     * Constructs an Account.
     *
     * @param parameters Token parameters for the OAuth2 request
     * @param sdkType  {@link SdkType}
     * @param response The authz response.
     * @return The derived Account.
     */
    AccountRecord createAccountRecord(TokenCommandParameters parameters, SdkType sdkType, V response) throws ServiceException;

    /**
     * Constructs an AccessTokenRecord from request parameters, account record and
     * authz response
     * @param parameters Request parameters
     * @param accountRecord The account record
     * @param response Token response
     * @return
     */
    AccessTokenRecord createAccessTokenRecord(TokenCommandParameters parameters, AccountRecord accountRecord, V response) throws ClientException;

    /**
     * Constructs an {@link RefreshTokenRecord} from request parameters, account record and
     * authz response
     * @param parameters Request parameters
     * @param accountRecord The account record
     * @param response Token response
     * @return
     */
    RefreshTokenRecord createRefreshTokenRecord(TokenCommandParameters parameters, AccountRecord accountRecord, V response);

    /**
     * Constructs an {@link IdTokenRecord} from request parameters, account record and
     * authz response
     * @param parameters Request parameters
     * @param accountRecord The account record
     * @param response Token response
     * @return
     */
    IdTokenRecord createIdTokenRecord(TokenCommandParameters parameters, AccountRecord accountRecord, V response);
}
