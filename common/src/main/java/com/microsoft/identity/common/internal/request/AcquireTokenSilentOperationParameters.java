//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.request;

import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;

import androidx.annotation.Nullable;

public class AcquireTokenSilentOperationParameters extends OperationParameters {

    private final static String TAG = AcquireTokenSilentOperationParameters.class.getSimpleName();

    private RefreshTokenRecord mRefreshToken;


    public RefreshTokenRecord getRefreshToken() {
        return mRefreshToken;
    }

    public void setRefreshToken(@Nullable final RefreshTokenRecord refreshToken) {
        mRefreshToken = refreshToken;
    }

    @Override
    public void validate() throws ArgumentException {
        super.validate();

        if (mAccount == null) {
            Logger.warn(TAG, "The account set on silent operation parameters is NULL.");
        }

    }

}
