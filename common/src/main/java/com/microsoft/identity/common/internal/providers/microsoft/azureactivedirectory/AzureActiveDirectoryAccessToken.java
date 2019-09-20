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

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.providers.oauth2.AccessToken;

import java.util.Date;

public class AzureActiveDirectoryAccessToken extends AccessToken {

    private Date mExpiresOn;
    private Date mExtendedExpiresOn;

    /**
     * Constructor of AzureActiveDirectoryAccessToken.
     *
     * @param response AzureActiveDirectoryTokenResponse
     */
    public AzureActiveDirectoryAccessToken(
            @NonNull final AzureActiveDirectoryTokenResponse response) {
        super(response);
        mExpiresOn = response.getExpiresOn();
        mExtendedExpiresOn = response.getExtExpiresOn();
    }

    /**
     * @return mExpiresOn of AzureActiveDirectoryAccessToken
     */
    public Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * @return mExtendedExpiresOn of AzureActiveDirectoryAccessToken
     */
    public Date getExtendedExpiresOn() {
        return mExtendedExpiresOn;
    }

    //TODO: Need to add override for IsExpired() to address extended token expires on

}
