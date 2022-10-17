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

import static com.microsoft.identity.common.java.cache.AbstractAccountCredentialCache.targetsIntersect;

import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.AuthenticationSchemeFactory;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

// Suppressing rawtype warnings due to the generic type OAuth2Strategy and AuthorizationRequest
@SuppressWarnings(WarningType.rawtype_warning)
public class MicrosoftFamilyOAuth2TokenCache
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends TokenResponse,
                GenericAccount extends BaseAccount,
                GenericRefreshToken extends RefreshToken>
        extends MsalOAuth2TokenCache<GenericOAuth2Strategy, GenericAuthorizationRequest, GenericTokenResponse, GenericAccount, GenericRefreshToken> {

    private static final String TAG = MicrosoftFamilyOAuth2TokenCache.class.getSimpleName();

    /**
     * Constructs a new OAuth2TokenCache.
     *
     * @param context The Application Context of the consuming app.
     */
    public MicrosoftFamilyOAuth2TokenCache(final IPlatformComponents context,
                                           final IAccountCredentialCache accountCredentialCache,
                                           final IAccountCredentialAdapter<
                                                                                              GenericOAuth2Strategy,
                                                                                              GenericAuthorizationRequest,
                                                                                              GenericTokenResponse,
                                                                                              GenericAccount,
                                                                                              GenericRefreshToken> accountCredentialAdapter) {
        super(context, accountCredentialCache, accountCredentialAdapter);
    }

    /**
     * Loads the tokens available for the supplied client criteria.
     *
     * @param clientId      The current client's id.
     * @param accountRecord The current account.
     * @return An ICacheRecord containing the account. If a matching refresh token is available
     * it is returned.
     */
    public ICacheRecord loadByFamilyId(@Nullable final String clientId,
                                       @Nullable final String target,
                                       @NonNull final AccountRecord accountRecord,
                                       @Nullable final AbstractAuthenticationScheme authenticationScheme) {
        final String methodName = ":loadByFamilyId";

        final String familyId = "1";

        Logger.verbose(
                TAG + methodName,
                "ClientId[" + clientId + ", " + familyId + "]"
        );

        // The following fields must match when querying for RTs:
        // - environment
        // - home_account_id
        // - credential_type == RT
        //
        // The following fields do not matter when querying for RTs:
        // - clientId doesn't matter (FRT)
        // - target doesn't matter (FRT) (but we will inspect it when looking for an AT)
        // - realm doesn't matter (MRRT)

        RefreshTokenRecord rtToReturn = null;
        IdTokenRecord idTokenToReturn = null;
        IdTokenRecord v1IdTokenToReturn = null;
        AccessTokenRecord atRecordToReturn = null;

        final List<Credential> allCredentials = getAccountCredentialCache().getCredentials();

        // First, filter down to only the refresh tokens...
        for (final Credential credential : allCredentials) {
            if (credential instanceof RefreshTokenRecord) {
                final RefreshTokenRecord rtRecord = (RefreshTokenRecord) credential;

                if (familyId.equals(rtRecord.getFamilyId())
                        && accountRecord.getEnvironment().equals(rtRecord.getEnvironment())
                        && accountRecord.getHomeAccountId().equals(rtRecord.getHomeAccountId())) {
                    rtToReturn = rtRecord;
                    break;
                }
            }
        }

        // If there's a matching IdToken, pick that up too...
        for (final Credential credential : allCredentials) {
            if (credential instanceof IdTokenRecord) {
                final IdTokenRecord idTokenRecord = (IdTokenRecord) credential;

                if (null != clientId && clientId.equals(idTokenRecord.getClientId())
                        && accountRecord.getEnvironment().equals(idTokenRecord.getEnvironment())
                        && accountRecord.getHomeAccountId().equals(idTokenRecord.getHomeAccountId())
                        && accountRecord.getRealm().equals(idTokenRecord.getRealm())) {
                    if (CredentialType.V1IdToken.name().equalsIgnoreCase(idTokenRecord.getCredentialType())) {
                        v1IdTokenToReturn = idTokenRecord;
                    } else {
                        idTokenToReturn = idTokenRecord;
                    }
                    // Do not 'break' as there may still be more IdTokens to inspect
                }
            }
        }

        if (null != target && null != authenticationScheme) {
            for (final Credential credential : allCredentials) {
                if (credential instanceof AccessTokenRecord) {
                    final AccessTokenRecord atRecord = (AccessTokenRecord) credential;

                    if (null != clientId && clientId.equals(atRecord.getClientId())
                            && accountRecord.getEnvironment().equals(atRecord.getEnvironment())
                            && accountRecord.getHomeAccountId().equals(atRecord.getHomeAccountId())
                            && accountRecord.getRealm().equals(atRecord.getRealm())
                            && targetsIntersect(target, atRecord.getTarget(), true)) {
                        if (CredentialType.AccessToken.name().equalsIgnoreCase(atRecord.getCredentialType())
                                && BearerAuthenticationSchemeInternal.SCHEME_BEARER.equalsIgnoreCase(authenticationScheme.getName())) {
                            atRecordToReturn = atRecord;
                            break;
                        } else if (CredentialType.AccessToken_With_AuthScheme.name().equalsIgnoreCase(atRecord.getCredentialType())
                                && AuthenticationSchemeFactory.isPopAuthenticationScheme(authenticationScheme)) {
                            atRecordToReturn = atRecord;
                            break;
                        }
                    }
                }
            }
        }

        final CacheRecord.CacheRecordBuilder result = CacheRecord.builder();
        result.account(accountRecord);
        result.refreshToken(rtToReturn);
        result.accessToken(atRecordToReturn);
        result.v1IdToken(v1IdTokenToReturn);
        result.idToken(idTokenToReturn);

        return result.build();
    }

    public List<ICacheRecord> loadByFamilyIdWithAggregatedAccountData(
            @NonNull final String clientId,
            @Nullable final String target,
            @NonNull final AccountRecord account,
            @Nullable final AbstractAuthenticationScheme authenticationScheme) {
        final String methodName = ":loadByFamilyIdWithAggregatedAccountData";

        final List<ICacheRecord> result = new ArrayList<>();

        // First, load our primary record...
        result.add(
                loadByFamilyId(
                        clientId,
                        target,
                        account,
                        authenticationScheme
                )
        );

        // We also want to add accounts from different realms...

        final List<AccountRecord> accountsInOtherTenants =
                new ArrayList<>(getAllTenantAccountsForAccountByClientId(clientId, account));

        Logger.info(
                TAG + methodName,
                "Found "
                        + (accountsInOtherTenants.size() - 1)
                        + " profiles for this account"
        );

        // Ignore the first element, as it will contain the same result as loadByFamilyId()....
        accountsInOtherTenants.remove(0);

        if (!accountsInOtherTenants.isEmpty()) {
            // We need the IdToken of each of these accounts... we can reuse the RT, since it is
            // an FRT...

            for (final AccountRecord accountRecord : accountsInOtherTenants) {
                // Declare our container
                final CacheRecord.CacheRecordBuilder cacheRecord = CacheRecord.builder();
                cacheRecord.account(accountRecord);
                cacheRecord.refreshToken(result.get(0).getRefreshToken());

                // Load all of the IdTokens and set as appropriate...
                final List<IdTokenRecord> idTokensForAccount = getIdTokensForAccountRecord(
                        clientId,
                        accountRecord
                );

                for (final IdTokenRecord idTokenRecord : idTokensForAccount) {
                    if (CredentialType.V1IdToken.name().equalsIgnoreCase(idTokenRecord.getCredentialType())) {
                        cacheRecord.v1IdToken(idTokenRecord);
                    } else {
                        cacheRecord.idToken(idTokenRecord);
                    }
                }

                // We can ignore the A/T, since this account isn't being authorized...
                result.add(cacheRecord.build());
            }
        }

        return result;
    }
}
