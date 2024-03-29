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
package com.microsoft.identity.labapi.utilities.authentication.adal4j;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.identity.labapi.utilities.authentication.IAuthenticationResult;
import com.microsoft.identity.labapi.utilities.authentication.IRefreshTokenSupplier;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import java.util.Date;

import lombok.AllArgsConstructor;

/**
 * An {@link IAuthenticationResult} that wraps around adal4j's authentication result.
 */
@AllArgsConstructor
public class Adal4jAuthenticationResult implements IAuthenticationResult, IRefreshTokenSupplier {

    private final AuthenticationResult mAuthenticationResult;

    @Override
    public String getAccessToken() throws LabApiException {
        return mAuthenticationResult.getAccessToken();
    }

    @Override
    public String getIdToken() {
        return mAuthenticationResult.getIdToken();
    }

    @Override
    public String getScopes() {
        throw new UnsupportedOperationException("We don't have this in ADAL4J");
    }

    @Override
    public Date getExpiresOnDate() {
        return mAuthenticationResult.getExpiresOnDate();
    }

    @Override
    public String getRefreshToken() throws LabApiException {
        return mAuthenticationResult.getRefreshToken();
    }
}
