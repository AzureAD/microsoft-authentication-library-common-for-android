//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.result;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.request.ILocalAuthenticationCallback;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Successful authentication result. When auth succeeds, token will be wrapped into the
 * {@link LocalAuthenticationResult} and passed back through the {@link ILocalAuthenticationCallback}.
 */
public class LocalAuthenticationResult implements ILocalAuthenticationResult {


    private final String mRawIdToken;
    private final AccessTokenRecord mAccessTokenRecord;
    private final IAccountRecord mAccountRecord;

    public LocalAuthenticationResult(@NonNull final ICacheRecord cacheRecord) {
        mAccessTokenRecord = cacheRecord.getAccessToken();
        mRawIdToken = cacheRecord.getIdToken().getSecret();
        mAccountRecord = cacheRecord.getAccount();
    }

    public LocalAuthenticationResult(@NonNull AccessTokenRecord accessTokenRecord,
                                     @Nullable String rawIdToken,
                                     @NonNull IAccountRecord accountRecord) {
        mAccessTokenRecord = accessTokenRecord;
        mRawIdToken = rawIdToken;
        mAccountRecord = accountRecord;

    }

    @Override
    @NonNull
    public String getAccessToken() {
        return mAccessTokenRecord.getSecret();
    }

    @Override
    @NonNull
    public Date getExpiresOn() {
        final Date expiresOn;

        expiresOn = new Date(
                TimeUnit.SECONDS.toMillis(
                        Long.parseLong(
                                mAccessTokenRecord.getExpiresOn()
                        )
                )
        );

        return expiresOn;
    }

    @Override
    @Nullable
    public String getTenantId() {
        return mAccessTokenRecord.getRealm();
    }

    @Override
    @NonNull
    public String getUniqueId() {
        return mAccessTokenRecord.getHomeAccountId();
    }

    @Override
    @Nullable
    public String getIdToken() {
        return mRawIdToken;
    }

    @Override
    @NonNull
    public IAccountRecord getAccountRecord() {
        return mAccountRecord;
    }

    @Override
    @NonNull
    public String[] getScope() {
        return mAccessTokenRecord.getTarget().split("\\s");
    }

    @Override
    @NonNull
    public AccessTokenRecord getAccessTokenRecord() {
        return mAccessTokenRecord;
    }
}