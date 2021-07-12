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

import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.IAccountRecord;

import java.util.List;

/**
 * Interface to wrap successful authentication result. When auth succeeds, token will be wrapped into the
 * {@link ILocalAuthenticationResult}
 *
 * NOTE: Due to dependencies with AccountRecord(s), we're not moving the whole class to common4j right now.
 */
public interface ILocalAuthenticationResult extends com.microsoft.identity.common.java.result.ILocalAuthenticationResultBase {

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
}
