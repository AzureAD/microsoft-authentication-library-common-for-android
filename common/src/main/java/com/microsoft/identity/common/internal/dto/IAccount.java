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

public interface IAccount {

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
     * Gets the realm.
     *
     * @return The realm to get.
     */
    String getRealm();

    /**
     * Gets the authority_account_id.
     *
     * @return The authority_account_id to get.
     */
    String getAuthorityAccountId();

    /**
     * Gets the username.
     *
     * @return The username to get.
     */
    String getUsername();

    /**
     * Gets the authority_type.
     *
     * @return The authority_type to get.
     */
    String getAuthorityType();

    /**
     * Gets the alternative_account_id.
     *
     * @return The alternative_account_id to get.
     */
    String getAlternativeAccountId();

    /**
     * Gets the first_name;
     *
     * @return The first_name to get.
     */
    String getFirstName();

    /**
     * Gets the last_name.
     *
     * @return The last_name to get.
     */
    String getLastName();

    /**
     * Gets the avatar_url.
     *
     * @return The avatar_url to get.
     */
    String getAvatarUrl();
}
