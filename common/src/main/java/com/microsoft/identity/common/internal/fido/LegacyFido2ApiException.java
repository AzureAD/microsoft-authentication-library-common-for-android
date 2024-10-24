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
package com.microsoft.identity.common.internal.fido;

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ErrorStrings;

/**
 * An exception class which holds attributes from the legacy FIDO2 API error responses.
 */
public class LegacyFido2ApiException extends BaseException {

    public static final String NULL_OBJECT = ErrorStrings.NULL_OBJECT;

    public static final String BAD_ACTIVITY_RESULT_CODE = ErrorStrings.BAD_ACTIVITY_RESULT_CODE;

    public static final String UNKNOWN_ERROR = ErrorStrings.UNKNOWN_ERROR;

    public static final String GET_PENDING_INTENT_ERROR = ErrorStrings.GET_PENDING_INTENT_ERROR;

    public static final String GET_PENDING_INTENT_CANCELED = ErrorStrings.GET_PENDING_INTENT_CANCELED;

    public LegacyFido2ApiException(final String errorCode) {
        super(errorCode);
    }

    public LegacyFido2ApiException(final String errorCode, final String errorMessage) {
        super(errorCode, errorMessage);
    }

    public LegacyFido2ApiException(final String errorCode, final String errorMessage, final Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }
}
