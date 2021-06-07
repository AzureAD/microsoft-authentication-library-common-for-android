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
package com.microsoft.identity.common.unit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.cache.AccountDeletionRecord;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;

import java.util.List;
import java.util.Set;

public class MockOauth2TokenCache extends OAuth2TokenCache<OAuth2Strategy, AuthorizationRequest, TokenResponse> {
    public MockOauth2TokenCache() {
        super(new MockContext());
    }

    @Override
    public ICacheRecord save(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) throws ClientException {
        return null;
    }

    @Override
    public List<ICacheRecord> saveAndLoadAggregatedAccountData(OAuth2Strategy oAuth2Strategy, AuthorizationRequest request, TokenResponse response) throws ClientException {
        return null;
    }

    @Override
    public ICacheRecord save(AccountRecord accountRecord, IdTokenRecord idTokenRecord) {
        return null;
    }

    @Override
    public ICacheRecord load(String clientId, String target, AccountRecord account, AbstractAuthenticationScheme authScheme) {
        return null;
    }

    @Override
    public List<ICacheRecord> loadWithAggregatedAccountData(String clientId, String target, AccountRecord account, AbstractAuthenticationScheme authenticationScheme) {
        return null;
    }

    @Override
    public boolean removeCredential(Credential credential) {
        return false;
    }

    @Override
    public AccountRecord getAccount(String environment, String clientId, String homeAccountId, String realm) {
        return null;
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(String environment, String clientId, String homeAccountId) {
        return null;
    }

    @Override
    public AccountRecord getAccountByLocalAccountId(String environment, String clientId, String localAccountId) {
        return null;
    }

    @Override
    public ICacheRecord getAccountWithAggregatedAccountDataByLocalAccountId(String environment, String clientId, String localAccountId) {
        return null;
    }

    @Override
    public List<AccountRecord> getAccounts(String environment, String clientId) {
        return null;
    }

    @Override
    public List<AccountRecord> getAllTenantAccountsForAccountByClientId(String clientId, AccountRecord accountRecord) {
        return null;
    }

    @Override
    public List<ICacheRecord> getAccountsWithAggregatedAccountData(String environment, String clientId) {
        return null;
    }

    @Override
    public List<IdTokenRecord> getIdTokensForAccountRecord(String clientId, AccountRecord accountRecord) {
        return null;
    }

    @Override
    public AccountDeletionRecord removeAccount(String environment, String clientId, String homeAccountId, String realm) {
        return null;
    }

    @Override
    public AccountDeletionRecord removeAccount(String environment, String clientId, String homeAccountId, String realm, CredentialType... typesToRemove) {
        return null;
    }

    @Override
    public void clearAll() {

    }

    @Override
    protected Set<String> getAllClientIds() {
        return null;
    }

    @Override
    public AccountRecord getAccountByHomeAccountId(@Nullable String environment, @NonNull String clientId, @NonNull String homeAccountId) {
        return null;
    }
}
