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
package com.microsoft.identity.common.java.providers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.NonNull;

/**
 * Cross-layer, in-memory registry of interactive requests that have been <em>parked</em> for
 * broker-install request resume.
 *
 * <p>When an interactive request is blocked by a Conditional-Access policy that requires installing
 * the broker (Company Portal), the request is not failed back to the calling app. Instead it is
 * parked: the original {@code CommandCallback} is held (by the Android-side resume coordinator) and
 * the request's correlation id is registered here. {@code CommandDispatcher} consults this registry
 * and <strong>suppresses</strong> the normal error callback for a parked command, so the app never
 * receives the {@code BROKER_INSTALLATION} interrupt. After the broker is installed and the request
 * is resumed in broker context, the resume coordinator fires the original callback with the token.
 *
 * <p>This class lives in {@code common4j} (pure Java) so {@code CommandDispatcher} can reference it
 * without depending on Android-only code. It holds only correlation ids — never callbacks, params,
 * tokens, or PII. The Android-side coordinator holds the callback/parameters keyed by the same id.
 *
 * <p>Entries are strictly in-memory: if the process dies during the Play Store install, the parked
 * request is lost (an accepted trade-off — the app simply falls back to its normal blocked-install
 * behavior on the next attempt).
 */
public final class BrokerInstallResumeParkRegistry {

    private static final Set<String> PARKED_CORRELATION_IDS = ConcurrentHashMap.newKeySet();

    private BrokerInstallResumeParkRegistry() {
        // no instances
    }

    /**
     * Marks the request identified by {@code correlationId} as parked so its error callback is
     * suppressed by {@code CommandDispatcher}.
     */
    public static void park(@NonNull final String correlationId) {
        PARKED_CORRELATION_IDS.add(correlationId);
    }

    /**
     * @return true if the request identified by {@code correlationId} is currently parked.
     */
    public static boolean isParked(final String correlationId) {
        return correlationId != null && PARKED_CORRELATION_IDS.contains(correlationId);
    }

    /**
     * Clears the parked marker for {@code correlationId}. Called by the resume coordinator right
     * before it re-dispatches the request in broker context, so the resumed request completes and
     * delivers its result normally (it is no longer suppressed).
     */
    public static void unpark(final String correlationId) {
        if (correlationId != null) {
            PARKED_CORRELATION_IDS.remove(correlationId);
        }
    }
}
