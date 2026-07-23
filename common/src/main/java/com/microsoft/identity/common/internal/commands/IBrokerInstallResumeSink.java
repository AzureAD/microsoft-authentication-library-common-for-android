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
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.microsoft.identity.common.internal.commands;

/**
 * The pair of terminal actions a {@link BrokerInstallResumeSinkWaiter} invokes on exactly one of its two
 * outcomes. The SDK glue supplies an implementation that closes over its host-specific request sink (e.g.
 * OneAuth's native event sink), keeping this orchestration decoupled from any particular SDK.
 * <p>
 * Exactly one method is called per wait, and only after the wait resolves.
 */
public interface IBrokerInstallResumeSink {

    /**
     * Company Portal became a valid broker within the TTL. The glue should re-deliver the broker-install
     * result to its held request so the native/broker retry runs against the now-present broker.
     */
    void onBrokerAvailable();

    /**
     * Company Portal never appeared within the TTL. The glue should deliver the original terminal error so
     * the request never hangs.
     */
    void onGiveUp();
}
