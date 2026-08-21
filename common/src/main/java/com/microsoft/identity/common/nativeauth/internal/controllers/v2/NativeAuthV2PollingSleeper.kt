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
package com.microsoft.identity.common.nativeauth.internal.controllers.v2

import com.microsoft.identity.common.java.util.ThreadUtils

/**
 * Abstraction over blocking sleep used in the V2 poll loop, enabling tests to replace the
 * production implementation with a no-op or recording fake without touching the controller.
 */
fun interface NativeAuthV2PollingSleeper {
    /**
     * Blocks the calling thread for approximately [delayMillis] milliseconds.
     *
     * @return `true` if the sleep completed normally; `false` if the thread was interrupted,
     * signalling the poll loop to stop and surface an interrupted result.
     */
    fun sleep(delayMillis: Long): Boolean
}

/**
 * Production implementation: delegates to [ThreadUtils.sleepSafely] and checks the thread
 * interrupted flag afterward.
 */
class ProductionNativeAuthV2PollingSleeper : NativeAuthV2PollingSleeper {
    override fun sleep(delayMillis: Long): Boolean {
        ThreadUtils.sleepSafely(delayMillis.toSafeSleepMillis(), TAG, "V2 poll sleep")
        return !Thread.currentThread().isInterrupted
    }

    private companion object {
        private val TAG: String = ProductionNativeAuthV2PollingSleeper::class.java.simpleName
    }
}

internal fun Long.toSafeSleepMillis(): Int =
    coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
