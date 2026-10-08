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
package com.microsoft.identity.common.internal.telemetry

import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.result.AcquireTokenResult
import com.microsoft.identity.common.logging.Logger
import java.io.Closeable

/**
 * Same-process request ownership for an existing concrete [recorder], in either onboarding mode.
 *
 * The terminal owner keeps this object across authorization views, retries and remediation, calls
 * [register] with each actual authorization request correlation ID immediately before launch, and
 * calls [complete] only after the whole request (including token exchange) succeeds or terminates.
 * An intermediate authorization interruption is not terminal while the owner intends to resume.
 * Stop page-event delivery before terminal completion; closing this owner does not detach a bridge.
 * Always [close] in finally, including launch failures. Close abandons handoff without finalizing.
 *
 * This API does not wire external broker/OneAuth consumers, transfer across processes, or retain
 * an Activity. Owners must supply the same recorder to their own instrumentation and must not
 * create multiple ownership objects for one recorder. Telemetry failures never fail authentication.
 */
class OnboardingTelemetryRequest(val recorder: OnboardingTelemetryRecorder) : Closeable {
    private val correlationIds = mutableSetOf<String>()
    private var closed = false
    private var completed = false
    private var blob: String? = null

    /** Returns false when closed, completed, unavailable, conflicting, or at registry capacity. */
    @Synchronized
    fun register(correlationId: String?): Boolean {
        if (closed || completed || correlationId == null) return false
        return try {
            if (!OnboardingRecorderRegistry.tryRegister(correlationId, recorder)) return false
            correlationIds.add(correlationId)
            true
        } catch (exception: RuntimeException) {
            false
        }
    }

    /** Attaches the cached terminal snapshot and releases all this owner's handoffs. */
    @Synchronized
    fun complete(result: AcquireTokenResult) {
        try {
            finalizeOnce()?.let { result.onboardingBlob = it }
        } finally {
            close()
        }
    }

    /** Attaches the same terminal snapshot to a domain failure and releases handoffs. */
    @Synchronized
    fun complete(exception: BaseException) {
        try {
            finalizeOnce()?.let { exception.onboardingBlob = it }
        } finally {
            close()
        }
    }

    /** Finalizes at most once, including when serialization fails or yields an empty blob. */
    @Synchronized
    fun finalizeOnce(): String? {
        if (!completed && !closed) {
            completed = true
            blob = try {
                recorder.finalizeBlob().takeIf { it.isNotEmpty() }
            } catch (exception: RuntimeException) {
                Logger.warn(TAG, "Unable to finalize onboarding telemetry.")
                null
            }
        }
        return blob
    }

    /** Identity-safe cleanup, with no terminal inference from a view or Activity lifecycle. */
    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        correlationIds.forEach { OnboardingRecorderRegistry.unregister(it, recorder) }
        correlationIds.clear()
    }

    private companion object {
        val TAG = OnboardingTelemetryRequest::class.java.simpleName
    }
}
