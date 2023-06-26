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
package com.microsoft.identity.common.internal.cache

import com.microsoft.identity.common.logging.Logger

/**
 * Holds a cache result while reading.
 * Holds a cache value while writing and provides method to validate, serialize and deserialize an entry.
 * Both [negotiatedProtocolVersion] and [error] canont be provided simultaneously.
 * @param negotiatedProtocolVersion Stores either negotiated protocol value.
 * @param error Stores error.
 * @param timeStamp Time stamp of entry.
 */
class HelloCacheResult (
    val negotiatedProtocolVersion: String?,
    val error: String?,
    internal val timeStamp: Long
) {
    init {
        if (!(negotiatedProtocolVersion.isNullOrEmpty() xor error.isNullOrEmpty())) {
            throw IllegalStateException("Either both parameters provided or none provided.")
        }
    }

    companion object {
        private val TAG = HelloCacheResult::class.java.simpleName
        private const val SEPARATOR = ","
        private const val ERROR_PREFIX = "E"
        private const val SUCCESS_PREFIX = "S"
        private const val SUCCESS_CACHE_VALUE_FORMAT = "$SUCCESS_PREFIX$SEPARATOR%s$SEPARATOR%d" // e.g. S,13.0,16000700
        private const val ERROR_CACHE_VALUE_FORMAT = "$ERROR_PREFIX$SEPARATOR%s$SEPARATOR%d" // E,handshake_failure,16000700
        private const val HANDSHAKE_ERROR = "handshake_error"

        /**
         * Reads raw entry from cache validates format and reads in to HelloCacheResult object.
         * @param value cache entry read from file.
         * @return null if value is not valid.
         */
        internal fun deserialize(value: String): HelloCacheResult? {
            val methodTag = "$TAG:deserialize"
            val values = value.split(SEPARATOR)
            // valid value is <String>,<TimeStamp>
            if (values.size != 3) {
                Logger.warn(methodTag, "Legacy or Invalid cache entry. $value")
                return null
            }

            return try {
                val timeStamp = values[2].toLong()
                if (values[0] == ERROR_PREFIX) {
                    createError(values[1], timeStamp)
                } else {
                    createFromNegotiatedProtocolVersion(values[1], timeStamp)
                }
            } catch (e: NumberFormatException) {
                Logger.error(methodTag, "Invalid cache entry. $value", e)
                null
            }
        }

        /**
         * Constructs new successful HelloCacheResult using timeStamp as current time.
         * @param negotiatedProtocolVersion A valid negotiated protocol version value e.g. 13.0, 14.0
         */
        internal fun createFromNegotiatedProtocolVersion(negotiatedProtocolVersion: String): HelloCacheResult {
            return createFromNegotiatedProtocolVersion(negotiatedProtocolVersion, System.currentTimeMillis())
        }

        /**
         * Constructs new HelloCacheResult with handshake error using timeStamp as current time.
         */
        internal fun createHandshakeError(): HelloCacheResult {
            return createError(HANDSHAKE_ERROR, System.currentTimeMillis())
        }

        private fun createFromNegotiatedProtocolVersion(negotiatedProtocolVersion: String, timeStamp: Long): HelloCacheResult {
            return HelloCacheResult(negotiatedProtocolVersion, null, timeStamp)
        }

        private fun createError(error: String, timeStamp: Long): HelloCacheResult {
            return HelloCacheResult(null, error, timeStamp)
        }
    }

    /**
     * Returns true if entry contains handshake_failure.
     */
    fun isHandShakeError() : Boolean {
        return isError() && error == HANDSHAKE_ERROR
    }

    /**
     * Returns true if the result contains negotiated protocol version value.
     */
    fun isSuccess() : Boolean {
        return !negotiatedProtocolVersion.isNullOrEmpty()
    }

    /**
     * Returns true if entry contains an error.
     */
    fun isError() : Boolean {
        return !error.isNullOrEmpty()
    }

    /**
     * Value will be serialized as below
     * For successful negotiated protocol value (NBP): S,<NBP>,<timestamp> e.g S,13.0,16000700
     * For caching any error: S,<error>,<timestamp> e.g E,handshake_failure,16000700
     * This code should work for all error and success in general but currently the only
     * error supported is handshake_failure.
     */
    internal fun serialize(): String {
        return if (!error.isNullOrEmpty()) {
            String.format(ERROR_CACHE_VALUE_FORMAT, error, timeStamp)
        } else {
            String.format(SUCCESS_CACHE_VALUE_FORMAT, negotiatedProtocolVersion, timeStamp)
        }
    }
}
