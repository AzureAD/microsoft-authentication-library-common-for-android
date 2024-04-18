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
package com.microsoft.identity.common.internal.util

import android.util.Base64
import com.microsoft.identity.common.java.util.base64.Base64Flags
import com.microsoft.identity.common.java.util.base64.Java8Base64
import java.util.EnumSet

/**
 * In pre android-O, this must be used.
 * Java8Base64 only supports Android O and above.
 * */
class PreAndroidOBase64 : Java8Base64() {

    override fun encode(bytesToEncode: ByteArray, flag: EnumSet<Base64Flags>): ByteArray {
        return Base64.encode(bytesToEncode, getAndroidBase64Flag(flag))
    }

    override fun decode(input: ByteArray, flag: EnumSet<Base64Flags>): ByteArray {
        return Base64.decode(input, getAndroidBase64Flag(flag))
    }

    private fun getAndroidBase64Flag(flag: EnumSet<Base64Flags>): Int {
        var androidBase64Flag = Base64.DEFAULT
        if (flag.contains(Base64Flags.URL_SAFE)) {
            androidBase64Flag = androidBase64Flag or Base64.URL_SAFE
        }
        if (flag.contains(Base64Flags.NO_PADDING)) {
            androidBase64Flag = androidBase64Flag or Base64.NO_PADDING
        }
        if (flag.contains(Base64Flags.NO_WRAP)) {
            androidBase64Flag = androidBase64Flag or Base64.NO_WRAP
        }

        return androidBase64Flag
    }
}