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

import static com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal.SCHEME_BEARER;
import static com.microsoft.identity.common.java.dto.CredentialType.ID_TOKEN_TYPES;
import static com.microsoft.identity.common.java.dto.CredentialType.IdToken;
import static com.microsoft.identity.common.java.dto.CredentialType.RefreshToken;
import static com.microsoft.identity.common.java.dto.CredentialType.V1IdToken;
import static com.microsoft.identity.common.java.exception.ErrorStrings.ACCOUNT_IS_SCHEMA_NONCOMPLIANT;
import static com.microsoft.identity.common.java.exception.ErrorStrings.CREDENTIAL_IS_SCHEMA_NONCOMPLIANT;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeWithClientKeyInternal;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.java.telemetry.Telemetry;
import com.microsoft.identity.common.java.telemetry.events.CacheEndEvent;
import com.microsoft.identity.common.java.telemetry.events.CacheStartEvent;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

// Suppressing rawtype warnings due to the generic type OAuth2Strategy and AuthorizationRequest
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", WarningType.rawtype_warning})
public class MsalOAuth2TokenCache
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends TokenResponse,
                GenericAccount extends BaseAccount,
                GenericRefreshToken extends com.microsoft.identity.common.java.providers.oauth2.RefreshToken>
        extends OAuth2TokenCache<GenericOAuth2Strategy, GenericAuthorizationRequest, GenericTokenResponse>
        implements IShareSingleSignOnState<GenericAccount, GenericRefreshToken> {

    private static final String TAG = MsalOAuth2TokenCache.class.getSimpleName();
    private static final Object sCacheLock = new Object();

    private IAccountCredentialCache mAccountCredentialCache;

    private final IAccountCredentialAdapter<
            GenericOAuth2Strategy,
            GenericAuthorizationRequest,
            GenericTokenResponse,
            GenericAccount,
            GenericRefreshToken> mAccountCredentialAdapter;

    /**
     * Constructor of MsalOAuth2TokenCache.
     *
     * @param commonComponents         {@link IPlatformComponents}
     * @param accountCredentialCache   IAccountCredentialCache
     * @param accountCredentialAdapter IAccountCredentialAdapter
     */
    public MsalOAuth2TokenCache(final IPlatformComponents commonComponents,
                                final IAccountCredentialCache accountCredentialCache,
                                final IAccountCredentialAdapter<
                                        GenericOAuth2Strategy,
                                        GenericAuthorizationRequest,
                                        GenericTokenResponse,
                                        GenericAccount,
                                        GenericRefreshToken> accountCredentialAdapter) {
        super(commonComponents);
        Logger.verbose(TAG, "Init: " + TAG);
        mAccountCredentialCache = accountCredentialCache;
        mAccountCredentialAdapter = accountCredentialAdapter;
    }

    /**
     * Factory method for creating an instance of MsalOAuth2TokenCache
     * <p>
     * NOTE: Currently this is configured for AAD v2 as the only IDP
     *
     * @param components The platform components
     * @return An instance of the MsalOAuth2TokenCache.
     */
    public static MsalOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> create(@NonNull final IPlatformComponents components) {
        final String methodName = ":create";

        Logger.verbose(
                TAG + methodName,
                "Creating MsalOAuth2TokenCache"
        );

        // Init the new-schema cache
        final ICacheKeyValueDelegate cacheKeyValueDelegate = new CacheKeyValueDelegate();
        final INameValueStorage<String> sharedPreferencesFileManager =
                components.getEncryptedNameValueStore(
                        SharedPreferencesAccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                        components.getStorageEncryptionManager(),
                        String.class
                );
        final IAccountCredentialCache accountCredentialCache =
                new SharedPreferencesAccountCredentialCache(
                        cacheKeyValueDelegate,
                        sharedPreferencesFileManager
                );
        final MicrosoftStsAccountCredentialAdapter accountCredentialAdapter =
                new MicrosoftStsAccountCredentialAdapter();

        return new MsalOAuth2TokenCache<>(
                components,
                accountCredentialCache,
                accountCredentialAdapter
        );
    }


    void validateNonNull(@Nullable final Object object,
                         @NonNull final String type) throws ClientException {
        final String message = type + " passed in is Null";

        if (object == null) {
            Logger.warn(TAG, message);
            throw new ClientException(message);
        }
    }

    /**
     * @param accountRecord     The {@link AccountRecord} to store.
     * @param idTokenRecord     The {@link IdTokenRecord} to store.
     * @param accessTokenRecord The {@link AccessTokenRecord} to store.
     * @return The {@link ICacheRecord} result of this save action.
     * @throws ClientException If the supplied Accounts or Credentials are schema invalid.
     * @see OAuth2TokenCache#save(AccountRecord, IdTokenRecord)
     */
    @Deprecated
    ICacheRecord save(@NonNull AccountRecord accountRecord,
                      @NonNull IdTokenRecord idTokenRecord,
                      @NonNull AccessTokenRecord accessTokenRecord) throws ClientException {
        final String methodName = ":save (3 arg)";

        // Validate the supplied Accounts/Credentials
        final boolean isAccountValid = isAccountSchemaCompliant(accountRecord);
        final boolean isIdTokenValid = isIdTokenSchemaCompliant(idTokenRecord);
        final boolean isAccessTokenValid = isAccessTokenSchemaCompliant(accessTokenRecord);

        if (!isAccountValid) {
            throw new ClientException(ACCOUNT_IS_SCHEMA_NONCOMPLIANT);
        }

        if (!isIdTokenValid) {
            throw new ClientException(CREDENTIAL_IS_SCHEMA_NONCOMPLIANT, "[(ID)]");
        }

        if (!isAccessTokenValid) {
            throw new ClientException(CREDENTIAL_IS_SCHEMA_NONCOMPLIANT, "[(AT)]");
        }

        Logger.verbose(
                TAG + methodName,
                "Accounts/Credentials are valid.... proceeding"
        );

        saveAccounts(accountRecord);
        saveCredentialsInternal(idTokenRecord, accessTokenRecord);

        final CacheRecord.CacheRecordBuilder result = CacheRecord.builder();
        result.account(accountRecord);
        result.accessToken(accessTokenRecord);

        if (CredentialType.V1IdToken.name().equalsIgnoreCase(idTokenRecord.getCredentialType())) {
            result.v1IdToken(idTokenRecord);
        } else {
            result.idToken(idTokenRecord);
        }

        return result.build();
    }

//    ICacheRecord save(final @NonNull AccountRecord accountRecord,
//                      final @NonNull IdTokenRecord idTokenRecord,
//                      final @NonNull AccessTokenRecord accessTokenRecord,
//                      final @Nullable RefreshTokenRecord refreshTokenRecord,
//                      boolean isNAARequest) throws ClientException {
//        final String methodName = ":save (4 arg)";
//
//        // Validate the supplied Accounts/Credentials
//        if (!isAccountSchemaCompliant(accountRecord)) {
//            throw new ClientException(ACCOUNT_IS_SCHEMA_NONCOMPLIANT);
//        }
//
//        if (!isIdTokenSchemaCompliant(idTokenRecord)) {
//            throw new ClientException(CREDENTIAL_IS_SCHEMA_NONCOMPLIANT, "[(ID)]");
//        }
//
//        if (!isAccessTokenSchemaCompliant(accessTokenRecord)) {
//            throw new ClientException(CREDENTIAL_IS_SCHEMA_NONCOMPLIANT, "[(AT)]");
//        }
//
//        if (refreshTokenRecord != null && !isRefreshTokenSchemaCompliant(refreshTokenRecord)) {
//            throw new ClientException(CREDENTIAL_IS_SCHEMA_NONCOMPLIANT, "[(RT)]");
//        }
//
//        Logger.verbose(
//                TAG + methodName,
//                "Accounts/Credentials are valid.... proceeding"
//        );
//
//        saveAccounts(accountRecord);
//        saveCredentialsInternal(idTokenRecord, accessTokenRecord, refreshTokenRecord);
//
//        final CacheRecord.CacheRecordBuilder result = CacheRecord.builder();
//        result.account(accountRecord);
//        result.accessToken(accessTokenRecord);
//        result.refreshToken(refreshTokenRecord);
//
//        if (CredentialType.V1IdToken.name().equalsIgnoreCase(idTokenRecord.getCredentialType())) {
//            result.v1IdToken(idTokenRecord);
//        } else {
//            result.idToken(idTokenRecord);
//        }
//
//        return result.build();
//    }


    /**
     * @param accountRecord      The {@link AccountRecord} to store.
     * @param idTokenRecord      The {@link IdTokenRecord} to store.
     * @param accessTokenRecord  The {@link AccessTokenRecord} to store.
     * @param refreshTokenRecord The {@link RefreshTokenRecord} to store.
     * @return The {@link ICacheRecord} result of this save action.
     * @throws ClientException If the supplied Accounts or Credentials are schema invalid.
     * @see OAuth2TokenCache#save(AccountRecord, IdTokenRecord)
     */
    ICacheRecord save(final @NonNull AccountRecord accountRecord,
                      final @NonNull IdTokenRecord idTokenRecord,
                      final @NonNull AccessTokenRecord accessTokenRecord,
                      final @Nullable RefreshTokenRecord refreshTokenRecord) throws ClientException {
        final String methodName = ":save (4 arg)";

        // Validate the supplied Accounts/Credentials
        if (!isAccountSchemaCompliant(accountRecord)) {
            throw new ClientException(ACCOUNT_IS_SCHEMA_NONCOMPLIANT);
        }

        if (!isIdTokenSchemaCompliant(idTokenRecord)) {
            throw new ClientException(CREDENTIAL_IS_SCHEMA_NONCOMPLIANT, "[(ID)]");
        }

        if (!isAccessTokenSchemaCompliant(accessTokenRecord)) {
            throw new ClientException(CREDENTIAL_IS_SCHEMA_NONCOMPLIANT, "[(AT)]");
        }

        if (refreshTokenRecord != null && !isRefreshTokenSchemaCompliant(refreshTokenRecord)) {
            throw new ClientException(CREDENTIAL_IS_SCHEMA_NONCOMPLIANT, "[(RT)]");
        }

        Logger.verbose(
                TAG + methodName,
                "Accounts/Credentials are valid.... proceeding"
        );

        saveAccounts(accountRecord);
        saveCredentialsInternal(idTokenRecord, accessTokenRecord, refreshTokenRecord);

        final CacheRecord.CacheRecordBuilder result = CacheRecord.builder();
        result.account(accountRecord);
        result.accessToken(accessTokenRecord);
        result.refreshToken(refreshTokenRecord);

        if (CredentialType.V1IdToken.name().equalsIgnoreCase(idTokenRecord.getCredentialType())) {
            result.v1IdToken(idTokenRecord);
        } else {
            result.idToken(idTokenRecord);
        }

        return result.build();
    }

    // TODO Add unit test
    @NonNull
    List<ICacheRecord> saveAndLoadAggregatedAccountData(
            @NonNull AccountRecord accountRecord,
            @NonNull IdTokenRecord idTokenRecord,
            @NonNull AccessTokenRecord accessTokenRecord) throws ClientException {
        // Use the just-saved ICacheRecord to locate other cache records belonging to this
        // principal which may be associated to another tenant
        return mergeCacheRecordWithOtherTenantCacheRecords(
                save(accountRecord, idTokenRecord, accessTokenRecord)
        );
    }

    @NonNull
    private List<ICacheRecord> mergeCacheRecordWithOtherTenantCacheRecords(
            @NonNull final ICacheRecord savedCacheRecord) {
        final List<ICacheRecord> result = new ArrayList<>();
        // Whatever ICacheRecord you provide will _always_ be the first element in the result List.
        result.add(savedCacheRecord);

        final List<AccountRecord> accountsInOtherTenants = new ArrayList<>(
                getAllTenantAccountsForAccountByClientId(
                        savedCacheRecord
                                .getRefreshToken()
                                .getClientId(),
                        savedCacheRecord
                                .getAccount() // This account wil be the 0th element in the result.
                )
        );

        if (!accountsInOtherTenants.isEmpty()) {
            // Remove the first element from the List since it is already contained in the result List
            accountsInOtherTenants.remove(0);

            // Iterate over the rest of the Accounts to build up the final result
            for (final AccountRecord acct : accountsInOtherTenants) {
                result.add(
                        getSparseCacheRecordForAccount(
                                savedCacheRecord.getRefreshToken().getClientId(),
                                acct
                        )
                );
            }
        }

        return result;
    }

    @Override
    public ICacheRecord save(@NonNull final GenericOAuth2Strategy oAuth2Strategy,
                             @NonNull final GenericAuthorizationRequest request,
                             @NonNull final GenericTokenResponse response) throws ClientException {
        // Create the Account
        final AccountRecord accountToSave =
                mAccountCredentialAdapter.createAccount(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the AccessToken
        final AccessTokenRecord accessTokenToSave =
                mAccountCredentialAdapter.createAccessToken(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the RefreshToken
        final RefreshTokenRecord refreshTokenToSave =
                mAccountCredentialAdapter.createRefreshToken(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Create the IdToken
        final IdTokenRecord idTokenToSave =
                mAccountCredentialAdapter.createIdToken(
                        oAuth2Strategy,
                        request,
                        response
                );

        // Check that everything we're about to save is schema-compliant...
        validateCacheArtifacts(
                accountToSave,
                accessTokenToSave,
                refreshTokenToSave,
                idTokenToSave
        );

        // Save the Account and Credentials...
        saveAccounts(accountToSave);
        synchronized (sCacheLock) {
            if (response.getIsRequestForNAA())
                saveCredentialsInternal(accessTokenToSave, idTokenToSave);
            else {
                saveCredentialsInternal(accessTokenToSave, refreshTokenToSave, idTokenToSave);
            }
            // Remove old refresh tokens (except for the one we just saved) if it's MRRT or FRT
            if (!response.getIsRequestForNAA())
                removeAllRefreshTokensExcept(accountToSave, refreshTokenToSave);
        }

        final CacheRecord.CacheRecordBuilder result = CacheRecord.builder();
        result.account(accountToSave);
        result.accessToken(accessTokenToSave);
        result.refreshToken(refreshTokenToSave);
        setToCacheRecord(result, idTokenToSave);

        return result.build();
    }

    /**
     * Removes the refresh tokens in the cache for the provided {@link AccountRecord}; will not
     * remove the deletionExempt credential.
     *
     * @param accountRecord              The AccountRecord for which RTs should be removed.
     * @param deletionExemptRefreshToken The RT record we wish to exempt from deletion.
     */
    private void removeAllRefreshTokensExcept(@NonNull final AccountRecord accountRecord,
                                              @NonNull final RefreshTokenRecord deletionExemptRefreshToken) {
        // Delete all of the refresh tokens associated with this account, except for the provided one
        final String methodName = ":removeAllRefreshTokensExcept";
        final boolean isFamilyRefreshToken = !StringUtil.isNullOrEmpty(
                deletionExemptRefreshToken.getFamilyId()
        );

        Logger.info(
                TAG + methodName,
                "isFamilyRefreshToken? [" + isFamilyRefreshToken + "]"
        );

        final boolean isMultiResourceCapable = MicrosoftAccount.AUTHORITY_TYPE_MS_STS.equals(
                accountRecord.getAuthorityType()
        );

        Logger.info(
                TAG + methodName,
                "isMultiResourceCapable? [" + isMultiResourceCapable + "]"
        );

        if (isFamilyRefreshToken || isMultiResourceCapable) {
            final String environment = accountRecord.getEnvironment();
            final String clientId = deletionExemptRefreshToken.getClientId();

            final int refreshTokensRemoved = removeCredentialsOfTypeForAccountExcept(
                    environment,
                    isFamilyRefreshToken
                            // Delete all RTs, irrespective of client_id
                            // (so long as it is not the exempted record)
                            ? null
                            : clientId,
                    CredentialType.RefreshToken,
                    accountRecord,
                    true,
                    deletionExemptRefreshToken
            );

            Logger.info(
                    TAG + methodName,
                    "Refresh tokens removed: [" + refreshTokensRemoved + "]"
            );

            if (refreshTokensRemoved > 1) {
                Logger.warn(
                        TAG + methodName,
                        "Multiple refresh tokens found for Account."
                );
            }
        }
    }

    /**
     * Removes Credentials of the supplied type for the supplied Account; skipping any record
     * specified as exempt.
     *
     * @param environment          Entity which issued the token represented as a host.
     * @param clientId             The clientId of the target app.
     * @param credentialType       The type of Credential to remove.
     * @param targetAccount        The target Account whose Credentials should be removed.
     * @param realmAgnostic        True if the specified action should be completed irrespective of realm.
     * @param deletionExemptRecord A record which explicitly must not be removed.
     * @return The number of Credentials removed.
     */
    private int removeCredentialsOfTypeForAccountExcept(@NonNull final String environment,
                                                        @Nullable final String clientId,
                                                        @NonNull final CredentialType credentialType,
                                                        @NonNull final AccountRecord targetAccount,
                                                        final boolean realmAgnostic,
                                                        @NonNull final Credential deletionExemptRecord) {
        int credentialsRemoved = 0;

        // Query it for Credentials matching the supplied targetAccount
        final List<Credential> credentialsToRemove =
                mAccountCredentialCache.getCredentialsFilteredBy(
                        targetAccount.getHomeAccountId(),
                        environment,
                        credentialType,
                        clientId,
                        null, //wildcard (*)
                        null, //wildcard
                        realmAgnostic
                                ? null // wildcard (*) realm
                                : targetAccount.getRealm(),
                        null, // wildcard (*) target,
                        null
                );

        for (final Credential credentialToRemove : credentialsToRemove) {
            // Do not delete the record, if it is the supplied exempted Credential.
            if (!deletionExemptRecord.equals(credentialToRemove)
                    && mAccountCredentialCache.removeCredential(credentialToRemove)) {
                credentialsRemoved++;
            }
        }

        return credentialsRemoved;
    }

    @Override
    @NonNull
    public List<ICacheRecord> saveAndLoadAggregatedAccountData(
            @NonNull final GenericOAuth2Strategy oAuth2Strategy,
            @NonNull final GenericAuthorizationRequest request,
            @NonNull final GenericTokenResponse response) throws ClientException {
        synchronized (this) {
            return mergeCacheRecordWithOtherTenantCacheRecords(
                    save(oAuth2Strategy, request, response)
            );
        }
    }

    /**
     * Given an AccountRecord and associated client_id, load a sparse ICacheRecord containing
     * the provided AccountRecord and its accompanying IdTokens. "Sparse" here indicates that any
     * accompanying access tokens or refresh tokens will not be loaded into the ICacheRecord.
     *
     * @param clientId The client_id relative to which IdTokens should be loaded.
     * @param acct     The target AccountRecord.
     * @return A sparse ICacheRecord containing the provided AccountRecord and its IdTokens.
     */
    ICacheRecord getSparseCacheRecordForAccount(@NonNull final String clientId,
                                                @NonNull final AccountRecord acct) {
        final String methodName = ":getSparseCacheRecordForAccount";

        final List<IdTokenRecord> acctIdTokens = getIdTokensForAccountRecord(
                clientId,
                acct
        );

        if (acctIdTokens.size() > ID_TOKEN_TYPES.size()) {
            // We shouldn't have more idtokens than types of idtokens... 1 each
            Logger.warn(
                    TAG + methodName,
                    "Found more IdTokens than expected."
                            + "\nFound: [" + acctIdTokens.size() + "]"
            );
        }

        final CacheRecord.CacheRecordBuilder associatedRecord = CacheRecord.builder();
        associatedRecord.account(acct);

        for (final IdTokenRecord idTokenRecord : acctIdTokens) {
            setToCacheRecord(associatedRecord, idTokenRecord);
        }

        return associatedRecord.build();
    }

    /**
     * Helper method to remove an old refresh token if it's MRRT ot FRT.
     */
    void removeRefreshTokenIfNeeded(@NonNull final AccountRecord accountRecord,
                                    @NonNull final RefreshTokenRecord refreshTokenRecord) {
        final String methodName = ":removeRefreshTokenIfNeeded";
        final boolean isFamilyRefreshToken = !StringUtil.isNullOrEmpty(
                refreshTokenRecord.getFamilyId()
        );

        Logger.info(
                TAG + methodName,
                "isFamilyRefreshToken? [" + isFamilyRefreshToken + "]"
        );

        final boolean isMultiResourceCapable = MicrosoftAccount.AUTHORITY_TYPE_MS_STS.equals(
                accountRecord.getAuthorityType()
        );

        Logger.info(
                TAG + methodName,
                "isMultiResourceCapable? [" + isMultiResourceCapable + "]"
        );

        if (isFamilyRefreshToken || isMultiResourceCapable) {
            final String environment = accountRecord.getEnvironment();
            final String clientId = refreshTokenRecord.getClientId();

            final int refreshTokensRemoved = removeRefreshTokensForAccount(
                    accountRecord,
                    isFamilyRefreshToken,
                    environment,
                    clientId
            );

            Logger.info(
                    TAG + methodName,
                    "Refresh tokens removed: [" + refreshTokensRemoved + "]"
            );

            if (refreshTokensRemoved > 1) {
                Logger.warn(
                        TAG + methodName,
                        "Multiple refresh tokens found for Account."
                );
            }
        }
    }

    private int removeRefreshTokensForAccount(@NonNull final AccountRecord accountToSave,
                                              final boolean isFamilyRefreshToken,
                                              @NonNull final String environment,
                                              @Nullable final String clientId) {
        // AAD v1 & v2 support multi-resource refresh tokens, allowing us to use
        // a single refresh token to service all of an account's requests.
        // To ensure that only one refresh token is maintained for an account,
        // refresh tokens are cleared from the cache for the account which is about to be
        // saved (in the event that there was already a refresh token in the cache)

        // AAD v1 & v2 also support the use of family refresh tokens (FRTs).
        // FRTs allow us to think of 1st party clients as having a shared clientId, though
        // this isn't *actually* the case. Basically, 1st party tokens in "the family"
        // are allowed to use one another's MRRTs so long as they match the current account.
        return removeCredentialsOfTypeForAccount(
                environment,
                isFamilyRefreshToken
                        // Delete all RTs, irrespective of clientId.
                        // Please note: this mechanism relies on there being a logically
                        // separate cache for FOCI inside the broker. If more families are
                        // added in the future (eg "foci" : "2") this logic will need to be
                        // modified so that the deletion is scoped to only delete RTs in a
                        // provided family. This is unimplemented for now, as it complicates the
                        // api and has only a marginal chance of ever being needed.
                        // Basically, FOCI is more like true/false than a real "id".

                        // If this cache is running in standalone (no-broker) mode,
                        // then we can think of this call as saying 'delete all RTs in the cache
                        // relative to the current account and for all client ids'. Since
                        // standalone mode only ever serves a single client id, this should be
                        // OK for now. If TSL support comes later, this approach may need to be
                        // reevaluated.
                        ? null
                        // Delete all RTs relative to this client ID
                        : clientId,
                CredentialType.RefreshToken,
                accountToSave,
                true
        );
    }

    @Override
    public ICacheRecord save(@NonNull final AccountRecord accountToSave,
                             @NonNull final IdTokenRecord idTokenToSave) {
        final String methodName = ":save";

        Logger.verbose(
                TAG + methodName,
                "Importing AccountRecord, IdTokenRecord (direct)"
        );

        // Validate the incoming artifacts
        final boolean isAccountCompliant = isAccountSchemaCompliant(accountToSave);
        final boolean isIdTokenCompliant = isIdTokenSchemaCompliant(idTokenToSave);

        final CacheRecord.CacheRecordBuilder result = CacheRecord.builder();

        if (!(isAccountCompliant && isIdTokenCompliant)) {
            String nonCompliantCredentials = "[";

            if (!isAccountCompliant) {
                nonCompliantCredentials += "(Account)";
            }

            if (!isIdTokenCompliant) {
                nonCompliantCredentials += "(ID)";
            }

            nonCompliantCredentials += "]";

            Logger.warn(
                    TAG + methodName,
                    "Skipping persistence of non-compliant credentials: "
                            + nonCompliantCredentials
            );
        } else {
            // Save the inputs
            saveAccounts(accountToSave);
            saveCredentialsInternal(idTokenToSave);

            // Set them as the result outputs
            result.account(accountToSave);
            if (CredentialType.V1IdToken.name().equalsIgnoreCase(idTokenToSave.getCredentialType())) {
                result.v1IdToken(idTokenToSave);
            } else {
                result.idToken(idTokenToSave);
            }
        }

        return result.build();
    }

    @Override
    public ICacheRecord load(@NonNull final String clientId,
                             @NonNull final String applicationIdentifier,
                             @Nullable final String mamEnrollmentIdentifier,
                             @Nullable final String target,
                             @NonNull final AccountRecord account,
                             @NonNull final AbstractAuthenticationScheme authScheme) {
        Telemetry.emit(new CacheStartEvent());

        final boolean isMultiResourceCapable = MicrosoftAccount.AUTHORITY_TYPE_MS_STS.equals(
                account.getAuthorityType()
        );

        // 'Preloading' our credentials to avoid repeated expensive cache hits
        final List<Credential> allCredentials = mAccountCredentialCache.getCredentials();

        // Load the AccessTokens
        final String kid = authScheme instanceof PopAuthenticationSchemeWithClientKeyInternal ?
                ((PopAuthenticationSchemeWithClientKeyInternal) authScheme).getKid()
                : null;
        final List<Credential> accessTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                allCredentials,
                account.getHomeAccountId(),
                account.getEnvironment(),
                getAccessTokenCredentialTypeForAuthenticationScheme(authScheme),
                clientId,
                applicationIdentifier,
                mamEnrollmentIdentifier, //Null unless Intune reports one available for this app
                account.getRealm(),
                target,
                authScheme.getName(),
                null,
                kid
        );

        // Load the RefreshTokens
        List<Credential> refreshTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                account.getHomeAccountId(),
                account.getEnvironment(),
                CredentialType.RefreshToken,
                clientId,
                null, //wildcard (*)
                null, //wildcard (*)
                isMultiResourceCapable
                        ? null // wildcard (*)
                        : account.getRealm(),
                isMultiResourceCapable
                        ? null // wildcard (*)
                        : target,
                null, // not applicable
                allCredentials
        );

        if (refreshTokens.isEmpty()) {
            // If we didn't find an RT in the cache, this could be a "TSL-seed" or "dual-client stack"
            // scenario
            //
            // Defining these terms:
            // TSL-seed: another 1P TSL integrated app has put a token into our cache so we can
            //     pick it up
            //
            // Dual-Client stack: two FoCI-enabled app registrations are sharing a single binary
            //     and accordingly, can share RTs.
            //     Examples for this might be TFL/TFW - which uses multiple client ids to enable
            //     different scenarios depending on enterprise vs. consumer usage

            // Unlike the broker, where we check if an app is FoCI prior to making a network call
            // with an arbitrary FoCI RT we find in the cache, if we're in standalone mode and find
            // a FoCI RT in the cache, the current app must also be FoCI (!!!)
            //
            // Making the assumption that the current client id can use any FoCI RT we find in the
            // cache is strictly contingent that app developers NOT mix FoCI/non-FoCI registrations
            // into same binary. If you do this, you'll get confusing errors that the RT used doesn't
            // match the client app registration. This assumption means we don't need to implement
            // "FoCI probing" and/or track FoCI app meta
            final Credential fallbackFrt = getFamilyRefreshTokenForAccount(account);

            if (null != fallbackFrt) {
                refreshTokens = new ArrayList<>();
                refreshTokens.add(fallbackFrt);
            }
        }

        // Load the IdTokens
        final List<Credential> idTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                account.getHomeAccountId(),
                account.getEnvironment(),
                IdToken,
                clientId,
                null, //wildcard (*)
                null, //wildcard (*)
                account.getRealm(),
                null, // wildcard (*),
                null, // not applicable
                allCredentials
        );

        // Load the v1 IdTokens
        final List<Credential> v1IdTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                account.getHomeAccountId(),
                account.getEnvironment(),
                CredentialType.V1IdToken,
                clientId,
                null, //wildcard (*)
                null, //wildcard (*)
                account.getRealm(),
                null, // wildcard (*)
                null, // not applicable
                allCredentials
        );

        final CacheRecord.CacheRecordBuilder result = CacheRecord.builder();
        result.account(account);
        result.accessToken(accessTokens.isEmpty() ? null : (AccessTokenRecord) accessTokens.get(0));
        result.refreshToken(refreshTokens.isEmpty() ? null : (RefreshTokenRecord) refreshTokens.get(0));
        result.idToken(idTokens.isEmpty() ? null : (IdTokenRecord) idTokens.get(0));
        result.v1IdToken(v1IdTokens.isEmpty() ? null : (IdTokenRecord) v1IdTokens.get(0));

        Telemetry.emit(new CacheEndEvent().putCacheRecordStatus(result.build()));
        return result.build();
    }

    /**
     * Load an FRTs from the cache which may be used by this account.
     *
     * @param account The account for which an FRT is sought.
     * @return A matching FRT credential, if exists. May be null.
     */
    @Nullable
    private RefreshTokenRecord getFamilyRefreshTokenForAccount(@NonNull final AccountRecord account) {
        final String methodName = ":getFamilyRefreshTokensForAccount";

        // Our eventual result - init to null, will assign if valid FRT is found
        RefreshTokenRecord result = null;

        // Look for an arbitrary RT matching the current user.
        // If we find one, check that it is FoCI, if it is, assume it works.
        final List<Credential> fallbackRts = mAccountCredentialCache.getCredentialsFilteredBy(
                account.getHomeAccountId(),
                account.getEnvironment(),
                CredentialType.RefreshToken,
                null, // wildcard (*)
                null, //wildcard (*)
                null, //wildcard (*)
                null, // wildcard (*) -- all FRTs are MRRTs by definition
                null, // wildcard (*) -- all FRTs are MRRTs by definition
                null // not applicable
        );

        if (!fallbackRts.isEmpty()) {
            Logger.verbose(
                    TAG + methodName,
                    "Inspecting fallback RTs for a FoCI match."
            );

            // Any arbitrary RT should be OK -- if multiple clients are stacked,
            // they're either "all FoCI" or none are.
            for (final Credential rt : fallbackRts) {
                if (rt instanceof RefreshTokenRecord) {
                    final RefreshTokenRecord refreshTokenRecord = (RefreshTokenRecord) rt;

                    final boolean isFamilyRefreshToken = !StringUtil.isNullOrEmpty(
                            refreshTokenRecord.getFamilyId()
                    );

                    if (isFamilyRefreshToken) {
                        Logger.verbose(
                                TAG + methodName,
                                "Fallback RT found."
                        );

                        result = refreshTokenRecord;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Load FRTs from the cache for an account matching the homeAccountId
     *
     * @param homeAccountId homeAccountId for which FRT is sought
     * @return an FRT if available else null.
     */
    @Nullable
    public RefreshTokenRecord getFamilyRefreshTokenForHomeAccountId(@NonNull final String homeAccountId) {

        for (AccountRecord accountRecord : mAccountCredentialCache.getAccounts()) {
            if (accountRecord.getHomeAccountId().equals(homeAccountId)) {
                return getFamilyRefreshTokenForAccount(accountRecord);
            }
        }
        return null;
    }

    @Override
    public List<ICacheRecord> loadWithAggregatedAccountData(@NonNull final String clientId,
                                                            @NonNull final String applicationIdentifier,
                                                            @Nullable final String mamEnrollmentIdentifier,
                                                            @Nullable final String target,
                                                            @NonNull final AccountRecord account,
                                                            @NonNull final AbstractAuthenticationScheme authScheme) {
        synchronized (this) {
            final List<ICacheRecord> result = new ArrayList<>();

            final ICacheRecord primaryCacheRecord = load(clientId, applicationIdentifier, mamEnrollmentIdentifier, target, account, authScheme);

            // Set this result as the 0th entry in the result...
            result.add(primaryCacheRecord);

            final List<ICacheRecord> corollaryCacheRecords = getAccountsWithAggregatedAccountData(
                    account.getEnvironment(),
                    clientId,
                    account.getHomeAccountId()
            );

            // corollaryCacheRecords will contain the original element that we've already added to
            // our result so skip that element, but add the rest...
            for (final ICacheRecord cacheRecord : corollaryCacheRecords) {
                if (!account.equals(cacheRecord.getAccount())) {
                    result.add(cacheRecord);
                }
            }

            return result;
        }
    }

    @Override
    public List<IdTokenRecord> getIdTokensForAccountRecord(@Nullable String clientId,
                                                           @NonNull AccountRecord accountRecord) {
        final List<IdTokenRecord> result = new ArrayList<>();

        // Load all the credentials to inspect once, such that we don't need to requery the cache
        // pass these into the new getCredentialsFilteredBy overload, rather than hit disk again
        final List<Credential> allCredentials = mAccountCredentialCache.getCredentials();

        final List<Credential> idTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                accountRecord.getHomeAccountId(),
                accountRecord.getEnvironment(),
                IdToken,
                clientId, // If null, behaves as wildcard
                null,
                null,
                accountRecord.getRealm(),
                null, // wildcard (*),
                null, // not applicable
                allCredentials
        );

        idTokens.addAll(
                mAccountCredentialCache.getCredentialsFilteredBy(
                        accountRecord.getHomeAccountId(),
                        accountRecord.getEnvironment(),
                        CredentialType.V1IdToken,
                        clientId,
                        null, //wildcard (*)
                        null, //wildcard (*)
                        accountRecord.getRealm(),
                        null, // wildcard (*)
                        null, // not applicable
                        allCredentials
                )
        );

        for (final Credential credential : idTokens) {
            if (credential instanceof IdTokenRecord) {
                result.add((IdTokenRecord) credential);
            }
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean removeCredential(final Credential credential) {
        final String methodName = ":removeCredential";

        Logger.info(
                TAG + methodName,
                "Removing credential..."
        );

        Logger.verbosePII(
                TAG + methodName,
                "ClientId: [" + credential.getClientId() + "]"
                        + "\n"
                        + "CredentialType: [" + credential.getCredentialType() + "]"
                        + "\n"
                        + "CachedAt: [" + credential.getCachedAt() + "]"
                        + "\n"
                        + "Environment: [" + credential.getEnvironment() + "]"
                        + "\n"
                        + "HomeAccountId: [" + credential.getHomeAccountId() + "]"
                        + "\n"
                        + "IsExpired?: [" + credential.isExpired() + "]"
        );

        return mAccountCredentialCache.removeCredential(credential);
    }

    @Override
    @Nullable
    public AccountRecord getAccount(@Nullable final String environment,
                                    @NonNull final String clientId,
                                    @NonNull final String homeAccountId,
                                    @Nullable final String realm) {
        final String methodName = ":getAccount";

        Logger.verbosePII(
                TAG + methodName,
                "Environment: [" + environment + "]"
                        + "\n"
                        + "ClientId: [" + clientId + "]"
                        + "\n"
                        + "HomeAccountId: [" + homeAccountId + "]"
                        + "\n"
                        + "Realm: [" + realm + "]"
        );

        final List<AccountRecord> allAccounts = getAccounts(environment, clientId);

        Logger.info(
                TAG + methodName,
                "Found " + allAccounts.size() + " accounts"
        );

        // Return the sought Account matching the supplied homeAccountId and realm, if applicable
        for (final AccountRecord account : allAccounts) {
            if (homeAccountId.equals(account.getHomeAccountId())
                    && (null == realm || realm.equals(account.getRealm()))) {
                return account;
            }
        }

        Logger.warn(
                TAG + methodName,
                "No matching account found."
        );

        return null;
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(@Nullable final String environment,
                                                                   @NonNull final String clientId,
                                                                   @NonNull final String homeAccountId) {
        final List<ICacheRecord> result = new ArrayList<>();

        final AccountRecord anyMatchingAccount = getAccount(
                environment,
                clientId,
                homeAccountId,
                null // realm
        );

        if (null != anyMatchingAccount) {
            final List<AccountRecord> corollaryAccounts = getAllTenantAccountsForAccountByClientId(
                    clientId,
                    anyMatchingAccount
            );

            for (final AccountRecord accountRecord : corollaryAccounts) {
                result.add(
                        getSparseCacheRecordForAccount(
                                clientId,
                                accountRecord
                        )
                );
            }
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    @Nullable
    public AccountRecord getAccountByLocalAccountId(@Nullable final String environment,
                                                    @NonNull final String clientId,
                                                    @NonNull final String localAccountId) {
        final String methodName = ":getAccountByLocalAccountId";

        final List<AccountRecord> accounts = getAccounts(environment, clientId);

        Logger.verbosePII(
                TAG + methodName,
                "LocalAccountId: [" + localAccountId + "]"
        );

        for (final AccountRecord account : accounts) {
            if (localAccountId.equals(account.getLocalAccountId())) {
                return account;
            }
        }

        return null;
    }

    /**
     * MSAL-only API for querying AccountRecords by username (upn/preferred_username).
     *
     * @param environment The environment to which the sought AccountRecords are associated.
     * @param clientId    The clientId to which the sought AccountRecords are associated.
     * @param username    The username of the sought AccountRecords.
     * @return A List of AccountRecords matching the supplied criteria. Cannot be null, may be empty.
     */
    public List<AccountRecord> getAccountsByUsername(@Nullable final String environment,
                                                     @NonNull final String clientId,
                                                     @NonNull final String username) {
        final String methodName = ":getAccountsByUsername";
        final List<AccountRecord> result = new ArrayList<>();

        final List<AccountRecord> accounts = getAccounts(environment, clientId);

        for (final AccountRecord account : accounts) {
            if (StringUtil.equalsIgnoreCase(account.getUsername(), username)) {
                result.add(account);
            }
        }

        Logger.verbose(
                TAG + methodName,
                "Found "
                        + accounts.size()
                        + " accounts matching username."
        );

        return result;
    }

    @Override
    @Nullable
    public ICacheRecord getAccountWithAggregatedAccountDataByLocalAccountId(
            @Nullable String environment,
            @NonNull String clientId,
            @NonNull String localAccountId) {
        CacheRecord.CacheRecordBuilder result = null;

        final AccountRecord acct = getAccountByLocalAccountId(
                environment,
                clientId,
                localAccountId
        );

        if (null != acct) {
            final List<IdTokenRecord> acctIdTokens = getIdTokensForAccountRecord(
                    clientId,
                    acct
            );

            result = CacheRecord.builder();
            result.account(acct);
            for (final IdTokenRecord idTokenRecord : acctIdTokens) {
                setToCacheRecord(result, idTokenRecord);
            }
            return result.build();
        }

        return null;
    }

    /**
     * Given a CacheRecord and IdTokenRecord, set the IdToken on the cache record in the field
     * corresponding to the IdToken's version.
     *
     * @param target        The CacheRecord into which said IdToken should be placed.
     * @param idTokenRecord The IdToken to associate.
     */
    private void setToCacheRecord(@NonNull final CacheRecord.CacheRecordBuilder target,
                                  @NonNull final IdTokenRecord idTokenRecord) {
        final String methodName = ":setToCacheRecord";

        final CredentialType type = CredentialType.fromString(
                idTokenRecord.getCredentialType()
        );

        if (null != type) {
            if (CredentialType.V1IdToken == type) {
                target.v1IdToken(idTokenRecord);
            } else if (IdToken == type) {
                target.idToken(idTokenRecord);
            } else {
                Logger.warn(
                        TAG + methodName,
                        "Unrecognized IdToken type: "
                                + idTokenRecord.getCredentialType()
                );
            }
        }
    }

    @Override
    public List<AccountRecord> getAccounts(@Nullable final String environment,
                                           @NonNull final String clientId) {
        final String methodName = ":getAccounts";

        Logger.verbosePII(
                TAG + methodName,
                "Environment: [" + environment + "]"
                        + "\n"
                        + "ClientId: [" + clientId + "]"
        );

        final List<AccountRecord> accountsForThisApp = new ArrayList<>();

        // Get all of the Accounts for this environment
        final List<AccountRecord> accountsForEnvironment =
                mAccountCredentialCache.getAccountsFilteredBy(
                        null, // wildcard (*) homeAccountId
                        environment,
                        null // wildcard (*) realm
                );

        Logger.verbose(
                TAG + methodName,
                "Found " + accountsForEnvironment.size() + " accounts for this environment"
        );

        final Set<CredentialType> credentialTypes = new HashSet<>(
                Arrays.asList(IdToken, V1IdToken, RefreshToken)
        );

        final List<Credential> appCredentials = mAccountCredentialCache.getCredentialsFilteredBy(
                null, // homeAccountId
                environment,
                credentialTypes,
                clientId,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // For each Account with an associated RT, add it to the result List...
        for (final AccountRecord account : accountsForEnvironment) {
            if (accountHasCredential(account, appCredentials)) {
                accountsForThisApp.add(account);
            }
        }

        Logger.verbose(
                TAG + methodName,
                "Found " + accountsForThisApp.size() + " accounts for this clientId"
        );

        return Collections.unmodifiableList(accountsForThisApp);
    }

    @Override
    public List<AccountRecord> getAllTenantAccountsForAccountByClientId(@NonNull final String clientId,
                                                                        @NonNull final AccountRecord accountRecord) {
        final List<AccountRecord> allTenantAccounts = new ArrayList<>();

        // Add the supplied AccountRecord as the 0th element...
        allTenantAccounts.add(accountRecord);

        // Grab all the accounts which might match
        final List<AccountRecord> allMatchingAccountsByHomeId =
                mAccountCredentialCache.getAccountsFilteredBy(
                        accountRecord.getHomeAccountId(),
                        accountRecord.getEnvironment(),
                        null // realm
                );

        // Grab all of the AccountRecords associated with this clientId
        final List<AccountRecord> allAppAccounts = getAccounts(
                accountRecord.getEnvironment(),
                clientId
        );

        // Iterate and populate
        for (final AccountRecord acct : allAppAccounts) {
            if (allMatchingAccountsByHomeId.contains(acct) && !accountRecord.equals(acct)) {
                allTenantAccounts.add(acct);
            }
        }

        return Collections.unmodifiableList(allTenantAccounts);
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(@Nullable final String environment,
                                                                   @NonNull final String clientId) {
        final String methodName = ":getAccountsWithAggregatedAccountData";
        final List<ICacheRecord> result = new ArrayList<>();

        final List<AccountRecord> allMatchingAccounts = getAccounts(
                environment,
                clientId
        );

        for (final AccountRecord accountRecord : allMatchingAccounts) {
            final List<IdTokenRecord> idTokensForAccount = getIdTokensForAccountRecord(
                    clientId,
                    accountRecord
            );

            // may use idToken entries
            if (idTokensForAccount == null || idTokensForAccount.size() == 0) {
                // Skip returning account record if there is no corresponding idToken record in the cache for the given clientId
                continue;
            }

            // Construct the cache record....
            final CacheRecord.CacheRecordBuilder cacheRecordBuilder = CacheRecord.builder();
            cacheRecordBuilder.account(accountRecord);
            // Set the IdTokens...
            for (IdTokenRecord idTokenRecord : idTokensForAccount) {
                setToCacheRecord(cacheRecordBuilder, idTokenRecord);
            }

            result.add(cacheRecordBuilder.build());

        }

        Logger.verbose(
                TAG + methodName,
                "Found " + result.size() + " accounts with IdTokens"
        );

        return Collections.unmodifiableList(result);
    }

    private CredentialType getAccessTokenCredentialTypeForAuthenticationScheme(
            @NonNull final AbstractAuthenticationScheme authScheme) {
        if (SCHEME_BEARER.equalsIgnoreCase(authScheme.getName())) {
            return CredentialType.AccessToken;
        } else {
            return CredentialType.AccessToken_With_AuthScheme;
        }
    }

    /**
     * Evaluates the supplied list of Credentials. Returns true if the provided Account
     * 'owns' any one of these tokens.
     *
     * @param account        The Account whose credential ownership should be evaluated.
     * @param appCredentials The Credentials to evaluate.
     * @return True, if this Account has Credentials. False otherwise.
     */
    private boolean accountHasCredential(@NonNull final AccountRecord account,
                                         @NonNull final List<Credential> appCredentials) {
        final String methodName = ":accountHasCredential";

        final String accountHomeId = account.getHomeAccountId();
        final String accountEnvironment = account.getEnvironment();

        Logger.verbosePII(
                TAG + methodName,
                "HomeAccountId: [" + accountHomeId + "]"
                        + "\n"
                        + "Environment: [" + accountEnvironment + "]"
        );

        for (final Credential credential : appCredentials) {
            if (accountHomeId.equals(credential.getHomeAccountId())
                    && accountEnvironment.equals(credential.getEnvironment())) {
                Logger.verbose(
                        TAG + methodName,
                        "Credentials located for account."
                );
                return true;
            }
        }

        return false;
    }

    /**
     * Removes the specified Account or Accounts from the cache.
     * <p>
     * Note: if realm is passed as null, all tokens and AccountRecords associated to the
     * provided homeAccountId will be deleted. If a realm is provided, then the deletion is
     * restricted to only those AccountRecords and Credentials in that realm (tenant).
     * <p>
     * clientId, and home_account_id are nullable parameters. However, it should be
     * noted that if these params are null, this method will have no effect.
     *
     * @param environment   The environment to which the targeted Account is associated.
     * @param clientId      The clientId of this current app.
     * @param homeAccountId The homeAccountId of the Account targeted for deletion.
     * @param realm         The tenant id of the targeted Account (if applicable).
     * @return An {@link AccountDeletionRecord}, containing the deleted {@link AccountDeletionRecord}s.
     */
    @Override
    public AccountDeletionRecord removeAccount(@Nullable final String environment,
                                               @Nullable final String clientId,
                                               @Nullable final String homeAccountId,
                                               @Nullable final String realm) {
        return removeAccount(
                environment,
                clientId,
                homeAccountId,
                realm,
                CredentialType.AccessToken,
                CredentialType.AccessToken_With_AuthScheme,
                CredentialType.RefreshToken,
                IdToken,
                CredentialType.V1IdToken
        );
    }

    /**
     * Removes the specified Account or Accounts from the cache.
     * <p>
     * Note: if realm is passed as null, all tokens and AccountRecords associated to the
     * provided homeAccountId will be deleted. If a realm is provided, then the deletion is
     * restricted to only those AccountRecords and Credentials in that realm (tenant).
     * <p>
     * clientId, and home_account_id are nullable parameters. However, it should be
     * noted that if these params are null, this method will have no effect.
     *
     * @param environment   The environment to which the targeted Account is associated.
     * @param clientId      The clientId of this current app.
     * @param homeAccountId The homeAccountId of the Account targeted for deletion.
     * @param realm         The tenant id of the targeted Account (if applicable).
     * @param typesToRemove The CredentialTypes to delete for the targeted Account.
     * @return An {@link AccountDeletionRecord}, containing the deleted {@link AccountDeletionRecord}s.
     */
    @Override
    public AccountDeletionRecord removeAccount(@Nullable final String environment,
                                               @Nullable final String clientId,
                                               @Nullable final String homeAccountId,
                                               @Nullable final String realm,
                                               @Nullable final CredentialType... typesToRemove) {
        final String methodName = ":removeAccount";

        Logger.verbosePII(
                TAG + methodName,
                "Environment: [" + environment + "]"
                        + "\n"
                        + "ClientId: [" + clientId + "]"
                        + "\n"
                        + "HomeAccountId: [" + homeAccountId + "]"
                        + "\n"
                        + "Realm: [" + realm + "]"
                        + "\n"
                        + "CredentialTypes to delete: [" + Arrays.toString(typesToRemove) + "]"
        );

        final AccountRecord targetAccount;
        if (null == clientId
                || null == homeAccountId
                || null == (targetAccount =
                getAccount(
                        environment,
                        clientId,
                        homeAccountId,
                        realm
                ))) {

            Logger.warn(
                    TAG + methodName,
                    "Insufficient filtering provided for account removal - preserving Account."
            );

            return new AccountDeletionRecord(null);
        }

        // If no realm is provided, remove the Account/Credentials from all realms.
        final boolean isRealmAgnostic = (null == realm);

        Logger.verbose(
                TAG + methodName,
                "IsRealmAgnostic? " + isRealmAgnostic
        );

        if (null != typesToRemove && typesToRemove.length > 0) {
            for (final CredentialType type : typesToRemove) {
                // A count of the deleted creds...
                int deletedCredentialsOfTypeCount = removeCredentialsOfTypeForAccount(
                        environment,
                        clientId,
                        type,
                        targetAccount,
                        isRealmAgnostic
                );

                Logger.info(
                        TAG + methodName,
                        "Removed "
                                + deletedCredentialsOfTypeCount
                                + " credentials of type: "
                                + type
                );
            }
        } else {
            Logger.warn(
                    TAG + methodName,
                    "removeAccount called, but no CredentialTypes to remove specified"
            );
        }

        final List<AccountRecord> deletedAccounts = new ArrayList<>();

        if (isRealmAgnostic) {
            // Remove all Accounts associated with this home_account_id...
            final List<AccountRecord> accountsToRemove = mAccountCredentialCache.getAccountsFilteredBy(
                    homeAccountId,
                    environment,
                    null // wildcard (*) realm
            );

            for (final AccountRecord accountToRemove : accountsToRemove) {
                if (mAccountCredentialCache.removeAccount(accountToRemove)) {
                    deletedAccounts.add(accountToRemove);
                }
            }
        } else {
            // Remove only the target Account
            if (mAccountCredentialCache.removeAccount(targetAccount)) {
                deletedAccounts.add(targetAccount);
            }
        }

        return new AccountDeletionRecord(deletedAccounts);
    }

    @Override
    public void clearAll() {
        final String methodName = ":clearAll";

        Logger.warn(
                TAG + methodName,
                "Clearing cache."
        );

        mAccountCredentialCache.clearAll();
    }

    @Override
    protected Set<String> getAllClientIds() {
        final String methodName = ":getAllClientIds";

        final Set<String> result = new HashSet<>();

        for (final Credential credential : mAccountCredentialCache.getCredentials()) {
            result.add(credential.getClientId());
        }

        Logger.verbose(
                TAG + methodName,
                "Found ["
                        + result.size()
                        + "] clientIds/"
        );

        return result;
    }

    @Nullable
    @Override
    public AccountRecord getAccountByHomeAccountId(@Nullable final String environment,
                                                   @NonNull final String clientId,
                                                   @NonNull final String homeAccountId) {
        final String methodName = ":getAccountByHomeAccountId";

        final List<AccountRecord> accounts = getAccounts(environment, clientId);

        Logger.verbosePII(
                TAG + methodName,
                "homeAccountId: [" + homeAccountId + "]"
        );

        for (final AccountRecord account : accounts) {
            if (homeAccountId.equals(account.getHomeAccountId())) {
                return account;
            }
        }

        return null;
    }

    /**
     * Removes Credentials of the supplied type for the supplied Account.
     *
     * @param environment    Entity which issued the token represented as a host.
     * @param clientId       The clientId of the target app.
     * @param credentialType The type of Credential to remove.
     * @param targetAccount  The target Account whose Credentials should be removed.
     * @param realmAgnostic  True if the specified action should be completed irrespective of realm.
     * @return The number of Credentials removed.
     */
    private int removeCredentialsOfTypeForAccount(
            @Nullable final String environment, // 'authority host'
            @Nullable final String clientId,
            @NonNull final CredentialType credentialType,
            @NonNull final AccountRecord targetAccount,
            boolean realmAgnostic) {
        int credentialsRemoved = 0;

        // Query it for Credentials matching the supplied targetAccount
        final List<Credential> credentialsToRemove =
                mAccountCredentialCache.getCredentialsFilteredBy(
                        targetAccount.getHomeAccountId(),
                        environment,
                        credentialType,
                        clientId,
                        null, //wildcard (*)
                        null, //wildcard (*)
                        realmAgnostic
                                ? null // wildcard (*) realm
                                : targetAccount.getRealm(),
                        null, // wildcard (*) target,
                        null
                );

        for (final Credential credentialToRemove : credentialsToRemove) {
            if (mAccountCredentialCache.removeCredential(credentialToRemove)) {
                credentialsRemoved++;
            }
        }

        return credentialsRemoved;
    }

    private void saveAccounts(final AccountRecord... accounts) {
        for (final AccountRecord account : accounts) {
            mAccountCredentialCache.saveAccount(account);
        }
    }

    void saveCredentialsInternal(final Credential... credentials) {
        for (final Credential credential : credentials) {
            if (credential == null) {
                continue;
            }

            if (credential instanceof AccessTokenRecord) {
                deleteAccessTokensWithIntersectingScopes((AccessTokenRecord) credential);
            }

            mAccountCredentialCache.saveCredential(credential);
        }
    }

    public void saveRefreshTokenForNAA(@NonNull final GenericOAuth2Strategy oAuth2Strategy,
                                       @NonNull final GenericAuthorizationRequest request,
                                       @NonNull final GenericTokenResponse response) throws ClientException {
        final AccountRecord accountToSave =
                mAccountCredentialAdapter.createAccount(
                        oAuth2Strategy,
                        request,
                        response
                );
        // Create the RefreshToken
        final RefreshTokenRecord refreshTokenToSave =
                mAccountCredentialAdapter.createRefreshToken(
                        oAuth2Strategy,
                        request,
                        response
                );
        final boolean isRefreshTokenCompliant = isRefreshTokenSchemaCompliant(refreshTokenToSave);
        if (!isRefreshTokenCompliant) {
            throw new ClientException(
                    CREDENTIAL_IS_SCHEMA_NONCOMPLIANT,
                    "(RT)"
            );
        }
        synchronized (sCacheLock) {
            saveCredentialsInternal(refreshTokenToSave);
            // Remove old refresh tokens (except for the one we just saved) if it's MRRT or FRT
                removeAllRefreshTokensExcept(accountToSave, refreshTokenToSave);
        }
    }

    public void saveRefreshTokenForNAA(@Nullable final AccountRecord accountRecord,
                                       @NonNull final RefreshTokenRecord refreshTokenRecord) throws ClientException {
        final boolean isRefreshTokenCompliant = isRefreshTokenSchemaCompliant(refreshTokenRecord);
        if (!isRefreshTokenCompliant) {
            throw new ClientException(
                    CREDENTIAL_IS_SCHEMA_NONCOMPLIANT,
                    "(RT)"
            );
        }
        synchronized (sCacheLock) {
            saveCredentialsInternal(refreshTokenRecord);
            // Remove old refresh tokens (except for the one we just saved) if it's MRRT or FRT
            removeAllRefreshTokensExcept(accountRecord, refreshTokenRecord);
        }
    }


    /**
     * Validates that the supplied artifacts are schema-compliant and OK to write to the cache.
     *
     * @param accountToSave      The {@link AccountRecord} to save.
     * @param accessTokenToSave  The {@link AccessTokenRecord} to save or null. Null params are assumed
     *                           valid; this condition supports the SSO case.
     * @param refreshTokenToSave The {@link RefreshTokenRecord}
     *                           to save.
     * @param idTokenToSave      The {@link IdTokenRecord} to save.
     * @throws ClientException If any of the supplied artifacts are non schema-compliant.
     */
    void validateCacheArtifacts(
            @NonNull final AccountRecord accountToSave,
            final AccessTokenRecord accessTokenToSave,
            @NonNull final RefreshTokenRecord refreshTokenToSave,
            @NonNull final IdTokenRecord idTokenToSave) throws ClientException {
        final String methodName = ":validateCacheArtifacts";
        Logger.verbose(
                TAG + methodName,
                "Validating cache artifacts..."
        );

        final boolean isAccountCompliant = isAccountSchemaCompliant(accountToSave);
        final boolean isAccessTokenCompliant = null == accessTokenToSave || isAccessTokenSchemaCompliant(accessTokenToSave);
        final boolean isRefreshTokenCompliant = isRefreshTokenSchemaCompliant(refreshTokenToSave);
        final boolean isIdTokenCompliant = isIdTokenSchemaCompliant(idTokenToSave);

        if (!isAccountCompliant) {
            throw new ClientException(ACCOUNT_IS_SCHEMA_NONCOMPLIANT);
        }

        if (!(isAccessTokenCompliant
                && isRefreshTokenCompliant
                && isIdTokenCompliant)) {
            String nonCompliantCredentials = "[";

            if (!isAccessTokenCompliant) {
                nonCompliantCredentials += "(AT)";
            }

            if (!isRefreshTokenCompliant) {
                nonCompliantCredentials += "(RT)";
            }

            if (!isIdTokenCompliant) {
                nonCompliantCredentials += "(ID)";
            }

            nonCompliantCredentials += "]";

            throw new ClientException(
                    CREDENTIAL_IS_SCHEMA_NONCOMPLIANT,
                    nonCompliantCredentials
            );
        }
    }

    private void deleteAccessTokensWithIntersectingScopes(
            final AccessTokenRecord referenceToken) {
        final String methodName = "deleteAccessTokensWithIntersectingScopes";

        final List<Credential> accessTokens = mAccountCredentialCache.getCredentialsFilteredBy(
                referenceToken.getHomeAccountId(),
                referenceToken.getEnvironment(),
                CredentialType.fromString(referenceToken.getCredentialType()),
                referenceToken.getClientId(),
                referenceToken.getApplicationIdentifier(),
                referenceToken.getMamEnrollmentIdentifier(),
                referenceToken.getRealm(),
                null, // Wildcard (*)
                referenceToken.getAccessTokenType(),
                referenceToken.getRequestedClaims(),
                mAccountCredentialCache.getCredentials()
        );

        Logger.verbose(
                TAG + ":" + methodName,
                "Inspecting " + accessTokens.size() + " accessToken[s]."
        );

        for (final Credential accessToken : accessTokens) {
            if (scopesIntersect(referenceToken, (AccessTokenRecord) accessToken, true)) {
                Logger.infoPII(
                        TAG + ":" + methodName,
                        "Removing credential: " + accessToken
                );
                mAccountCredentialCache.removeCredential(accessToken);
            }
        }
    }

    private boolean scopesIntersect(final AccessTokenRecord token1,
                                    final AccessTokenRecord token2,
                                    boolean omitDefaultScopes) {
        final String methodName = "scopesIntersect";

        final Set<String> token1Scopes = scopesAsSet(token1);
        final Set<String> token2Scopes = scopesAsSet(token2);

        if (omitDefaultScopes) {
            // Remove the default scopes (if present), do not consider them in lookup criteria
            token1Scopes.removeAll(AuthenticationConstants.DEFAULT_SCOPES);
            token2Scopes.removeAll(AuthenticationConstants.DEFAULT_SCOPES);
        }

        boolean result = false;
        for (final String scope : token2Scopes) {
            if (token1Scopes.contains(scope)) {
                Logger.info(TAG + ":" + methodName, "Scopes intersect.");
                Logger.infoPII(
                        TAG + ":" + methodName,
                        token1Scopes.toString() + " contains [" + scope + "]"
                );
                result = true;
                break;
            }
        }

        return result;
    }

    private Set<String> scopesAsSet(final AccessTokenRecord token) {
        final Set<String> scopeSet = new HashSet<>();
        final String scopeString = token.getTarget();

        if (!StringUtil.isNullOrEmpty(scopeString)) {
            final String[] scopeArray = scopeString.split("\\s+");
            scopeSet.addAll(Arrays.asList(scopeArray));
        }

        return scopeSet;
    }

    private static boolean isSchemaCompliant(final Class<?> clazz, final String[][] params) {
        final String methodName = "isSchemaCompliant";

        boolean isCompliant = true;
        for (final String[] param : params) {
            isCompliant = isCompliant && !StringUtil.isNullOrEmpty(param[1]);
        }

        if (!isCompliant) {
            Logger.warn(
                    TAG + ":" + methodName,
                    clazz.getSimpleName() + " does not contain all required fields."
            );

            for (final String[] param : params) {
                Logger.warn(
                        TAG + ":" + methodName,
                        param[0] + " is null? [" + StringUtil.isNullOrEmpty(param[1]) + "]"
                );
            }
        }

        return isCompliant;
    }

    private boolean isAccountSchemaCompliant(@NonNull final AccountRecord account) {
        // Required fields...
        final String[][] params = new String[][]{
                {AccountRecord.SerializedNames.HOME_ACCOUNT_ID, account.getHomeAccountId()},
                {AccountRecord.SerializedNames.ENVIRONMENT, account.getEnvironment()},
                //TODO Need to fix the validation for realm for AAD IDP scenario.
                //{AccountRecord.SerializedNames.REALM, account.getRealm()},
                {AccountRecord.SerializedNames.LOCAL_ACCOUNT_ID, account.getLocalAccountId()},
                {AccountRecord.SerializedNames.USERNAME, account.getUsername()},
                {AccountRecord.SerializedNames.AUTHORITY_TYPE, account.getAuthorityType()},
        };

        return isSchemaCompliant(account.getClass(), params);
    }

    boolean isAccessTokenSchemaCompliant(@NonNull final AccessTokenRecord accessToken) {
        // Required fields...
        final String[][] params = new String[][]{
                {Credential.SerializedNames.CREDENTIAL_TYPE, accessToken.getCredentialType()},
                {Credential.SerializedNames.HOME_ACCOUNT_ID, accessToken.getHomeAccountId()},
                //TODO Need to fix the validation for realm for AAD IDP scenario.
                //{AccessTokenRecord.SerializedNames.REALM, accessToken.getRealm()},
                {Credential.SerializedNames.ENVIRONMENT, accessToken.getEnvironment()},
                {Credential.SerializedNames.CLIENT_ID, accessToken.getClientId()},
                {AccessTokenRecord.SerializedNames.TARGET, accessToken.getTarget()},
                {Credential.SerializedNames.CACHED_AT, accessToken.getCachedAt()},
                {Credential.SerializedNames.EXPIRES_ON, accessToken.getExpiresOn()},
                {Credential.SerializedNames.SECRET, accessToken.getSecret()},
        };

        return isSchemaCompliant(accessToken.getClass(), params);
    }

    private boolean isRefreshTokenSchemaCompliant(
            @NonNull final RefreshTokenRecord refreshToken) {
        // Required fields...
        final String[][] params = new String[][]{
                {Credential.SerializedNames.CREDENTIAL_TYPE, refreshToken.getCredentialType()},
                {Credential.SerializedNames.ENVIRONMENT, refreshToken.getEnvironment()},
                {Credential.SerializedNames.HOME_ACCOUNT_ID, refreshToken.getHomeAccountId()},
                {Credential.SerializedNames.CLIENT_ID, refreshToken.getClientId()},
                {Credential.SerializedNames.SECRET, refreshToken.getSecret()},
        };

        return isSchemaCompliant(refreshToken.getClass(), params);
    }

    private boolean isIdTokenSchemaCompliant(@NonNull final IdTokenRecord idToken) {
        final String[][] params = new String[][]{
                {Credential.SerializedNames.HOME_ACCOUNT_ID, idToken.getHomeAccountId()},
                {Credential.SerializedNames.ENVIRONMENT, idToken.getEnvironment()},
                //TODO Need to fix the validation for realm for AAD IDP scenario.
                //{IdTokenRecord.SerializedNames.REALM, idToken.getRealm()},
                {Credential.SerializedNames.CREDENTIAL_TYPE, idToken.getCredentialType()},
                {Credential.SerializedNames.CLIENT_ID, idToken.getClientId()},
                {Credential.SerializedNames.SECRET, idToken.getSecret()},
        };

        return isSchemaCompliant(idToken.getClass(), params);
    }

    protected IAccountCredentialCache getAccountCredentialCache() {
        return mAccountCredentialCache;
    }

    IAccountCredentialAdapter<
            GenericOAuth2Strategy,
            GenericAuthorizationRequest,
            GenericTokenResponse,
            GenericAccount,
            GenericRefreshToken> getAccountCredentialAdapter() {
        return mAccountCredentialAdapter;
    }

    @Override
    public void setSingleSignOnState(final GenericAccount account,
                                     final GenericRefreshToken refreshToken) throws ClientException {
        Logger.info(TAG + ":setSingleSignOnState", "Set SSO state called.");

        final AccountRecord accountDto = mAccountCredentialAdapter.asAccount(account);
        final RefreshTokenRecord rt = mAccountCredentialAdapter.asRefreshToken(refreshToken);
        final IdTokenRecord idToken = mAccountCredentialAdapter.asIdToken(account, refreshToken);

        validateCacheArtifacts(
                accountDto,
                null,
                rt,
                idToken
        );

        saveAccounts(accountDto);
        synchronized (sCacheLock) {
            saveCredentialsInternal(idToken, rt);

            removeAllRefreshTokensExcept(accountDto, rt);
        }
    }

    @Override
    public GenericRefreshToken getSingleSignOnState(final GenericAccount account) {
        throw new UnsupportedOperationException("Unimplemented!");
    }

}
