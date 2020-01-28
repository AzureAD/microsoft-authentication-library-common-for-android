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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.IAccountRecord;

import java.util.Date;
import java.util.List;

/**
 * Interface to wrap successful authentication result. When auth succeeds, token will be wrapped into the
 * {@link ILocalAuthenticationResult}
 */
public interface ILocalAuthenticationResult {

    /**
     * @return The access token requested.
     */
    @NonNull
    String getAccessToken();

    /**
     * @return The expiration time of the access token returned in the Token property.
     * This value is calculated based on current UTC time measured locally and the value expiresIn returned from the
     * service.
     */
    @NonNull
    Date getExpiresOn();

    /**
     * @return A unique tenant identifier that was used in token acquisiton. Could be null if tenant information is not
     * returned by the service.
     */
    @Nullable
    String getTenantId();

    /**
     * @return The unique identifier of the user.
     */
    @NonNull
    String getUniqueId();

    /**
     * @return The refresh token
     */
    @NonNull
    String getRefreshToken();

    /**
     * @return The id token returned by the service or null if no id token is returned.
     */
    @Nullable
    String getIdToken();

    /**
     * Gets the AccountRecord.
     *
     * @return The AccountRecord to get.
     */
    @NonNull
    IAccountRecord getAccountRecord();


    /**
     * Gets the {@link AccessTokenRecord}.
     *
     * @return The AccessTokenRecord to get.
     */
    @NonNull
    AccessTokenRecord getAccessTokenRecord();

    /**
     * @return The scopes returned from the service.
     */
    @NonNull
    String[] getScope();

    /**
     * Gets the SPE Ring property returned from the STS client telemetry header (if present).
     *
     * @return The SPE Ring or null, if not present.
     */
    @Nullable
    String getSpeRing();

    /**
     * Gets the refresh token age property returned from the STS client telemetry header (if present).
     *
     * @return The refresh token age or null, if not present.
     */
    @Nullable
    String getRefreshTokenAge();


    /**
     * Information to uniquely identify the family that the client application belongs to.
     */
    @Nullable
    String getFamilyId();

    List<ICacheRecord> getCacheRecordWithTenantProfileData();
}
