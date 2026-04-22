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

import com.google.gson.annotations.SerializedName

/**
 * Enum of event tags representing phases of the broker authentication flow.
 * Each tag has a compact serialized value to minimize payload size.
 */
enum class EventTag(@SerializedName("v") val value: String) {
    // BrokerEntry (5)
    BrokerRequestReceived("bre.recv"),
    BrokerRequestDeserialized("bre.dser"),
    BrokerAccountLookupStart("bre.als"),
    BrokerAccountLookupEnd("bre.ale"),
    BrokerRequestValidated("bre.val"),
    // BrokerDispatch (3)
    BrokerControllerSelected("bdi.csel"),
    BrokerCommandQueued("bdi.cq"),
    BrokerCommandExecutionStart("bdi.cex"),
    // BrokerCache (6)
    BrokerCacheCheckStart("bca.cks"),
    BrokerCacheCheckEnd("bca.cke"),
    BrokerCacheHit("bca.hit"),
    BrokerCacheMiss("bca.miss"),
    BrokerCacheWriteStart("bca.wrs"),
    BrokerCacheWriteEnd("bca.wre"),
    // BrokerNetwork (5)
    BrokerPrtLoadStart("bne.prt"),
    BrokerNetworkCallStart("bne.ncs"),
    BrokerNetworkCallEnd("bne.nce"),
    BrokerTokenAcquired("bne.tok"),
    BrokerNetworkCallFailed("bne.nfl"),
    // BrokerResponse (3)
    BrokerResponseSerialized("brs.ser"),
    BrokerResponseSent("brs.snt"),
    BrokerRequestFailed("brs.fail"),
    // CommonStrategy (2)
    CommonHttpRequestExecute("cst.hreq"),
    CommonHttpResponseReceived("cst.hrsp")
}
