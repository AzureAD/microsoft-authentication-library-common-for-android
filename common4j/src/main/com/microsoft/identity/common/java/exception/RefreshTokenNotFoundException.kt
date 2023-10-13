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
package com.microsoft.identity.common.java.exception

/**
 * This exception indicates that no (valid) refresh token was found in the cache to perform
 * the refresh token flow with. The user should sign in again.
 */
class RefreshTokenNotFoundException(errorCode: String, errorMessage: String, throwable: Throwable? = null) :
    ServiceException(errorCode, errorMessage, throwable) {
    // This is needed for backward compatibility with older versions of MSAL (pre 3.0.0)
    // When MSAL converts the result bundle it looks for this value to know about exception type
    // We moved the exception class to a new package with refactoring work,
    // but need to keep this value to older package name to avoid breaking older MSAL clients.
    val sName = "com.microsoft.identity.common.exception.RefreshTokenNotFoundException"

    override fun getExceptionName(): String? {
        return sName
    }
}
