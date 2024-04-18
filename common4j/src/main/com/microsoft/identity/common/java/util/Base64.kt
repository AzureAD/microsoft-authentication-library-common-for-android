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
package com.microsoft.identity.common.java.util

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.util.base64.Base64Flags
import com.microsoft.identity.common.java.util.base64.IBase64
import com.microsoft.identity.common.java.util.base64.Java8Base64
import java.util.EnumSet
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Wrapper class around Base64 Implementations.
 **/
class Base64 {

    companion object {
        val lock = ReentrantReadWriteLock()

        // In Android < 26, this must be set to PreAndroidOBase64
        private var instance: IBase64 = Java8Base64()

        private val TAG = Base64::class.java.simpleName

        @JvmStatic
        fun setInstance(newInstance: IBase64){
            lock.writeLock().lock()
            return try {
                instance = newInstance
            } finally {
                lock.writeLock().unlock()
            }
        }

        @JvmStatic
        fun encode(byteArray: ByteArray, flag: EnumSet<Base64Flags>): ByteArray {
            lock.readLock().lock()
            return try {
                instance.encode(byteArray, flag)
            } finally {
                lock.readLock().unlock()
            }
        }

        @JvmStatic
        fun encode(stringToEncode: String, flag: EnumSet<Base64Flags>): ByteArray {
            return encode(stringToEncode.toByteArray(AuthenticationConstants.ENCODING_UTF8), flag)
        }

        @JvmStatic
        fun encodeToString(stringToEncode: String, flag: EnumSet<Base64Flags>): String {
            return String(encode(stringToEncode, flag), AuthenticationConstants.ENCODING_UTF8)
        }

        @JvmStatic
        fun encodeToString(byteArray: ByteArray, flag: EnumSet<Base64Flags>): String {
            return String(encode(byteArray, flag), AuthenticationConstants.ENCODING_UTF8)
        }

        @JvmStatic
        fun encodeUrlSafeString(stringToEncode: String) : String {
            return encodeToString(stringToEncode,
                EnumSet.of(Base64Flags.NO_WRAP, Base64Flags.NO_PADDING, Base64Flags.URL_SAFE))
        }

        @JvmStatic
        fun encodeUrlSafeString(byteArray: ByteArray) : String {
            return encodeToString(byteArray,
                EnumSet.of(Base64Flags.NO_WRAP, Base64Flags.NO_PADDING, Base64Flags.URL_SAFE))
        }

        @JvmStatic
        fun decode(byteArray: ByteArray, flag: EnumSet<Base64Flags>): ByteArray {
            lock.readLock().lock()
            return try {
                instance.decode(byteArray, flag)
            } finally {
                lock.readLock().unlock()
            }
        }

        @JvmStatic
        fun decode(byteArray: String, flag: EnumSet<Base64Flags>): ByteArray {
            return decode(byteArray.toByteArray(AuthenticationConstants.ENCODING_UTF8), flag)
        }

        @JvmStatic
        fun decode(stringToDecode: String,
                   flag: EnumSet<Base64Flags>,
                   failureMessage: String?): ByteArray {
            return decode(stringToDecode.toByteArray(AuthenticationConstants.ENCODING_UTF8),
                flag,
                failureMessage)
        }

        @JvmStatic
        fun decode(byteArray: ByteArray,
                   flag: EnumSet<Base64Flags>,
                   failureMessage: String?): ByteArray {
            val methodTag = "$TAG:decode"

            return try {
                decode(byteArray, flag)
            } catch (e: Throwable) {
                if (!failureMessage.isNullOrEmpty()) {
                    Logger.error(methodTag, "Fail to decode: " + failureMessage + " " + e.message, null)
                }
                throw e
            }
        }
    }
}