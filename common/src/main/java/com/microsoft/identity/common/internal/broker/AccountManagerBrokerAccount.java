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
package com.microsoft.identity.common.internal.broker;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Build;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.broker.IBrokerAccount;
import com.microsoft.identity.common.logging.Logger;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

/**
 * An instance of {@link IBrokerAccount} which contains an Android's {@link AccountManager} account.
 **/
@Getter
@Accessors(prefix = "m")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class AccountManagerBrokerAccount implements IBrokerAccount {
    private static final String TAG = AccountManagerBrokerAccount.class.getSimpleName();

    /**
     * Android's {@link AccountManager} account.
     */
    @NonNull
    private final Account mAccount;

    @Override
    @NonNull
    public String getUsername() {
        return mAccount.name;
    }

    @NonNull
    @Override
    public String getType() {
        return mAccount.type;
    }

    /**
     * Cast an {@link IBrokerAccount} into {@link AccountManagerBrokerAccount}.
     */
    @NonNull
    public static AccountManagerBrokerAccount cast(@NonNull final AccountManager accountManager,
                                                   @NonNull final IBrokerAccount account) {
        if (account instanceof AccountManagerBrokerAccount){
            return (AccountManagerBrokerAccount) account;
        }

        // Cannot cast, load/create Account Manager Account and wrap it with AndroidManagerBrokerAccount.
        return create(accountManager, account.getUsername(), account.getType());
    }

    @NonNull
    public static AccountManagerBrokerAccount adapt(@NonNull final Account account) {
        return new AccountManagerBrokerAccount(account);
    }

    @NonNull
    public static AccountManagerBrokerAccount create(@NonNull final AccountManager accountManager,
                                                     @NonNull final String accountName,
                                                     @NonNull final String accountType) {
        final String methodTag = TAG + ":create";

        Account account = getAccount(accountManager, accountName, accountType);
        if (account == null) {
            account = new Account(accountName, accountType);
            Logger.verbose(methodTag, "Creating account.");
            Logger.verbosePII(methodTag, "Creating account with name :" + account.name);
            accountManager.addAccountExplicitly(account, null, null);
        }

        setVisibility(accountManager, account, AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);
        setVisibility(accountManager, account, COMPANY_PORTAL_APP_PACKAGE_NAME);

        if (BrokerData.getShouldTrustDebugBrokers()){
            setVisibility(accountManager, account, BrokerData.getDebugMockCp().getPackageName());
            setVisibility(accountManager, account, BrokerData.getDebugMockAuthApp().getPackageName());
            setVisibility(accountManager, account, BrokerData.getDebugBrokerHost().getPackageName());
        }

        return adapt(account);
    }

    // On Android O and above, GET_ACCOUNTS permission is being replaced by accountVisibility.
    // This change is to make the account visible to both Authenticator App and Company Portal.
    private static void setVisibility(@NonNull final AccountManager accountManager,
                                      @NonNull final Account account,
                                      @NonNull final String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (accountManager.getAccountVisibility(account, packageName)
                    != AccountManager.VISIBILITY_VISIBLE) {
                accountManager.setAccountVisibility(
                        account,
                        packageName,
                        AccountManager.VISIBILITY_VISIBLE
                );
            }
        }
    }

    @Nullable
    private static Account getAccount(@NonNull final AccountManager accountManager,
                                      @NonNull final String accountName,
                                      @NonNull final String accountType) {
        final String methodTag = TAG + ":getAccount";
        final Account[] accountList = accountManager.getAccountsByType(accountType);

        if (accountList != null) {
            for (final Account existingAcct : accountList) {
                if (existingAcct.name.equalsIgnoreCase(accountName)) {
                    return existingAcct;
                }
            }
        }

        Logger.verbose(methodTag, "Account not found.");
        return null;
    }
}
