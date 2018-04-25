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
package com.microsoft.identity.common.exception;

import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.adal.internal.util.HashMapExtensions;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;

public class BaseException extends Exception {
    private String mErrorCode;

    /**
     * Default constructor.
     */
    BaseException() {
    }

    /**
     * Initiates the detailed error code.
     *
     * @param errorCode The error code contained in the exception.
     */
    public BaseException(final String errorCode) {
        mErrorCode = errorCode;
    }

    /**
     * Initiates the {@link BaseException} with error code and error message.
     *
     * @param errorCode    The error code contained in the exception.
     * @param errorMessage The error message contained in the exception.
     */
    public BaseException(final String errorCode, final String errorMessage) {
        super(errorMessage);
        mErrorCode = errorCode;
    }

    /**
     * Initiates the {@link BaseException} with error code, error message and throwable.
     *
     * @param errorCode    The error code contained in the exception.
     * @param errorMessage The error message contained in the exception.
     * @param throwable    The {@link Throwable} contains the cause for the exception.
     */
    public BaseException(final String errorCode, final String errorMessage,
                  final Throwable throwable) {
        super(errorMessage, throwable);
        mErrorCode = errorCode;
    }

    /**
     * @return The error code for the exception, could be null. {@link BaseException} is the top level base exception, for the
     * constants value of all the error code.
     */
    public String getErrorCode() {
        return mErrorCode;
    }

    /**
     * {@inheritDoc}
     * Return the detailed description explaining why the exception is returned back.
     */
    @Override
    public String getMessage() {
        if (!StringExtensions.isNullOrBlank(super.getMessage())) {
            return super.getMessage();
        }

        return null;
    }
}
