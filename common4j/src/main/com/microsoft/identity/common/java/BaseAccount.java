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
package com.microsoft.identity.common.java;

import com.microsoft.identity.common.java.dto.IAccountRecord;

import java.util.List;

/**
 * In MSAL we have user... in ADAL we have userinfo
 * Users are human or software agents.... humans and software agents have accounts
 * UserInfo shouldn't be used in common since it collides with the OIDC spec
 * This class contains information about the user/account associated with the authenticated subject/principal
 */
public abstract class BaseAccount implements IAccountRecord {

    /**
     * Not all IDPs will have the same unique identifier for a user
     * Per the OIDC spec the unique identifier is subject... or the sub claim; however AAD and other
     * IDPs have their own unique identifiers for users
     * <p>
     * Let the IDP give us the representation of the user/account based on the token response
     *
     * @return String of unique identifier
     */
    public abstract String getUniqueIdentifier();

    /**
     * @return cache identifiers.
     */
    public abstract List<String> getCacheIdentifiers();

}
