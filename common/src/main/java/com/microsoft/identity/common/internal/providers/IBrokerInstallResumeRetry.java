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
package com.microsoft.identity.common.internal.providers;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.commands.parameters.AndroidInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

/**
 * Retry callback supplied by the broker-path caller (OneAuth's {@code BrokerClient}) to
 * {@link BrokerInstallResumeCoordinator} for the broker-install auto-resume flow.
 *
 * <p>The coordinator owns <em>park / await / resume</em>: it holds the blocked
 * {@code acquireToken} thread on a future, launches the Company-Portal install, and — when the
 * post-install deep link arrives — invalidates the stale broker-discovery cache and asks this
 * callback to re-acquire the token.
 *
 * <p>The caller owns <em>broker rebinding</em>, which only it can do: when the original request ran
 * the broker was not installed, so no broker controller could be constructed and any active-broker
 * package resolved to nothing. On resume the caller must perform a <strong>fresh, cache-skipping
 * active-broker discovery</strong> (the just-installed Company Portal is now present) and build a
 * controller bound to it before acquiring — reusing the pre-install controller would target a broker
 * that did not exist. Clearing the coordinator's discovery cache alone is necessary but not
 * sufficient; the caller's in-memory controller/strategies must be rebuilt too.
 */
public interface IBrokerInstallResumeRetry {

    /**
     * Re-acquire the token in a freshly discovered broker context after Company Portal is installed.
     *
     * @param resumeParams the original request parameters, re-based onto the foreground resume
     *                     activity + platform components and with the broker-install URL cleared so
     *                     the retry runs the normal broker path and never re-parks.
     * @return the acquire result to deliver on the caller's original sink.
     * @throws Exception if broker rebinding or the acquire fails; the coordinator completes the
     *                  parked future exceptionally so the caller surfaces a real error instead of
     *                  hanging until the resume timeout.
     */
    @NonNull
    AcquireTokenResult retryInBrokerContext(
            @NonNull AndroidInteractiveTokenCommandParameters resumeParams) throws Exception;
}
