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
import com.microsoft.identity.common.java.telemetry.ClientDataInfo;
import com.microsoft.identity.common.java.telemetry.ITelemetryAccessor;
import com.microsoft.identity.common.java.util.ISensitiveData;
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
public class LocalAuthenticationResult implements ILocalAuthenticationResult, ITelemetryAccessor, ISensitiveData {

    private String mRawIdToken;
    private AccessTokenRecord mAccessTokenRecord;
    private final IAccountRecord mAccountRecord;
    private String mRefreshToken;
    private String mFamilyId;
    private String mSpeRing;
    private String mRefreshTokenAge;
    @Nullable
    private ClientDataInfo mClientDataInfo;
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
            mFamilyId = cacheRecord.getRefreshToken().getFamilyId();
        }

        final IdTokenRecord idTokenRecord = sdkType == SdkType.ADAL ?
                cacheRecord.getV1IdToken() :
                cacheRecord.getIdToken();
        if (idTokenRecord != null) {
            mRawIdToken = idTokenRecord.getSecret();
            Logger.verbose(TAG, "Id Token type: " +
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
    public String getAccessToken() {
        return mAccessTokenRecord == null ? null : mAccessTokenRecord.getSecret();
    }

    @Override
    public Date getExpiresOn() {
        if (mAccessTokenRecord == null) {
            return null;
        }

        return new Date(
                TimeUnit.SECONDS.toMillis(
                        Long.parseLong(
                                mAccessTokenRecord.getExpiresOn()
                        )
                )
        );
    }

    @Override
    @Nullable
    public String getTenantId() {
        return mAccessTokenRecord == null ? null : mAccessTokenRecord.getRealm();
    }

    @Override
    public String getUniqueId() {
        return mAccessTokenRecord == null ? null : mAccessTokenRecord.getHomeAccountId();
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
    public String[] getScope() {
        return mAccessTokenRecord == null ? new String[0] : mAccessTokenRecord.getTarget().split("\\s");
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

    /**
     * Gets the server client data info parsed from the {@code x-ms-clientdata} header, which can
     * be returned by either the {@code /authorize} or {@code /token} endpoint.
     *
     * @return The ClientDataInfo, or null if not available.
     */
    @Nullable
    public ClientDataInfo getClientDataInfo() {
        return mClientDataInfo;
    }

    /**
     * Sets the server client data info.
     *
     * @param clientDataInfo The ClientDataInfo to set.
     */
    public void setClientDataInfo(@Nullable final ClientDataInfo clientDataInfo) {
        mClientDataInfo = clientDataInfo;
    }

    @Override
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
     * @param telemetry the telemetry data
     */
    public void setTelemetry(@NonNull final List<Map<String, String>> telemetry) {
        mTelemetry.addAll(telemetry);
    }

    @Override
    public List<Map<String, String>> getTelemetry() {
        return mTelemetry;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Nulls out the access token record, raw ID token, refresh token, and releases the cache record list reference
     * so they become eligible for garbage collection. After this call, this result should not
     * be used to retrieve token values.
     */
    @Override
    public void clearSensitiveData() {
        mRawIdToken = null;
        mRefreshToken = null;
        mAccessTokenRecord = null;
        mCompleteResultFromCache = null;
    }
}
