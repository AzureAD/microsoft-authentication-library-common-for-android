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
 * Compact JSON keys on [ExecutionEvent] fields (t, ts, tid, s, e) minimize payload size.
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
    CommonTokenRequestExecute,
    CommonTokenResponseReceived,

    /**
     * Sentinel for a tag emitted by a broker newer than this client. Never emitted by this SDK —
     * it is produced only when deserializing a forward-version payload. Broker and client ship
     * independently, so a client can receive a tag added after it was built; mapping that to a
     * sentinel degrades the single event rather than discarding the whole payload.
     *
     * The "never emitted" rule is a convention, not an enforced invariant: nothing stops a caller
     * passing this to [EventCollector.addEvent], and this enum is shared with the broker repo,
     * which *is* an emitter — so the convention crosses a repo boundary. It is left unenforced
     * deliberately. A `require` would trade a merely uninformative tag for a thrown exception on
     * the telemetry path, and that path is built never to fail an authentication: see the broad
     * `catch (Exception)` around telemetry deserialization in `MsalBrokerResultAdapter`. If this
     * ever needs teeth, the safe form is for [EventCollector.addEvent] to drop the event and log,
     * not to throw.
     */
    Unknown
}
