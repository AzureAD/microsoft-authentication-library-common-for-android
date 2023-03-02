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

import java.util.Date;
import java.util.List;

import lombok.NonNull;

/**
 * Interface to wrap successful authentication result. When auth succeeds, token will be wrapped into the
 * {@link ILocalAuthenticationResult}
 *
 * NOTE: Due to dependencies with AccountRecord(s), we're not moving the whole class to common4j right now.
 */
public interface ILocalAuthenticationResult {

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
     * Gets a list of credentials and accounts from cache. This list will include both root accounts
     * as well as guest accounts aka tenant profiles.
     *
     * @return a list of {@link ICacheRecord} objects
     */
    List<ICacheRecord> getCacheRecordWithTenantProfileData();
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
     * @return A unique tenant identifier that was used in token acquisition. Could be null if tenant information is not
     * returned by the service.
     */
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
    String getIdToken();

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
    String getSpeRing();

    /**
     * Gets the refresh token age property returned from the STS client telemetry header (if present).
     *
     * @return The refresh token age or null, if not present.
     */
    String getRefreshTokenAge();

    /**
     * Information to uniquely identify the family that the client application belongs to.
     */
    String getFamilyId();


    /**
     * Gets whether the result of token request was returned from cache or not
     *
     * @return a boolean indicating if the request was serviced from cache
     */
    boolean isServicedFromCache();

    /**
     * Gets the correlation id used during this request. Could be null in non-MSAL scenarios. This
     * should never be null in the case of MSAL.
     *
     * @return a String representing a correlation id
     */
    String getCorrelationId();
}
