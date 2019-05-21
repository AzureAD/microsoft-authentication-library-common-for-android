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

import com.microsoft.identity.common.BaseAccount;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.providers.oauth2.RefreshToken;

/**
 * Interface that defines methods allowing refresh token cache state to be shared between Cache Implementations.
 * The assumption being that in order for a client to avoid prompting a user to sign in they need a refresh token (effectively SSO state)
 */
public interface IShareSingleSignOnState<T extends BaseAccount, U extends RefreshToken> {
    /**
     * Set the single sign on state for account.
     *
     * @param account      T
     * @param refreshToken U
     * @throws ClientException
     */
    void setSingleSignOnState(T account, U refreshToken) throws ClientException;

    /**
     * Get the single sign on state.
     *
     * @param account T
     * @return U
     */
    U getSingleSignOnState(T account);

}
