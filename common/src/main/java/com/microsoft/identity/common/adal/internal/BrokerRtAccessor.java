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
package com.microsoft.identity.common.adal.internal;

import android.content.Context;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.dto.AccountRecord;

import static com.microsoft.identity.common.exception.ClientException.TOKEN_CACHE_ITEM_NOT_FOUND;

public final class BrokerRtAccessor {

    private final MsalOAuth2TokenCache mTokenCache;
    private final Context mContext;

    public BrokerRtAccessor(@NonNull final MsalOAuth2TokenCache cache,
                            @NonNull final Context context) {
        mTokenCache = cache;
        mContext = context;
    }

    public String getBrokerRt(String accountObjectId) throws ClientException {
        if (!AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equals(mContext.getPackageName()) &&
                !AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME.equals(mContext.getPackageName())) {
            throw new ClientException("This can only be invoked by Broker apps.");
        }

        if (!new BrokerValidator(mContext).verifySignature(mContext.getPackageName())) {
            throw new ClientException("This can only be invoked by apps with a valid signature hash.");
        }

        final ICacheRecord cacheRecord = getCacheRecordForIdentifier(accountObjectId);
        return cacheRecord.getRefreshToken().getSecret();
    }

    private ICacheRecord getCacheRecordForIdentifier(@NonNull final String accountObjectId) throws ClientException {
        final AccountRecord localAccountRecord = mTokenCache.getAccountByLocalAccountId(
                null,
                AuthenticationConstants.Broker.BROKER_CLIENT_ID,
                accountObjectId
        );

        // Check it's not null
        if (null == localAccountRecord) {
            // Unrecognized identifier, cannot supply a token.
            throw new ClientException(TOKEN_CACHE_ITEM_NOT_FOUND);
        }

        return mTokenCache.load(
                AuthenticationConstants.Broker.BROKER_CLIENT_ID,
                null, // wildcard (*)
                localAccountRecord,
                new BearerAuthenticationSchemeInternal() // Auth scheme is inconsequential - only using RT
        );
    }
}
