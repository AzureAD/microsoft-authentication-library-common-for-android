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
package com.microsoft.identity.common.internal.servertelemetry;

public final class PublicApiId {

    // Silent Apis
    public static final String BROKER_ACQUIRE_TOKEN_SILENT = "21";
    public static final String LOCAL_ACQUIRE_TOKEN_SILENT = "22";

    // Interactive APIs
    public static final String BROKER_ACQUIRE_TOKEN_INTERACTIVE = "121";
    public static final String LOCAL_ACQUIRE_TOKEN_INTERACTIVE = "122";

    // Get/Remove accounts
    public static final String GET_ACCOUNTS = "921";
    public static final String GET_ACCOUNT = "922";
    public static final String GET_CURRENT_ACCOUNT_ASYNC = "923";
    public static final String REMOVE_ACCOUNT = "924";
    public static final String SIGN_OUT = "925";
}
