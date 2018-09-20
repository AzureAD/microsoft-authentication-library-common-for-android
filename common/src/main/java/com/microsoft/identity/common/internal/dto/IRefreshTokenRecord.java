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
package com.microsoft.identity.common.internal.dto;

/**
 * Interface for schema-necessary fields for RefreshTokens.
 */
public interface IRefreshTokenRecord {

    /**
     * Gets the home_account_id.
     *
     * @return The home_account_id to get.
     */
    String getHomeAccountId();

    /**
     * Gets the environment.
     *
     * @return The environment to get.
     */
    String getEnvironment();

    /**
     * Gets the clientId.
     *
     * @return The clientId to get.
     */
    String getClientId();

    /**
     * Gets the secret.
     *
     * @return The secret to get.
     */
    String getSecret();

    /**
     * Gets the target.
     *
     * @return The target to get.
     */
    String getTarget();

    /**
     * Gets the family_id.
     *
     * @return The family_id to get.
     */
    String getFamilyId();

}
