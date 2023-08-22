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
package com.microsoft.identity.common.java.exception;

public class ArgumentException extends BaseException {

    // This is needed for backward compatibility with older versions of MSAL (pre 3.0.0)
    // When MSAL converts the result bundle it looks for this value to know about exception type
    // We moved the exception class to a new package with refactoring work,
    // but need to keep this value to older package name to avoid breaking older MSAL clients.
    public static final String sName =  "com.microsoft.identity.common.exception.ArgumentException";

    private static final long serialVersionUID = -6399451133831073876L;

    public final static String ACQUIRE_TOKEN_OPERATION_NAME = "acquireToken";
    public final static String ACQUIRE_TOKEN_SILENT_OPERATION_NAME = "acquireTokenSilent";
    public final static String ACQUIRE_TOKEN_WITH_DEVICE_CODE_OPERATION_NAME = "acquireTokenWithDeviceCode";
    public final static String ACQUIRE_TOKEN_WITH_PASSWORD_OPERATION_NAME = "acquireTokenWithPassword";
    public final static String BROKER_TOKEN_REQUEST_OPERATION_NAME = "brokerTokenRequest";
    public final static String GET_ACCOUNTS_OPERATION_NAME = "getAllAccounts";
    public final static String REMOVE_ACCOUNT_OPERATION_NAME = "removeAccount";
    public final static String GENERATE_SHR_OPERATION_NAME = "generateShr";
    public final static String LINUX_BROKER_VERSION_NAME = "linuxBrokerVersion";
    public final static String ACQUIRE_PRT_SSO_COOKIE_OPERATION_NAME = "acquirePrtSsoCookie";

    public final static String SCOPE_ARGUMENT_NAME = "scopes";
    public final static String AUTHORITY_ARGUMENT_NAME = "authority";
    public final static String IACCOUNT_ARGUMENT_NAME = "account";
    public final static String AUTHENTICATION_SCHEME_ARGUMENT_NAME = "authentication_scheme";

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

    @Override
    public String getExceptionName(){
        return sName;
    }

}
