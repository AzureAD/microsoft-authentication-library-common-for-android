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

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.opentelemetry.perf.PerfOperation;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * A wrapper around {@link BrokerOAuth2TokenCache} to facilitate capturing telemery around cache operations.
 * <p>
 * This class today only captures performance data for each operation in this class and puts it on the current span.
 * For token operations, a current span is present and therefore these attributes will go on that span.
 */
// Suppressing rawtype warnings due to the generic type OAuth2Strategy, AuthorizationRequest, MicrosoftFamilyOAuth2TokenCache, MsalOAuth2TokenCache and OAuth2TokenCache
@SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE"})
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", WarningType.rawtype_warning})
public class BrokerOAuth2TokenCacheTelemetryWrapper
        <GenericOAuth2Strategy extends OAuth2Strategy,
                GenericAuthorizationRequest extends AuthorizationRequest,
                GenericTokenResponse extends MicrosoftTokenResponse,
                GenericAccount extends MicrosoftAccount,
                GenericRefreshToken extends MicrosoftRefreshToken> extends BrokerOAuth2TokenCache<GenericOAuth2Strategy, GenericAuthorizationRequest, GenericTokenResponse, GenericAccount, GenericRefreshToken> {

    private final BrokerOAuth2TokenCache mCacheToWrap;

    public BrokerOAuth2TokenCacheTelemetryWrapper(@NonNull final IPlatformComponents mPlatformComponents,
                                                  int uid,
                                                  @NonNull IBrokerApplicationMetadataCache applicationMetadataCache,
                                                  @NonNull final BrokerOAuth2TokenCache cacheToWrap) {
        super(mPlatformComponents, uid, applicationMetadataCache);
        mCacheToWrap = cacheToWrap;
    }

    @Override
    public ICacheRecord save(@NonNull GenericOAuth2Strategy oAuth2Strategy, @NonNull GenericAuthorizationRequest request, @NonNull GenericTokenResponse response) throws ClientException {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.save(oAuth2Strategy, request, response);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_save,
                    elapsedTime
            );
        }
    }

    @Override
    public List<ICacheRecord> saveAndLoadAggregatedAccountData(@NonNull GenericOAuth2Strategy oAuth2Strategy, @NonNull GenericAuthorizationRequest request, @NonNull GenericTokenResponse response) throws ClientException {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.saveAndLoadAggregatedAccountData(oAuth2Strategy, request, response);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_save_and_load_aggregated_account_data,
                    elapsedTime
            );
        }
    }

    @Override
    public ICacheRecord save(AccountRecord accountRecord, IdTokenRecord idTokenRecord) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.save(accountRecord, idTokenRecord);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_save,
                    elapsedTime
            );
        }
    }

    @Override
    public ICacheRecord load(String clientId, String applicationIdentifier, String mamEnrollmentIdentifier, String target, AccountRecord account, AbstractAuthenticationScheme authScheme) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.load(clientId, applicationIdentifier, mamEnrollmentIdentifier, target, account, authScheme);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_load,
                    elapsedTime
            );
        }
    }

    @Override
    public List<ICacheRecord> loadWithAggregatedAccountData(String clientId, String applicationIdentifier, String mamEnrollmentIdentifier, String target, AccountRecord account, AbstractAuthenticationScheme authenticationScheme) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.loadWithAggregatedAccountData(clientId, applicationIdentifier, mamEnrollmentIdentifier, target, account, authenticationScheme);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_load_aggregated_account_data,
                    elapsedTime
            );
        }
    }

    @Override
    public boolean removeCredential(Credential credential) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.removeCredential(credential);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_remove_credential,
                    elapsedTime
            );
        }
    }

    @Override
    public AccountRecord getAccount(String environment, String clientId, String homeAccountId, String realm) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAccount(environment, clientId, homeAccountId, realm);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_account,
                    elapsedTime
            );
        }
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(String environment, String clientId, String homeAccountId) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAccountsWithAggregatedAccountData(environment, clientId, homeAccountId);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_accounts_with_aggregated_account_data,
                    elapsedTime
            );
        }
    }

    @Override
    public AccountRecord getAccountByLocalAccountId(String environment, String clientId, String localAccountId) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAccountByLocalAccountId(environment, clientId, localAccountId);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_account_by_local_account_id,
                    elapsedTime
            );
        }
    }

    @Override
    public ICacheRecord getAccountWithAggregatedAccountDataByLocalAccountId(String environment, String clientId, String localAccountId) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAccountWithAggregatedAccountDataByLocalAccountId(environment, clientId, localAccountId);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_account_with_aggregated_account_data_by_local_account_id,
                    elapsedTime
            );
        }
    }

    @Override
    public List<AccountRecord> getAccounts(String environment, String clientId) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAccounts(environment, clientId);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_accounts,
                    elapsedTime
            );
        }
    }

    @Override
    public List<AccountRecord> getAllTenantAccountsForAccountByClientId(String clientId, AccountRecord accountRecord) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAllTenantAccountsForAccountByClientId(clientId, accountRecord);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_all_tenant_accounts_for_account_by_client_id,
                    elapsedTime
            );
        }
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(String environment, String clientId) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAccountsWithAggregatedAccountData(environment, clientId);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_accounts_with_aggregated_account_data,
                    elapsedTime
            );
        }
    }

    @Override
    public List<IdTokenRecord> getIdTokensForAccountRecord(String clientId, AccountRecord accountRecord) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getIdTokensForAccountRecord(clientId, accountRecord);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_id_tokens_for_account_record,
                    elapsedTime
            );
        }
    }

    @Override
    public AccountDeletionRecord removeAccount(String environment, String clientId, String homeAccountId, String realm) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.removeAccount(environment, clientId, homeAccountId, realm);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_remove_account,
                    elapsedTime
            );
        }
    }

    @Override
    public AccountDeletionRecord removeAccount(String environment, String clientId, String homeAccountId, String realm, CredentialType... typesToRemove) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.removeAccount(environment, clientId, homeAccountId, realm, typesToRemove);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_remove_account,
                    elapsedTime
            );
        }
    }

    @Override
    public void clearAll() {
        final long startTime = System.currentTimeMillis();

        try {
            mCacheToWrap.clearAll();
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_clear_all,
                    elapsedTime
            );
        }
    }

    @Override
    protected Set<String> getAllClientIds() {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAllClientIds();
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_all_client_ids,
                    elapsedTime
            );
        }
    }

    @Override
    public AccountRecord getAccountByHomeAccountId(@Nullable String environment, @NonNull String clientId, @NonNull String homeAccountId) {
        final long startTime = System.currentTimeMillis();

        try {
            return mCacheToWrap.getAccountByHomeAccountId(environment, clientId, homeAccountId);
        } finally {
            final long endTime = System.currentTimeMillis();
            final long elapsedTime = endTime - startTime;
            SpanExtension.capturePerfMeasurement(
                    PerfOperation.cache_get_account_by_home_account_id,
                    elapsedTime
            );
        }
    }
}
