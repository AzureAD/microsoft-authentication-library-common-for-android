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
package com.microsoft.identity.common.java.util.base64

import java.util.Base64
import java.util.EnumSet

/**
 * Base64 implementation based on java.util.Base64
 *
 * NOTE: This requires Android API 26 and Above.
 *       In Android, we would have to override this class and add pre android 26 implementation.
 **/
open class Java8Base64 : IBase64 {

    @Suppress("NewApi")
    override fun encode(bytesToEncode: ByteArray,
                        flag: EnumSet<Base64Flags>): ByteArray {
        return getEncoder(flag).encode(bytesToEncode)
    }

    @Suppress("NewApi")
    private fun getEncoder(flag: EnumSet<Base64Flags>): Base64.Encoder {
        var encoder =
            if (flag.contains(Base64Flags.URL_SAFE)) {
                Base64.getUrlEncoder()
            } else if (!flag.contains(Base64Flags.NO_WRAP)) {
                Base64.getMimeEncoder()
            } else {
                return Base64.getEncoder()
            }

        if (flag.contains(Base64Flags.NO_PADDING)){
            encoder = encoder.withoutPadding()
        }

        return encoder
    }

    @Suppress("NewApi")
    override fun decode(input: ByteArray,
                        flag: EnumSet<Base64Flags>): ByteArray {
        return getDecoder(flag).decode(input)
    }

    @Suppress("NewApi")
    private fun getDecoder(flag: EnumSet<Base64Flags>): Base64.Decoder {
        return if (flag.contains(Base64Flags.URL_SAFE)) {
            Base64.getUrlDecoder()
        } else if (!flag.contains(Base64Flags.NO_WRAP)) {
            Base64.getMimeDecoder()
        } else {
            Base64.getDecoder()
        }
    }

}