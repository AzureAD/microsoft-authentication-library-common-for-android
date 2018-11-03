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
package com.microsoft.identity.common.internal.cache;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.getAccountsFilteredByInternal;
import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.getCredentialsFilteredByInternal;
import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.getTargetClassForCredentialType;

public class AccountManagerAccountCredentalCache implements IAccountCredentialCache {

    private static final String TAG = AccountManagerAccountCredentalCache.class.getSimpleName();
    private static final String ACCOUNT_TYPE = "com.microsoft.workaccount";

    private static class AccountUserDataKeys {
        static final String ACCOUNT_JSON_REPRESENTATION = "account_json";
        static final String ACCOUNT_CREDENTIALS = "account_credentials";
    }

    private final AccountManager mAccountManager;
    private final ICacheKeyValueDelegate mCacheValueDelegate;
    private final IStorageHelper mStorageHelper;

    public AccountManagerAccountCredentalCache(@NonNull final Context context,
                                               @NonNull final ICacheKeyValueDelegate cacheKeyValueDelegate,
                                               @Nullable final IStorageHelper storageHelper) {
        mAccountManager = AccountManager.get(context);
        mCacheValueDelegate = cacheKeyValueDelegate;
        mStorageHelper = storageHelper;
    }

    @Override
    public void saveAccount(final AccountRecord accountRecord) {
        final Account account = createAccount(accountRecord);
        // TODO if this Account already exists in the AccountManager, see that any already-associated Credentials are moved to the new record.
        mAccountManager.setUserData(
                account,
                AccountUserDataKeys.ACCOUNT_JSON_REPRESENTATION,
                serialize(accountRecord)
        );
    }

    @Override
    public void saveCredential(final Credential credential) {
        // Find the Account to 'own' this credential
        final AccountRecord account = getAccountForCredential(credential);

        // Retrieve the List of credentials associated to this account
        List<Credential> accountCredentials = getCredentialsForAccount(account);

        // If there are no credentials, create a place to store them
        if (null == accountCredentials) {
            accountCredentials = new ArrayList<>();
        }

        // Add the credentials we're about to save to the List of credentials to save for this account
        accountCredentials.add(credential);

        // Re-save the Account with the associated credential[s]
        saveAccountWithCredentialData(
                account,
                accountCredentials
        );
    }

    @Override
    public AccountRecord getAccount(final String cacheKey) {
        AccountRecord result = null;

        final List<AccountRecord> accounts = getAccounts();

        for (final AccountRecord account : accounts) {
            final String currentCacheKey = mCacheValueDelegate.generateCacheKey(account);

            if (cacheKey.equals(currentCacheKey)) {
                result = account;
                break;
            }
        }

        return result;
    }

    @Override
    public Credential getCredential(final String cacheKey) {
        final List<AccountRecord> accounts = getAccounts();

        for (final AccountRecord account : accounts) {
            final List<Credential> credentials = getCredentialsForAccount(account);

            for (final Credential credential : credentials) {
                final String currentCacheKey = mCacheValueDelegate.generateCacheKey(credential);

                if (cacheKey.equals(currentCacheKey)) {
                    return credential;
                }
            }
        }

        return null;
    }

    @Override
    public List<AccountRecord> getAccounts() {
        final List<AccountRecord> result = new ArrayList<>();
        final Account[] accounts = mAccountManager.getAccountsByType(ACCOUNT_TYPE);

        for (final Account account : accounts) {
            // Deserialize the account object and add it to the List
            final String accountJson = mAccountManager.getUserData(
                    account,
                    AccountUserDataKeys.ACCOUNT_JSON_REPRESENTATION
            );

            if (null != accountJson) {
                result.add(
                        mCacheValueDelegate.<AccountRecord>fromCacheValue(
                                accountJson,
                                AccountRecord.class
                        )
                );
            }
        }

        return result;
    }

    @Override
    public List<AccountRecord> getAccountsFilteredBy(@Nullable final String homeAccountId,
                                                     @Nullable final String environment,
                                                     @Nullable final String realm) {
        final List<AccountRecord> allAccounts = getAccounts();

        return getAccountsFilteredByInternal(
                homeAccountId,
                environment,
                realm,
                allAccounts
        );
    }

    @Override
    public List<Credential> getCredentials() {
        final List<Credential> allCredentials = new ArrayList<>();
        final List<AccountRecord> allAccounts = getAccounts();

        for (final AccountRecord accountRecord : allAccounts) {
            allCredentials.addAll(getCredentialsForAccount(accountRecord));
        }

        return allCredentials;
    }

    @Override
    public List<Credential> getCredentialsFilteredBy(@Nullable final String homeAccountId,
                                                     @Nullable final String environment,
                                                     @NonNull final CredentialType credentialType,
                                                     @NonNull final String clientId,
                                                     @Nullable final String realm,
                                                     @Nullable final String target) {
        return getCredentialsFilteredByInternal(
                homeAccountId,
                environment,
                credentialType,
                clientId,
                realm,
                target,
                getCredentials()
        );
    }

    @Override
    public boolean removeAccount(final AccountRecord accountToRemove) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return mAccountManager.removeAccountExplicitly(createAccount(accountToRemove));
        } else {
            final Boolean[] result = new Boolean[1];
            final CountDownLatch latch = new CountDownLatch(1);
            mAccountManager.removeAccount(
                    createAccount(accountToRemove),
                    new AccountManagerCallback<Boolean>() {
                        @Override
                        public void run(AccountManagerFuture<Boolean> future) {
                            try {
                                result[0] = future.getResult();
                                latch.countDown();
                            } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    null
            );
            try {
                latch.await();
                return result[0];
            } catch (InterruptedException e) {
                return false;
            }
        }
    }

    @Override
    public boolean removeCredential(final Credential credentialToRemove) {
        final String targetCacheKey = mCacheValueDelegate.generateCacheKey(credentialToRemove);
        final AccountRecord accountRecord = getAccountForCredential(credentialToRemove);
        final List<Credential> accountCredentials = getCredentialsForAccount(accountRecord);
        boolean removed = false;

        for (final Iterator<Credential> iterator = accountCredentials.iterator(); iterator.hasNext(); ) {
            final Credential currentCredential = iterator.next();
            final String currentCacheKey = mCacheValueDelegate.generateCacheKey(currentCredential);

            if (targetCacheKey.equals(currentCacheKey)) {
                iterator.remove();
                removed = true;
                break;
            }
        }

        saveAccountWithCredentialData(accountRecord, accountCredentials);

        return removed;
    }

    @Override
    public void clearAll() {
        for (final AccountRecord accountRecord : getAccounts()) {
            removeAccount(accountRecord);
        }
    }

    private void saveAccountWithCredentialData(@NonNull final AccountRecord account,
                                               @NonNull final List<Credential> accountCredentials) {
        mAccountManager.setUserData(
                createAccount(account),
                AccountUserDataKeys.ACCOUNT_CREDENTIALS,
                serialize(accountCredentials)
        );
    }

    private Account createAccount(final AccountRecord accountRecord) {
        return new Account(
                accountRecord.getUsername(),
                ACCOUNT_TYPE
        );
    }

    private String serialize(@NonNull final AccountRecord accountRecord) {
        final String serializedRepresentation = mCacheValueDelegate.generateCacheValue(
                accountRecord
        );
        try {
            return null != mStorageHelper
                    ? mStorageHelper.encrypt(serializedRepresentation)
                    : serializedRepresentation;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to serialize AccountRecord");
        }
    }

    private String serialize(@NonNull final List<Credential> accountCredentials) {
        final JsonArray credentials = new JsonArray();

        for (final Credential credential : accountCredentials) {
            credentials.add(
                    mCacheValueDelegate.generateCacheValue(
                            credential
                    )
            );
        }

        try {
            return null != mStorageHelper
                    ? mStorageHelper.encrypt(credentials.toString())
                    : credentials.toString();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to serialize AccountRecord");
        }
    }

    private List<Credential> getCredentialsForAccount(@NonNull final AccountRecord accountRecord) {
        final List<Credential> credentials = new ArrayList<>();

        // Create the AccountManager representation of this record
        final Account account = createAccount(accountRecord);

        // Get the associated 'user data'
        String accountCredentials = mAccountManager.getUserData(
                account,
                AccountUserDataKeys.ACCOUNT_CREDENTIALS
        );

        if (null != mStorageHelper) {
            try {
                accountCredentials = mStorageHelper.decrypt(accountCredentials);
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException("Failed to serialize AccountRecord");
            }
        }

        try {
            final JsonArray credentialArray =
                    new JsonParser()
                            .parse(accountCredentials)
                            .getAsJsonArray();

            for (final JsonElement element : credentialArray) {
                final JsonObject credentialJsonObj = element.getAsJsonObject();

                final CredentialType type = CredentialType.fromString(
                        credentialJsonObj.get(
                                Credential.SerializedNames.CREDENTIAL_TYPE
                        ).toString()
                );

                if (null != type) {
                    credentials.add(
                            (Credential) mCacheValueDelegate.fromCacheValue(
                                    element.toString(),
                                    getTargetClassForCredentialType(
                                            null, // Not needed
                                            type
                                    )
                            )
                    );
                }
            }
        } catch (JsonSyntaxException e) {
            final String errMsg = "Failed to deserialize JsonArray of credentials";
            Logger.error(
                    TAG,
                    errMsg,
                    null
            );
        }

        return credentials;
    }

    private AccountRecord getAccountForCredential(@NonNull final Credential credential) {
        final String soughtKey = mCacheValueDelegate.generateCacheKey(credential);
        final List<AccountRecord> accountRecords = getAccounts();

        for (final AccountRecord record : accountRecords) {
            final List<Credential> accountCredentials = getCredentialsForAccount(record);

            for (final Credential credentialBase : accountCredentials) {
                final String currentKey = mCacheValueDelegate.generateCacheKey((Credential) credentialBase);

                if (soughtKey.equals(currentKey)) {
                    return record;
                }
            }
        }

        return null;
    }
}
