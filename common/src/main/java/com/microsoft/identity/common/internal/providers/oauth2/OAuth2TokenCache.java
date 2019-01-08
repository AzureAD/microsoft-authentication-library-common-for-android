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
import com.microsoft.identity.common.internal.cache.AccountDeletionRecord;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;

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
     * @return The {@link ICacheRecord} containing the Account + Credentials saved to the cache.
     * @throws ClientException If tokens cannot be successfully saved.
     */
    public abstract ICacheRecord save(final T oAuth2Strategy,
                                      final U request,
                                      final V response) throws ClientException;

    /**
     * Saves the supplied Account and Credential in the cache.
     *
     * @param accountRecord The AccountRecord to save.
     * @param idTokenRecord The IdTokenRecord to save.
     * @return The {@link ICacheRecord} containing the Account + Credential[s] saved to the cache.
     */
    public abstract ICacheRecord save(final AccountRecord accountRecord,
                                      final IdTokenRecord idTokenRecord
    );

    /**
     * Loads the tokens available for the supplied client criteria.
     *
     * @param clientId      The current client's id.
     * @param target        The desired scopes.
     * @param accountRecord The current account.
     * @return An ICacheRecord containing the account. If a matching id token is available
     * (for the provided clientId), it is returned. If a matching access token is available
     * (for the provided client id), it is also returned. If a matching refresh token is available
     * it is returned.
     */
    public abstract ICacheRecord loadByFamilyId(final String clientId,
                                                final String target,
                                                final AccountRecord accountRecord
    );

    /**
     * Loads the tokens for the supplied Account into the result {@link ICacheRecord}.
     *
     * @param clientId The ClientId of the current app.
     * @param target   The 'target' (scopes) the requested token should contain.
     * @param account  The Account whose Credentials should be loaded.
     * @return The resulting ICacheRecord. Entries may be empty if not present in the cache.
     */
    public abstract ICacheRecord load(
            final String clientId,
            final String target,
            final AccountRecord account
    );

    /**
     * Removes the supplied Credential from the cache.
     *
     * @param credential The Credential to remove.
     * @return True, if the Credential was removed. False otherwise.
     */
    public abstract boolean removeCredential(final Credential credential);

    /**
     * Returns the AccountRecord matching the supplied criteria.
     *
     * @param environment   The environment to which the sought AccountRecord is associated.
     * @param clientId      The clientId to which the sought AccountRecord is associated.
     * @param homeAccountId The homeAccountId of the sought AccountRecord.
     * @param realm         The tenant id of the targeted account (if applicable).
     * @return The sought AccountRecord or null if it cannot be found.
     */
    public abstract AccountRecord getAccount(final String environment,
                                             final String clientId,
                                             final String homeAccountId,
                                             final String realm
    );

    /**
     * Returns the AccountRecord matching the supplied criteria.
     *
     * @param environment    The environment to which the sought IAccount is associated.
     * @param clientId       The clientId to which the sought IAccount is associated.
     * @param localAccountId The local account id of the targeted account.
     * @return The sought AccountRecord or null if it cannot be found.
     */
    public abstract AccountRecord getAccountWithLocalAccountId(final String environment,
                                                               final String clientId,
                                                               final String localAccountId
    );

    /**
     * Gets an immutable List of AccountRecords for this app which have RefreshTokens in the cache.
     *
     * @param clientId    The current application.
     * @param environment The current environment.
     * @return An immutable List of AccountRecords.
     */
    public abstract List<AccountRecord> getAccounts(final String environment, final String clientId);

    /**
     * Removes the Account (and its associated Credentials) matching the supplied criteria.
     *
     * @param environment   The environment to which the targeted Account is associated.
     * @param clientId      The clientId of this current app.
     * @param homeAccountId The homeAccountId of the Account targeted for deletion.
     * @param realm         The tenant id of the targeted Account (if applicable).
     * @return The {@link AccountDeletionRecord} containing the removed AccountRecords.
     */
    public abstract AccountDeletionRecord removeAccount(final String environment,
                                                        final String clientId,
                                                        final String homeAccountId,
                                                        final String realm
    );

    /**
     * Gets the Context used to initialize this OAuth2TokenCache.
     *
     * @return The Context.
     */
    protected final Context getContext() {
        return mContext;
    }
}
