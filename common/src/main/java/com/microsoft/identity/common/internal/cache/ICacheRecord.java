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
package com.microsoft.identity.common.internal.cache;

import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;

/**
 * Result container for Account and Credential - usually the result of a save or load operation.
 */
public interface ICacheRecord {

    /**
     * Gets the {@link AccountRecord}.
     *
     * @return The Account to get.
     */
    AccountRecord getAccount();

    /**
     * Gets the {@link AccessTokenRecord}.
     *
     * @return The AccessToken to get.
     */
    AccessTokenRecord getAccessToken();

    /**
     * Gets the {@link RefreshTokenRecord}.
     *
     * @return The RefreshToken to get.
     */
    RefreshTokenRecord getRefreshToken();

    /**
     * Gets the {@link IdTokenRecord}.
     *
     * @return The IdToken to get.
     */
    IdTokenRecord getIdToken();

    /**
     * Gets the {@link IdTokenRecord} in v1 format.
     * @return
     */
    IdTokenRecord getV1IdToken();
}
