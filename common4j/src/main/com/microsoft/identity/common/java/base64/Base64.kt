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
package com.microsoft.identity.common.java.base64

import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.util.StringUtil
import java.nio.charset.StandardCharsets

class Base64Util {
    companion object {
        private val TAG = Base64Util::class.java.simpleName

        private var base64: IBase64 = initialize()

        /**
         * If this is executed on Android, then we'll use Android's Base64.
         * Otherwise, we'll keep using Msebera.
         *
         * NOTE2: msebera is not in common's dependency chain (common ingest common4j with transitive = false),
         *       so we'll need to add that in the test module
         *       (currently done through testfixtures, which consumes common4j with transitive = true)
         * */
        fun initialize() : IBase64 {
            return try {
                // If the class is not included in the final product, ClassNotFoundException will be thrown.
                // (e.g. when executing common4j unit tests, or in Linux Broker)
                val androidBase64 = Class.forName("com.microsoft.identity.common.base64.AndroidBase64").getDeclaredConstructor().newInstance() as IBase64

                // If executed in Android Unit tests, androidBase64 will fail (mocking required) with a RuntimeException.
                androidBase64.encode(ByteArray(0), Base64Flags.DEFAULT)

                return androidBase64
            } catch (e: ClassNotFoundException){
                MseberaBase64()
            } catch (e: RuntimeException){
                MseberaBase64()
            }
        }

//region encode
        @JvmStatic
        fun encode(input: ByteArray, vararg flags: Base64Flags): ByteArray {
            return base64.encode(input, *flags)
        }

        @JvmStatic
        fun encodeToString(input: ByteArray, vararg flags: Base64Flags): String {
            return String(encode(input, *flags), StandardCharsets.US_ASCII)
        }

        @JvmStatic
        fun encodeToStringNoWrap(input: ByteArray): String {
            return encodeToString(input, Base64Flags.NO_WRAP)
        }

        @JvmStatic
        fun encodeUrlSafeString(input: ByteArray): String {
            return encodeToString(
                input,
                Base64Flags.NO_WRAP, Base64Flags.NO_PADDING, Base64Flags.URL_SAFE
            )
        }

        @JvmStatic
        fun encodeUrlSafeString(input: String): String {
            return encodeUrlSafeString(StringUtil.toByteArray(input))
        }

//endregion

//region decode
        @JvmStatic
        fun decode(input: ByteArray, vararg flags: Base64Flags): ByteArray {
            return base64.decode(input, *flags)
        }

        @JvmStatic
        fun decode(input: String, vararg flags: Base64Flags): ByteArray {
            return base64.decode(input.toByteArray(), *flags)
        }

        /**
         * Converts the given String into a rawData byte array, and Base64-decode it.
         */
        @JvmStatic
        fun decodeNoWrap(input: String): ByteArray {
            return decode(input, Base64Flags.NO_WRAP)
        }

        /***
         * Helper to perform base64 decoding with logging.
         * @param failureMessage The message to log in case of failure.
         * @param input Input string
         * @param flags
         */
        @JvmStatic
        fun decode(failureMessage: String, input: String, vararg flags: Base64Flags): ByteArray {
            val methodTag = "$TAG:decode"
            try {
                return decode(input, *flags)
            } catch (e: IllegalArgumentException) {
                Logger.error(methodTag, failureMessage + " " + e.message, null)
                throw e
            }
        }

//endregion
    }
}