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
package com.microsoft.identity.common.java.result;

import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.IAccountRecord;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.request.ILocalAuthenticationCallback;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.telemetry.ITelemetryAccessor;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Successful authentication result. When auth succeeds, token will be wrapped into the
 * {@link LocalAuthenticationResult} and passed back through the {@link ILocalAuthenticationCallback}.
 */
public class LocalAuthenticationResult implements ILocalAuthenticationResult, ITelemetryAccessor {

    private String mRawIdToken;
    private final AccessTokenRecord mAccessTokenRecord;
    private final IAccountRecord mAccountRecord;
    private String mRefreshToken;
    private String mFamilyId;
    private String mSpeRing;
    private String mRefreshTokenAge;
    private List<ICacheRecord> mCompleteResultFromCache;
    private boolean mServicedFromCache;
    private String mCorrelationId;
    private final List<Map<String, String>> mTelemetry = new ArrayList<>();

    private static final String TAG = LocalAuthenticationResult.class.getSimpleName();

    public LocalAuthenticationResult(@NonNull final ICacheRecord lastAuthorized,
                                     @NonNull final List<ICacheRecord> completeResultFromCache,
                                     @NonNull final SdkType sdkType,
                                     final boolean isServicedFromCache) {
        this(lastAuthorized, sdkType);
        mCompleteResultFromCache = completeResultFromCache;
        mServicedFromCache = isServicedFromCache;
    }

    private LocalAuthenticationResult(@NonNull final ICacheRecord cacheRecord, @NonNull SdkType sdkType) {
        mAccessTokenRecord = cacheRecord.getAccessToken();
        mAccountRecord = cacheRecord.getAccount();

        if (cacheRecord.getRefreshToken() != null) {
            mRefreshToken = cacheRecord.getRefreshToken().getSecret();
        }

        final IdTokenRecord idTokenRecord = sdkType == SdkType.ADAL ?
                cacheRecord.getV1IdToken() :
                cacheRecord.getIdToken();
        if (idTokenRecord != null) {
            mRawIdToken = idTokenRecord.getSecret();
            Logger.info(TAG, "Id Token type: " +
                    idTokenRecord.getCredentialType());
        } else if (cacheRecord.getV1IdToken() != null) {
            // For all AAD requests, we hit the V2 endpoint, so the id token returned will be of version 2.0 (V2 )
            // However for B2C we might get back v1 id tokens, so check if getV1IdToken() is not null and add it
            Logger.info(TAG, "V1 Id Token returned here, ");
            mRawIdToken = cacheRecord.getV1IdToken().getSecret();
        }

        Logger.info(
                TAG,
                "Constructing LocalAuthentication result"
                        + ", AccessTokenRecord null: " + (mAccessTokenRecord == null)
                        + ", AccountRecord null: " + (mAccountRecord == null)
                        + ", RefreshTokenRecord null or empty: " + StringUtil.isNullOrEmpty(mRefreshToken)
                        + ", IdTokenRecord null: " + (idTokenRecord == null)
        );

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

    @NonNull
    @Override
    public String getRefreshToken() {
        return mRefreshToken;
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

    @Nullable
    @Override
    public String getSpeRing() {
        return mSpeRing;
    }

    /**
     * Sets the SPE Ring.
     *
     * @param speRing The SPE Ring to set.
     */
    public void setSpeRing(final String speRing) {
        mSpeRing = speRing;
    }

    @Nullable
    @Override
    public String getRefreshTokenAge() {
        return mRefreshTokenAge;
    }

    @Nullable
    @Override
    public String getFamilyId() {
        return mFamilyId;
    }

    @Override
    public List<ICacheRecord> getCacheRecordWithTenantProfileData() {
        return mCompleteResultFromCache;
    }

    /**
     * Sets the refresh token age.
     *
     * @param refreshTokenAge The refresh token age to set.
     */
    public void setRefreshTokenAge(final String refreshTokenAge) {
        mRefreshTokenAge = refreshTokenAge;
    }

    @Override
    @NonNull
    public AccessTokenRecord getAccessTokenRecord() {
        return mAccessTokenRecord;
    }


    @Override
    public boolean isServicedFromCache() {
        return mServicedFromCache;
    }

    public void setCorrelationId(@NonNull final String correlationId) {
        mCorrelationId = correlationId;
    }

    @Nullable
    @Override
    public String getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * Set the telemetry on local authentication result.
     *
     * @param telemetry the {@link List<Map<String, String>>} containing telemetry data
     */
    public void setTelemetry(@NonNull final List<Map<String, String>> telemetry) {
        mTelemetry.addAll(telemetry);
    }

    @Override
    public List<Map<String, String>> getTelemetry() {
        return mTelemetry;
    }
}
