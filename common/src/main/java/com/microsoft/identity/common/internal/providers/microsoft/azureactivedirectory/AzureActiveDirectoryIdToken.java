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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;

/**
 * Represents additional id token claims issued by AAD (V1 Endpoint).
 * https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-token-and-claims
 */
public class AzureActiveDirectoryIdToken extends MicrosoftIdToken {

    /**
     * Stores the user name of the user principal.
     */
    public static final String UPN = "upn";

    /**
     * Provides a human readable value that identifies the subject of the token. This value is not
     * guaranteed to be unique within a tenant and is designed to be used only for display purposes.
     */
    public static final String UNIQUE_NAME = "unique_name";

    /**
     * Password expiration.
     */
    public static final String PASSWORD_EXPIRATION = "pwd_exp";

    /**
     * Password change URL.
     */
    public static final String PASSWORD_CHANGE_URL = "pwd_url";

    /**
     * Records the identity provider that authenticated the subject of the token.
     * This value is identical to the value of the Issuer claim unless the user account
     * not in the same tenant as the issuer - guests, for instance.
     * If the claim is not present, it means that the value of iss can be used instead.
     */
    public static final String IDENTITY_PROVIDER = "idp";

    /**
     * Constructor of AzureActiveDirectoryIdToken.
     *
     * @param rawIdToken raw ID token
     * @throws ServiceException if rawIdToken is malformed in JSON format.
     */
    public AzureActiveDirectoryIdToken(String rawIdToken) throws ServiceException {
        super(rawIdToken);
    }
}
