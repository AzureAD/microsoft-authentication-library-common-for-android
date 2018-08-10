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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;

/**
 * IdToken claims emitted by the Microsoft STS (V2).
 *
 * @see <a href="https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-tokens">Azure Active Directory v2.0 tokens reference</a>
 */
public class MicrosoftStsIdToken extends MicrosoftIdToken {
    /**
     * The time at which the token becomes invalid, represented in epoch time. Your app should use
     * this claim to verify the validity of the token lifetime.
     */
    public static final String EXPIRATION_TIME = "exp";

    // TODO Could not locate documentation for the following fields.
    public static final String AIO = "aio";
    public static final String UTI = "uti";

    /**
     * Constructor of MicrosoftStsIdToken.
     *
     * @param rawIdToken String
     * @throws ServiceException if rawIdToken is malformed in JSON format.
     */
    public MicrosoftStsIdToken(String rawIdToken) throws ServiceException {
        super(rawIdToken);
    }
}
