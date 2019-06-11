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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectoryb2c;

import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Map;

/**
 * Azure Active Directory B2C Id Token.
 * B2C supports customizing the claims contained in tokens
 * see <a href='https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-tokens'>https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-reference-tokens</a>
 */
public class AzureActiveDirectoryB2CIdToken extends IDToken {
    /**
     * Constructor of AzureActiveDirectoryB2CIdToken.
     *
     * @param rawIdToken String
     * @throws ServiceException if rawIdToken is malformed in JSON format.
     */
    public AzureActiveDirectoryB2CIdToken(String rawIdToken) throws ServiceException {
        super(rawIdToken);
    }

    @Override
    public Map<String, ?> getTokenClaims() {
        return super.getTokenClaims();
    }
}
