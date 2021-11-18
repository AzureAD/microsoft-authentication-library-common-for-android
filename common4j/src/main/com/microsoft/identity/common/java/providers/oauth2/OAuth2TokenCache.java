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
package com.microsoft.identity.common.java.providers.oauth2;

import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.cache.AccountDeletionRecord;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;

import com.microsoft.identity.common.java.exception.ClientException;

import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Class for managing the tokens saved locally on a device.
 */
// Suppressing rawtype warnings due to the generic type OAuth2Strategy and AuthorizationRequest
@SuppressWarnings(WarningType.rawtype_warning)
@AllArgsConstructor
public abstract class OAuth2TokenCache
        <T extends OAuth2Strategy, U extends AuthorizationRequest, V extends TokenResponse> {

    public static final String ERR_UNSUPPORTED_OPERATION = "This method is unsupported.";
    private final @NonNull IPlatformComponents mPlatformComponents;

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
     * Saves the credentials and tokens returned by the service to the cache and returns all of
     * the AccountRecords associated to the target home_account_id for the target client_id and
     * environment. Please note, only the ICacheRecord associated to *this* save() action is
     * fully-formed (returning both the access_token and refresh_token in the payload),
     * all other ICacheRecords in the result will be sparse, containing only the AccountRecord
     * and IdTokenRecord.
     *
     * @param oAuth2Strategy The strategy used to create the token request.
     * @param request        The request used to acquire tokens and credentials.
     * @param response       The response received from the IdP/STS.
     * @return A {@link List} of {@link ICacheRecord} associated with this principal, client_id,
     * and environment.
     * @throws ClientException If tokens cannot be successfully saved.
     */
    public abstract List<ICacheRecord> saveAndLoadAggregatedAccountData(
            final T oAuth2Strategy,
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
     * Loads the tokens for the supplied Account into the result {@link ICacheRecord}.
     *
     * @param clientId The ClientId of the current app. (Logical Identifier)
     * @param applicationIdentifier An optional physical identifier (Android: PackageName/Signature)
     * @param target   The 'target' (scopes) the requested token should contain.
     * @param account  The Account whose Credentials should be loaded.
     * @return The resulting ICacheRecord. Entries may be empty if not present in the cache.
     */
    public abstract ICacheRecord load(
            final String clientId,
            final String applicationIdentifier,
            final String target,
            final AccountRecord account,
            final AbstractAuthenticationScheme authScheme
    );

    /**
     * Loads the tokens for the supplied Account into the result {@link ICacheRecord} - this will
     * be the first element in the result List. Subsequent ICacheRecords are sparse records for
     * other authorized tenants.
     *
     * @param clientId The ClientId of the current app. (Logical Identifier)
     * @param applicationIdentifier An optional physical identifier (Android: PackageName/Signature)
     * @param target   The 'target' (scopes) the requested token should contain.
     * @param account  The Account whose Credentials should be loaded.
     * @return The resulting ICacheRecord. Entries may be empty if not present in the cache.
     */
    public abstract List<ICacheRecord> loadWithAggregatedAccountData(
            final String clientId,
            final String applicationIdentifier,
            final String target,
            final AccountRecord account,
            final AbstractAuthenticationScheme authenticationScheme
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
     * Returns sparse ICacheRecords (containing only AccountRecord + IdTokenRecord) based on the
     * supplied criteria.
     *
     * @param environment   The environment to which the sought AccountRecord is associated. // TODO update javadoc
     * @param clientId      The clientId to which the sought AccountRecord is associated.
     * @param homeAccountId The homeAccountId of the sought AccountRecord.
     * @return An unmodifiable List of ICacheRecords matching the supplid criteria.
     */
    public abstract List<ICacheRecord> getAccountsWithAggregatedAccountData(final String environment,
                                                                            final String clientId,
                                                                            final String homeAccountId
    );

    /**
     * Returns the AccountRecord matching the supplied criteria.
     *
     * @param environment    The environment to which the sought IAccount is associated.
     * @param clientId       The clientId to which the sought IAccount is associated.
     * @param localAccountId The local account id of the targeted account.
     * @return The sought AccountRecord or null if it cannot be found.
     */
    public abstract AccountRecord getAccountByLocalAccountId(final String environment,
                                                             final String clientId,
                                                             final String localAccountId
    );

    /**
     * Returns the ICacheRecord matching the supplied criteria.
     *
     * @param environment    The environment to which the sought IAccount is associated.
     * @param clientId       The clientId to which the sought IAccount is associated.
     * @param localAccountId The local account id of the targeted account.
     * @return The sought AccountRecord or null if it cannot be found.
     */
    public abstract ICacheRecord getAccountWithAggregatedAccountDataByLocalAccountId(
            final String environment,
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
    public abstract List<AccountRecord> getAccounts(final String environment,
                                                    final String clientId
    );

    /**
     * For a provided {@link AccountRecord} and clientId, find other AccountRecords which share a
     * home_account_id and environment.
     * <p>
     * The originally provided AccountRecord will be provided in the result as the 0th element.
     *
     * @param clientId      The current application.
     * @param accountRecord The AccountRecord whose corollary AccountRecords should be loaded.
     * @return a list of all matching {@link AccountRecord}s.
     */
    public abstract List<AccountRecord> getAllTenantAccountsForAccountByClientId(final String clientId,
                                                                                 final AccountRecord accountRecord
    );

    /**
     * Gets an immutable List of ICacheRecords for this app which have RefreshTokens in the cache.
     * Please note, these records are sparse: no access_tokens or refresh_tokens will be returned.
     *
     * @param clientId    The current application.
     * @param environment The current environment.
     * @return An immutable List of ICacheRecords.
     */
    public abstract List<ICacheRecord> getAccountsWithAggregatedAccountData(
            final String environment,
            final String clientId
    );

    /**
     * Gets an immutable List of IdTokenRecords for the supplied AccountRecord.
     *
     * @param clientId      The client id of the app to query.
     * @param accountRecord The AccountRecord for which IdTokenRecords should be loaded.
     * @return An immutable List of IdTokenRecords.
     */
    public abstract List<IdTokenRecord> getIdTokensForAccountRecord(final String clientId,
                                                                    final AccountRecord accountRecord
    );

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
     * Removes the Account (and its associated Credentials) matching the supplied criteria.
     *
     * @param environment   The environment to which the targeted Account is associated.
     * @param clientId      The clientId of this current app.
     * @param homeAccountId The homeAccountId of the Account targeted for deletion.
     * @param realm         The tenant id of the targeted Account (if applicable).
     * @param typesToRemove The CredentialTypes to be deleted for this Account.
     * @return The {@link AccountDeletionRecord} containing the removed AccountRecords.
     */
    public abstract AccountDeletionRecord removeAccount(final String environment,
                                                        final String clientId,
                                                        final String homeAccountId,
                                                        final String realm,
                                                        final CredentialType... typesToRemove
    );

    /**
     * Removes all entries from the cache.
     */
    public abstract void clearAll();

    /**
     * Returns a Set of all of the ClientIds which have tokens stored in this cache.
     *
     * @return A Set of ClientIds.
     */
    protected abstract Set<String> getAllClientIds();

    /**
     * Gets the set of common components that this cache was created with.
     *
     * @return The set of common components that this cache was created with.
     */
    protected final IPlatformComponents getComponents() {
        return mPlatformComponents;
    }

    public abstract AccountRecord getAccountByHomeAccountId(@Nullable final String environment,
                                                            @NonNull final String clientId,
                                                            @NonNull final String homeAccountId
    );
}
