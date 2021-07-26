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
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_HOST_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

@Builder(access = AccessLevel.PRIVATE)
@Getter
@Accessors(prefix = "m")
public class AndroidBrokerAccount implements IBrokerAccount {
    private static final String TAG = AndroidBrokerAccount.class.getSimpleName();

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

    /**
     * Logs and throw {@link ClassCastException} if it fails to parse.
     */
    @NonNull
    public static AndroidBrokerAccount cast(@NonNull final IBrokerAccount account) {
        final String methodName = ":cast";
        try {
            return (AndroidBrokerAccount) account;
        } catch (final ClassCastException e) {
            Logger.error(TAG + methodName,
                    "Expected an AndroidBrokerAccount, but got something else.", e);
            throw e;
        }
    }

    @NonNull
    public static AndroidBrokerAccount load(@NonNull final Account account) {
        return AndroidBrokerAccount.builder()
                .account(account)
                .build();
    }

    @NonNull
    public static AndroidBrokerAccount create(@NonNull final AccountManager accountManager,
                                              @NonNull final String accountName,
                                              @NonNull final String accountType) {
        final String methodName = ":create";

        Account account = getAccount(accountManager, accountName, accountType);
        if (account == null) {
            account = new Account(accountName, accountType);
            Logger.verbose(TAG + methodName, "Creating account.");
            Logger.verbosePII(TAG + methodName, ACCOUNT_NAME + ":" + account.name);
            accountManager.addAccountExplicitly(account, null, null);
        } else {
            Logger.verbose(TAG + methodName, "Account found.");
            Logger.verbosePII(TAG + methodName, ACCOUNT_NAME + ":" + account.name);
        }

        // On Android O and above, GET_ACCOUNTS permission is being replaced by accountVisibility.
        // This change is to make the account visible to both Authenticator App and Company Portal.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            accountManager.setAccountVisibility(
                    account,
                    AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
                    AccountManager.VISIBILITY_VISIBLE
            );
            accountManager.setAccountVisibility(
                    account,
                    COMPANY_PORTAL_APP_PACKAGE_NAME,
                    AccountManager.VISIBILITY_VISIBLE
            );
            accountManager.setAccountVisibility(
                    account,
                    BROKER_HOST_APP_PACKAGE_NAME,
                    AccountManager.VISIBILITY_VISIBLE
            );
        }

        return load(account);
    }

    @Nullable
    private static Account getAccount(@NonNull final AccountManager accountManager,
                                      @Nullable final String accountName,
                                      @NonNull final String accountType) {
        final String methodName = "getAccount";
        if (accountName == null) {
            return null;
        }

        final Account[] accountList = accountManager.getAccountsByType(accountType);

        if (accountList != null) {
            for (final Account existingAcct : accountList) {
                if (existingAcct.name.equalsIgnoreCase(accountName)) {
                    Logger.verbose(TAG + methodName, "Account found.");
                    return existingAcct;
                }
            }
        } else {
            Logger.verbose(TAG + methodName, "Account list null.");
        }

        return null;
    }
}
