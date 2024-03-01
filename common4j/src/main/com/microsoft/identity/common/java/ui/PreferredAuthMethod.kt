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
package com.microsoft.identity.common.java.ui

import kotlin.jvm.Throws

/**
 *  Authentication methods for the user.
 * This code will be sent to eSTS as a hint to what authentication method the user prefers.
 * If not specified, eSTS will use the default authentication method.
 */
enum class PreferredAuthMethod(@JvmField val code: Int, @JvmField val value: String?) {
    /**
     * No preferred authentication method.
     */
    NONE(0, null),

    /**
     * QR code + PIN authentication.
     * QR + PIN Authentication, is possible in MSAL only and MSAL with Broker flows.
     * By default broker flows will have the capability to use QR code + PIN authentication.
     * If is only MSAL, the user will have to add the camera permission to the app manifest
     * to use QR code + PIN authentication, i.e. <uses-permission android:name="android.permission.CAMERA" />
     * 18 is the code for QR code + PIN authentication on ESTS.
     */
    QR(18, "qrpin");
    companion object {
        @JvmStatic
        @Throws(NoSuchElementException::class)
        fun fromCode(code: Int) = PreferredAuthMethod.values().first { it.code == code }
        @JvmStatic
        @Throws(NoSuchElementException::class)
        fun fromValue(value: String?) = PreferredAuthMethod.values().first { it.value == value }
    }
}
