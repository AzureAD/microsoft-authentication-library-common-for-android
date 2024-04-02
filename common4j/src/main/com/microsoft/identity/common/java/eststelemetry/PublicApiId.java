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
package com.microsoft.identity.common.java.eststelemetry;

public final class PublicApiId {

    //region Silent APIs

    // PublicClientApplication
    //==============================================================================================
    public static final String PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS = "21";
    public static final String PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS = "22";

    // SingleAccountPublicClientApplication
    //==============================================================================================
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS = "23";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS = "24";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_SCOPES_AUTHORITY = "25";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_SCOPES_AUTHORITY_CALLBACK = "26";

    // MultipleAccountPublicClientApplication
    //==============================================================================================
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_SCOPES_ACCOUNT_AUTHORITY = "27";
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS = "30";
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_SCOPES_ACCOUNT_AUTHORITY_CALLBACK = "28";
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS = "31";

    // BrokerClientApplication
    //==============================================================================================
    public static final String BROKER_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS_CALLBACK = "29";

    //endregion

    //region Interactive APIs

    // PublicClientApplication
    //==============================================================================================
    public static final String PCA_ACQUIRE_TOKEN_WITH_PARAMETERS = "121";
    public static final String PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_CALLBACK = "122";

    // SingleAccountPublicClientApplication;
    //==============================================================================================
    public static final String SINGLE_ACCOUNT_PCA_SIGN_IN = "123";
    public static final String SINGLE_ACCOUNT_PCA_SIGN_IN_WITH_PROMPT = "130";
    public static final String SINGLE_ACCOUNT_PCA_SIGN_IN_WITH_PARAMETERS = "132";
    public static final String SINGLE_ACCOUNT_PCA_SIGN_IN_WITH_PARAMETERS_PROMPT = "133";
    public static final String SINGLE_ACCOUNT_PCA_EXISTING_SIGN_IN_WITH_PROMPT = "131";
    public static final String SINGLE_ACCOUNT_PCA_EXISTING_SIGN_IN_WITH_PARAMETERS = "134";
    public static final String SINGLE_ACCOUNT_PCA_EXISTING_SIGN_IN_WITH_PARAMETERS_PROMPT = "135";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS = "124";
    public static final String SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_CALLBACK = "125";

    // MultipleAccountPublicClientApplication
    //==============================================================================================
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_LOGINHINT_CALLBACK = "126";
    public static final String MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS = "136";

    // BrokerClientApplication
    //==============================================================================================
    public static final String BROKER_ACQUIRE_TOKEN_WITH_PARAMETERS_CALLBACK = "127";
    public static final String BROKER_ADD_ACCOUNT_WITH_ACTIVITY = "128";
    public static final String BROKER_CHOOSE_ACCOUNT_WITH_ACTIVITY_ACCOUNTNAME = "129";
    public static final String BROKER_ADD_ACCOUNT_FOR_ATV2 = "301";

    //endregion

    // region GET Accounts

    // SingleAccountPublicClientApplication
    //==============================================================================================
    public static final String SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT = "921";
    public static final String SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT_ASYNC = "922";

    // MultipleAccountPublicClientApplication
    //==============================================================================================
    public static final String MULTIPLE_ACCOUNT_PCA_GET_ACCOUNTS = "923";
    public static final String MULTIPLE_ACCOUNT_PCA_GET_ACCOUNTS_WITH_CALLBACK = "924";
    public static final String MULTIPLE_ACCOUNT_PCA_GET_ACCOUNT_WITH_IDENTIFIER = "925";
    public static final String MULTIPLE_ACCOUNT_PCA_GET_ACCOUNT_WITH_IDENTIFIER_CALLBACK = "926";

    //endregion

    // region REMOVE account

    // SingleAccountPublicClientApplication
    //==============================================================================================
    public static final String SINGLE_ACCOUNT_PCA_SIGN_OUT = "927";
    public static final String SINGLE_ACCOUNT_PCA_SIGN_OUT_WITH_CALLBACK = "928";

    // MultipleAccountPublicClientApplication
    //==============================================================================================
    public static final String MULTIPLE_ACCOUNT_PCA_REMOVE_ACCOUNT_WITH_ACCOUNT = "929";
    public static final String MULTIPLE_ACCOUNT_PCA_REMOVE_ACCOUNT_WITH_ACCOUNT_CALLBACK = "930";

    //endregion

    // region Device Code Flow

    // PublicClientApplication
    //==============================================================================================
    public static final String DEVICE_CODE_FLOW_WITH_CALLBACK = "650";
    public static final String DEVICE_CODE_FLOW_WITH_CLAIMS_AND_CALLBACK = "651";

    //endregion

    // region generateSignedHttpRequest
    //==============================================================================================
    public static final String PCA_GENERATE_SIGNED_HTTP_REQUEST = "1100";
    public static final String PCA_GENERATE_SIGNED_HTTP_REQUEST_ASYNC = "1101";
    //endregion

    // region RefreshOn API
    //==============================================================================================
    public static final String MSAL_REFRESH_ON= "1201";
    public static final String BROKER_REFRESH_ON = "1202";
    //endregion

    public static final String PCA_GET_DEVICE_MODE = "1200";

    public static final String PCA_IS_QR_PIN_AVAILABLE = "1300";
    //region NativeAuthPublicClientApplication
    //==============================================================================================
    public static final String NATIVE_AUTH_SIGN_IN_WITH_EMAIL = "210";
    public static final String NATIVE_AUTH_SIGN_IN_WITH_EMAIL_PASSWORD = "211";
    public static final String NATIVE_AUTH_SIGN_IN_SUBMIT_CODE = "212";
    public static final String NATIVE_AUTH_SIGN_IN_RESEND_CODE = "213";
    public static final String NATIVE_AUTH_SIGN_IN_SUBMIT_PASSWORD = "214";
    public static final String NATIVE_AUTH_GET_ACCOUNT = "215";
    public static final String NATIVE_AUTH_SIGN_IN_WITH_SLT = "216";
    public static final String NATIVE_AUTH_RESET_PASSWORD_START = "220";
    public static final String NATIVE_AUTH_RESET_PASSWORD_SUBMIT_CODE = "221";
    public static final String NATIVE_AUTH_RESET_PASSWORD_RESEND_CODE = "222";
    public static final String NATIVE_AUTH_RESET_PASSWORD_SUBMIT_NEW_PASSWORD = "223";
    public static final String NATIVE_AUTH_SIGN_UP_WITH_CODE = "230";
    public static final String NATIVE_AUTH_SIGN_UP_START = "231";
    public static final String NATIVE_AUTH_SIGN_UP_START_WITH_PASSWORD = "232";
    public static final String NATIVE_AUTH_SIGN_UP_RESEND_CODE = "233";
    public static final String NATIVE_AUTH_SIGN_UP_SUBMIT_PASSWORD = "234";
    public static final String NATIVE_AUTH_SIGN_UP_SUBMIT_ATTRIBUTES = "234";
    public static final String NATIVE_AUTH_SIGN_UP_SUBMIT_CODE = "235";
    public static final String NATIVE_AUTH_ACCOUNT_SIGN_OUT = "240";
    public static final String NATIVE_AUTH_ACCOUNT_GET_ACCESS_TOKEN = "250";
    //endregion
}
