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
package com.microsoft.identity.common.java.broker.telemetry

/**
 * Enum of event tags representing phases of the broker authentication flow.
 * Serialized as human-readable enum names (e.g., "BrokerCacheHit") on the IPC wire format.
 * Compact JSON keys on [ExecutionEvent] fields (t, ts, tid, d, e) minimize payload size.
 */
enum class EventTag {
    // BrokerEntry (5)
    BrokerRequestReceived,
    BrokerRequestDeserialized,
    BrokerAccountLookupStart,
    BrokerAccountLookupEnd,
    BrokerRequestValidated,
    // BrokerDispatch (3)
    BrokerControllerSelected,
    BrokerCommandQueued,
    BrokerCommandExecutionStart,
    // BrokerCache (6)
    BrokerCacheCheckStart,
    BrokerCacheCheckEnd,
    BrokerCacheHit,
    BrokerCacheMiss,
    BrokerCacheWriteStart,
    BrokerCacheWriteEnd,
    // BrokerNetwork (5)
    BrokerPrtLoadStart,
    BrokerNetworkCallStart,
    BrokerNetworkCallEnd,
    BrokerTokenAcquired,
    BrokerNetworkCallFailed,
    // BrokerResponse (3)
    BrokerResponseSerialized,
    BrokerResponseSent,
    BrokerRequestFailed,
    // CommonStrategy (2)
    CommonHttpRequestExecute,
    CommonHttpResponseReceived
}
