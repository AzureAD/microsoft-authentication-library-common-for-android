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
package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Context;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.ISaveTokenResult;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;

import java.util.List;

/**
 * Class for managing the tokens saved locally on a device.
 */
public abstract class OAuth2TokenCache
        <T extends OAuth2Strategy, U extends AuthorizationRequest, V extends TokenResponse> {

    private final Context mContext;

    /**
     * Constructs a new OAuth2TokenCache.
     *
     * @param context The Application Context of the consuming app.
     */
    public OAuth2TokenCache(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Saves the credentials and tokens returned by the service to the cache.
     *
     * @param oAuth2Strategy The strategy used to create the token request.
     * @param request        The request used to acquire tokens and credentials.
     * @param response       The response received from the IdP/STS.
     * @return The {@link ISaveTokenResult} containing the Account + Credentials saved to the cache.
     * @throws ClientException If tokens cannot be successfully saved.
     */
    public abstract ISaveTokenResult saveTokens(final T oAuth2Strategy,
                                                final U request,
                                                final V response) throws ClientException;

    /**
     * Loads the tokens for the supplied Account into the result {@link ISaveTokenResult}.
     *
     * @param clientId The ClientId of the current app.
     * @param account  The Account whose Credentials should be loaded.
     * @return The resulting ISaveTokenResult. Entries may be empty if not present in the cache.
     */
    public abstract ISaveTokenResult loadTokens(final String clientId, final Account account);

    /**
     * Removes the supplied Credential from the cache.
     *
     * @param credential The Credential to remove.
     * @return True, if the Credential was removed. False otherwise.
     */
    public abstract boolean removeCredential(final Credential credential);

    /**
     * Returns the IAccount matching the supplied criteria.
     *
     * @param environment   The environment to which the sought IAccount is associated.
     * @param clientId      The clientId to which the sought IAccouct is associated.
     * @param homeAccountId The homeAccountId of the sought IAccount.
     * @return The sought IAccount or null if it cannot be found.
     */
    public abstract Account getAccount(final String environment,
                                       final String clientId,
                                       final String homeAccountId
    );

    /**
     * Gets an immutable List of IAccounts for this app which have RefreshTokens in the cache.
     *
     * @param clientId    The current application.
     * @param environment The current environment.
     * @return An immutable List of IAccounts.
     */
    public abstract List<Account> getAccounts(final String environment, final String clientId);

    /**
     * Removes the Account (and its associated Credentials) matching the supplied criteria.
     *
     * @param environment   The environment to which the targeted Account is associated.
     * @param clientId      The clientId of this current app.
     * @param homeAccountId The homeAccountId of the Account targeted for deletion.
     * @return True, if the Account was deleted. False otherwise.
     */
    public abstract boolean removeAccount(final String environment,
                                          final String clientId,
                                          final String homeAccountId);

    /**
     * Gets the Context used to initialize this OAuth2TokenCache.
     *
     * @return The Context.
     */
    protected final Context getContext() {
        return mContext;
    }
}
