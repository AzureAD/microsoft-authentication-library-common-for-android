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

public class ArgumentException extends BaseException {

    public final static String ACQUIRE_TOKEN_OPERATION_NAME = "acquireToken";
    public final static String ACQUIRE_TOKEN_SILENT_OPERATION_NAME = "acquireTokenSilent";

    public final static String SCOPE_ARGUMENT_NAME = "scopes";
    public final static String AUTHORITY_ARGUMENT_NAME = "authority";
    public final static String IACCOUNT_ARGUMENT_NAME = "account";

    public final static String ILLEGAL_ARGUMENT_ERROR_CODE = "illegal_argument_exception";

    private String mOperationName;
    private String mArgumentName;

    public ArgumentException(final String operationName, final String argumentName, final String message) {
        super(ILLEGAL_ARGUMENT_ERROR_CODE, message);
        mOperationName = operationName;
        mArgumentName = argumentName;

    }

    public ArgumentException(final String operationName,
                             final String argumentName,
                             final String message,
                             final Throwable throwable) {
        super(ILLEGAL_ARGUMENT_ERROR_CODE, message, throwable);
        mOperationName = operationName;
        mArgumentName = argumentName;
    }

    public String getOperationName() {
        return mOperationName;
    }

    public String getArgumentName() {
        return mArgumentName;
    }

}
