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
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets

/**
 * A class consolidating all Base64 operations in our code base.
 **/
class Base64Util {
    companion object {
        private val TAG = Base64Util::class.java.simpleName

        private var base64: IBase64 = initialize()

        /**
         * Class paths of Base64 implementation.
         * (Common4j doesn't have its own Base64 implementation.
         * The consuming library must supply it - in this case, Android and Linux common,msal/oneauth,broker)
         */
        private const val ANDROID_BASE64_CLASS_PATH = "com.microsoft.identity.common.base64.AndroidBase64"
        private const val LINUX_BASE64_CLASS_PATH = "com.microsoft.identity.broker.base64.MseberaBase64ForLinux"
        private const val COMMON4J_UNIT_TEST_BASE64_CLASS_PATH = "com.microsoft.identity.common.java.MseberaBase64ForCommon4jTests"
        private const val BROKER4J_UNIT_TEST_BASE64_CLASS_PATH = "com.microsoft.identity.broker4j.MseberaBase64ForBroker4jTests"
        private const val TESTUTILS_BASE64_CLASS_PATH = "com.microsoft.identity.internal.testutils.MseberaBase64ForTestUtils"

        /**
         * In Android, we'll use Android's own Base64 implementation.
         * In other places, we'll use Msebera's Base64.
         *
         * Both are forked from a legacy version of apache commons-codec.
         * (we had to use Msebera instead because the ClassLoader would throw an error due to a conflict between
         * the classes in commons-codec library and the one in the OS.)
         *
         * Msebera library is no longer maintained, and it was recently flagged with an Android apk tool.
         * Even if the flagged part is NOT the code path we use.
         * It's better that we migrate away from it (so that our customer don't see the false alarm).
         *
         * .. so we put them in LinuxBroker and test projects instead.
         **/
        fun initialize() : IBase64 {
            val androidBase64 = tryLoadAndroidBase64()
            if (androidBase64 != null){
                return androidBase64
            }

            val linuxBase64 = tryLoadMseberaBase64InLinux()
            if (linuxBase64 != null){
                return linuxBase64
            }

            val common4jUnitTestBase64 = tryLoadMseberaBase64InCommon4jUnitTest()
            if (common4jUnitTestBase64 != null){
                return common4jUnitTestBase64
            }

            val broker4jUnitTestBase64 = tryLoadMseberaBase64InBroker4jUnitTest()
            if (broker4jUnitTestBase64 != null){
                return broker4jUnitTestBase64
            }

            val testUtilsBase64 = tryLoadMseberaBase64InTestUtils()
            if (testUtilsBase64 != null){
                return testUtilsBase64
            }

            throw IllegalStateException("Cannot find a Base64 implementation to initialize.")
        }

        private fun tryLoadAndroidBase64(): IBase64? {
            return try {
                val androidBase64 = Class.forName(ANDROID_BASE64_CLASS_PATH).getDeclaredConstructor().newInstance() as IBase64

                // If executed in Android Unit tests, androidBase64 will fail (mocking required) with a RuntimeException.
                androidBase64.encode(ByteArray(0), Base64Flags.DEFAULT)

                return androidBase64
            } catch (e: ClassNotFoundException) {
                null
            } catch (e: RuntimeException){
                null
            }
        }

        private fun tryLoadMseberaBase64InLinux(): IBase64? {
            return try {
                Class.forName(LINUX_BASE64_CLASS_PATH)
                        .getDeclaredConstructor().newInstance() as IBase64
            } catch (e: ClassNotFoundException) {
                null
            }
        }

        private fun tryLoadMseberaBase64InCommon4jUnitTest(): IBase64? {
            return try {
                Class.forName(COMMON4J_UNIT_TEST_BASE64_CLASS_PATH)
                    .getDeclaredConstructor().newInstance() as IBase64
            } catch (e: ClassNotFoundException) {
                null
            }
        }

        private fun tryLoadMseberaBase64InBroker4jUnitTest(): IBase64? {
            return try {
                Class.forName(BROKER4J_UNIT_TEST_BASE64_CLASS_PATH)
                    .getDeclaredConstructor().newInstance() as IBase64
            } catch (e: ClassNotFoundException) {
                null
            }
        }

        private fun tryLoadMseberaBase64InTestUtils(): IBase64? {
            return try {
                Class.forName(TESTUTILS_BASE64_CLASS_PATH)
                    .getDeclaredConstructor().newInstance() as IBase64
            } catch (e: ClassNotFoundException) {
                null
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
