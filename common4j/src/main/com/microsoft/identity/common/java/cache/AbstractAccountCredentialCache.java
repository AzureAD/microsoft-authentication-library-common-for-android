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

import static com.microsoft.identity.common.java.AuthenticationConstants.DEFAULT_SCOPES;

import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeWithClientKeyInternal;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.PrimaryRefreshTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public abstract class AbstractAccountCredentialCache implements IAccountCredentialCache {

    private static final String TAG = AbstractAccountCredentialCache.class.getSimpleName();
    private static final String NEW_LINE = "\n";

    // SharedPreferences used to store Accounts and Credentials
    protected final INameValueStorage<String> mSharedPreferencesFileManager;

    /**
     * Constructor of AbstractAccountCredentialCache.
     * @param sharedPreferencesFileManager INameValueStorage
     */
    protected AbstractAccountCredentialCache(@NonNull final INameValueStorage<String> sharedPreferencesFileManager) {
        mSharedPreferencesFileManager = sharedPreferencesFileManager;
    }

    @Nullable
    protected Class<? extends Credential> getTargetClassForCredentialType(@Nullable final String cacheKey,
                                                                          @NonNull final CredentialType targetType) {
        Class<? extends Credential> credentialClass = null;

        switch (targetType) {
            case AccessToken:
            case AccessToken_With_AuthScheme:
                credentialClass = AccessTokenRecord.class;
                break;
            case RefreshToken:
                credentialClass = RefreshTokenRecord.class;
                break;
            case IdToken:
            case V1IdToken:
                credentialClass = IdTokenRecord.class;
                break;
            case PrimaryRefreshToken:
                credentialClass = PrimaryRefreshTokenRecord.class;
                break;
            default:
                Logger.warn(TAG, "Could not match CredentialType to class. "
                        + "Did you forget to update this method with a new type?");
                if (null != cacheKey) {
                    Logger.warnPII(TAG, "Sought key was: [" + cacheKey + "]");
                }
        }

        return credentialClass;
    }

    @NonNull
    protected List<AccountRecord> getAccountsFilteredByInternal(@Nullable final String homeAccountId,
                                                                @Nullable final String environment,
                                                                @Nullable final String realm,
                                                                @NonNull final List<AccountRecord> allAccounts) {
        final boolean mustMatchOnHomeAccountId = !StringUtil.isNullOrEmpty(homeAccountId);
        final boolean mustMatchOnEnvironment = !StringUtil.isNullOrEmpty(environment);
        final boolean mustMatchOnRealm = !StringUtil.isNullOrEmpty(realm);

        Logger.verbose(
                TAG,
                "Account lookup filtered by home_account_id? [" + mustMatchOnHomeAccountId + "]"
                        + NEW_LINE
                        + "Account lookup filtered by realm? [" + mustMatchOnRealm + "]"
        );

        final List<AccountRecord> matchingAccounts = new ArrayList<>();

        for (final AccountRecord account : allAccounts) {
            boolean matches = true;

            if (mustMatchOnHomeAccountId) {
                matches = StringUtil.equalsIgnoreCaseTrimBoth(homeAccountId, account.getHomeAccountId());
            }

            if (mustMatchOnEnvironment) {
                matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(environment, account.getEnvironment());
            }

            if (mustMatchOnRealm) {
                matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(realm, account.getRealm());
            }

            if (matches) {
                matchingAccounts.add(account);
            }
        }

        Logger.verbose(
                TAG,
                "Found [" + matchingAccounts.size() + "] matching accounts"
        );

        return matchingAccounts;
    }

    protected List<Credential> getCredentialsFilteredByInternal(@NonNull final List<Credential> allCredentials,
                                                                @Nullable final String homeAccountId,
                                                                @Nullable final String environment,
                                                                @Nullable final CredentialType credentialType,
                                                                @Nullable final String clientId,
                                                                @Nullable final String applicationIdentifier,
                                                                @Nullable final String mamEnrollmentIdentifier,
                                                                @Nullable final String realm,
                                                                @Nullable final String target,
                                                                @Nullable final String authScheme,
                                                                @Nullable final String requestedClaims,
                                                                @Nullable final String kid,
                                                                boolean mustMatchExactClaims) {
        final boolean mustMatchOnEnvironment = !StringUtil.isNullOrEmpty(environment);
        final boolean mustMatchOnHomeAccountId = !StringUtil.isNullOrEmpty(homeAccountId);
        final boolean mustMatchOnRealm = !StringUtil.isNullOrEmpty(realm);
        final boolean mustMatchOnTarget = !StringUtil.isNullOrEmpty(target);
        final boolean mustMatchOnClientId = !StringUtil.isNullOrEmpty(clientId);
        final boolean mustMatchOnApplicationIdentifier = !StringUtil.isNullOrEmpty(applicationIdentifier);
        final boolean mustMatchOnMamEnrollmentIdentifier = !StringUtil.isNullOrEmpty(mamEnrollmentIdentifier);
        final boolean mustMatchOnCredentialType = null != credentialType;
        final boolean mustMatchOnAuthScheme = mustMatchOnCredentialType
                && !StringUtil.isNullOrEmpty(authScheme)
                && credentialType == CredentialType.AccessToken_With_AuthScheme;
        final boolean mustMatchOnKid = !StringUtil.isNullOrEmpty(kid);
        final boolean mustMatchOnRequestedClaims = !StringUtil.isNullOrEmpty(requestedClaims);

        Logger.verbose(
                TAG,
                "Credential lookup filtered by home_account_id? [" + mustMatchOnHomeAccountId + "]"
                        + NEW_LINE
                        + "Credential lookup filtered by realm? [" + mustMatchOnRealm + "]"
                        + NEW_LINE
                        + "Credential lookup filtered by target? [" + mustMatchOnTarget + "]"
                        + NEW_LINE
                        + "Credential lookup filtered by clientId? [" + mustMatchOnClientId + "]"
                        + NEW_LINE
                        + "Credential lookup filtered by applicationIdentifier? [" + mustMatchOnApplicationIdentifier + "]"
                        + NEW_LINE
                        + "Credential lookup filtered by mamEnrollmentIdentifier? [" + mustMatchOnMamEnrollmentIdentifier + "]"
                        + NEW_LINE
                        + "Credential lookup filtered by credential type? [" + mustMatchOnCredentialType + "]"
                        + NEW_LINE
                        + "Credential lookup filtered by auth scheme? [" + mustMatchOnAuthScheme + "]"
                        + NEW_LINE
                        + "Credential lookup filtered by requested claims? [" + mustMatchOnRequestedClaims + "]"
        );

        final List<Credential> matchingCredentials = new ArrayList<>();

        for (final Credential credential : allCredentials) {
            boolean matches = true;

            if (mustMatchOnHomeAccountId) {
                matches = StringUtil.equalsIgnoreCaseTrimBoth(homeAccountId, credential.getHomeAccountId());
            }

            if (mustMatchOnEnvironment) {
                matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(environment, credential.getEnvironment());
            }

            if (mustMatchOnCredentialType) {
                matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(credentialType.name(), credential.getCredentialType());
            }

            if (mustMatchOnClientId) {
                matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(clientId, credential.getClientId());
            }

            if (mustMatchOnApplicationIdentifier) {
                if (credential instanceof AccessTokenRecord) {
                    final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                    matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(applicationIdentifier, accessToken.getApplicationIdentifier());
                } else {
                    Logger.verbose(TAG, "Query specified applicationIdentifier match, but credential type does not have application identifier");
                }
            }

            if (mustMatchOnMamEnrollmentIdentifier) {
                if (credential instanceof AccessTokenRecord) {
                    final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                    matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(mamEnrollmentIdentifier, accessToken.getMamEnrollmentIdentifier());
                } else {
                    Logger.verbose(TAG, "Query specified mamEnrollmentIdentifier match, but credential type does not have MAM enrollment identifier");
                }
            }

            if (mustMatchOnRealm && credential instanceof AccessTokenRecord) {
                final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(realm, accessToken.getRealm());
            }

            if (mustMatchOnRealm && credential instanceof IdTokenRecord) {
                final IdTokenRecord idToken = (IdTokenRecord) credential;
                matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(realm, idToken.getRealm());
            }

            if (mustMatchOnTarget) {
                if (credential instanceof AccessTokenRecord) {
                    final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                    matches = matches && targetsIntersect(target, accessToken.getTarget(), true);
                } else if (credential instanceof RefreshTokenRecord) {
                    final RefreshTokenRecord refreshToken = (RefreshTokenRecord) credential;
                    matches = matches && targetsIntersect(target, refreshToken.getTarget(), true);
                } else {
                    Logger.verbose(TAG, "Query specified target-match, but no target to match.");
                }
            }

            if (mustMatchOnAuthScheme && credential instanceof AccessTokenRecord) {
                final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                String atType = accessToken.getAccessTokenType();

                if (null != atType) {
                    atType = atType.trim();
                }

                if (TokenRequest.TokenType.POP.equalsIgnoreCase(atType)) {
                    matches = matches && (
                            authScheme.equalsIgnoreCase(PopAuthenticationSchemeWithClientKeyInternal.SCHEME_POP_WITH_CLIENT_KEY)
                                    || authScheme.equalsIgnoreCase(PopAuthenticationSchemeInternal.SCHEME_POP)
                    );
                } else {
                    matches = matches && authScheme.equalsIgnoreCase(atType);
                }
            }

            if(mustMatchOnKid && credential instanceof AccessTokenRecord) {
                final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                matches = matches && kid.equalsIgnoreCase(accessToken.getKid());
            }

            if (mustMatchOnRequestedClaims || mustMatchExactClaims) {
                if (credential instanceof AccessTokenRecord) {
                    final AccessTokenRecord accessToken = (AccessTokenRecord) credential;
                    matches = matches && StringUtil.equalsIgnoreCaseTrimBoth(requestedClaims, accessToken.getRequestedClaims());
                } else {
                    Logger.verbose(TAG, "Query specified requested_claims-match, but attempted to match with non-AT credential type.");
                }
            }

            if (matches) {
                matchingCredentials.add(credential);
            }
        }

        return matchingCredentials;
    }

    /**
     * Examines the intersections of the provided targets (scopes).
     *
     * @param targetToMatch     The target value[s] our cache-query is looking for.
     * @param credentialTarget  The target against which our sought value will be compared.
     * @param omitDefaultScopes True if MSAL's default scopes should be considered in this lookup.
     *                          False otherwise.
     * @return True, if the credentialTarget contains all of the targets (scopes) declared by
     * targetToMatch. False otherwise.
     */
    static boolean targetsIntersect(@NonNull final String targetToMatch,
                                    @NonNull final String credentialTarget,
                                    final boolean omitDefaultScopes) {
        // The credentialTarget must contain all of the scopes in the targetToMatch
        // It may contain more, but it must contain minimally those
        // Matching is case-insensitive
        final String splitCriteria = "\\s+";
        final String[] targetToMatchArray = targetToMatch.trim().split(splitCriteria);
        final String[] credentialTargetArray = credentialTarget.trim().split(splitCriteria);

        // Declare Sets to contain these scopes
        final Set<String> soughtTargetSet = new HashSet<>();
        final Set<String> credentialTargetSet = new HashSet<>();

        // Add the array values to these sets, lowercasing them
        for (final String target : targetToMatchArray) {
            soughtTargetSet.add(target.toLowerCase(Locale.ROOT));
        }

        for (final String target : credentialTargetArray) {
            credentialTargetSet.add(target.toLowerCase(Locale.ROOT));
        }

        if (omitDefaultScopes) {
            soughtTargetSet.removeAll(DEFAULT_SCOPES);
            credentialTargetSet.removeAll(DEFAULT_SCOPES);
        }

        return credentialTargetSet.containsAll(soughtTargetSet);
    }
}
