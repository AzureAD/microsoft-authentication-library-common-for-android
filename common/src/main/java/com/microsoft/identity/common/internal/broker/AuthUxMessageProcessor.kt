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

package com.microsoft.identity.common.internal.broker

/**
 * Defines which Auth UX message types may be processed for one authorization request.
 *
 * Capabilities are fixed when the request is created so the `JAVASCRIPT_INTERFACE` and
 * `WEB_MESSAGE_LISTENER` transports enforce the same caller-specific policy. Number Matching is
 * restricted to broker-owned requests.
 *
 * @property telemetryAllowed Whether `log_telemetry` messages may be forwarded to the request's
 * telemetry sink.
 * @property numberMatchingAllowed Whether Number Matching messages may update the broker's
 * ephemeral Number Matching store.
 */
data class AuthUxRequestCapabilities(
    val telemetryAllowed: Boolean,
    val numberMatchingAllowed: Boolean
) {
    companion object {
        /**
         * Capabilities for broker-owned Auth UX requests, which support both telemetry and
         * Broker-only Number Matching messages.
         */
        @JvmField
        val BROKER = AuthUxRequestCapabilities(
            telemetryAllowed = true,
            numberMatchingAllowed = true
        )
    }
}

/**
 * Processes Auth UX messages for one authorization request independently of their transport.
 *
 * Both the `addJavascriptInterface` bridge and `WebMessageListener` delegate to this processor so
 * parsing, capability enforcement, and side effects remain consistent. Closing the processor
 * permanently rejects late callbacks after the request lifecycle ends.
 */
interface AuthUxMessageProcessor : AutoCloseable {
    /** Whether this processor has stopped accepting messages. */
    val isClosed: Boolean

    /** Parses and handles one JSON payload when the request is still active. */
    fun process(jsonPayload: String)

    /** Idempotently stops this processor from accepting additional messages. */
    override fun close()
}
