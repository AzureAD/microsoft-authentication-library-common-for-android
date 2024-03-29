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
package com.microsoft.identity.common.java.cache;

import com.microsoft.identity.common.java.dto.AccountCredentialBase;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.dto.Credential;

public interface ICacheKeyValueDelegate {
    /**
     * Generate cache key for a specific account.
     *
     * @param account Account
     * @return String
     */
    String generateCacheKey(final AccountRecord account);

    /**
     * Generate cache value for a specific account.
     *
     * @param account Account
     * @return String
     */
    String generateCacheValue(final AccountRecord account);

    /**
     * Generate cache key from the credential.
     *
     * @param credential Credential
     * @return String
     */
    String generateCacheKey(final Credential credential);

    /**
     * Generate cache value from the credential.
     *
     * @param credential Credential
     * @return String
     */
    String generateCacheValue(final Credential credential);

    /**
     * Get the account credential from cache value.
     *
     * @param string String
     * @param t      AccountCredentialBase
     * @param <T>    Generic type
     * @return AccountCredentialBase
     */
    <T extends AccountCredentialBase> T fromCacheValue(final String string, Class<? extends AccountCredentialBase> t); // TODO consider throwing an Exception if parsing fails

}
