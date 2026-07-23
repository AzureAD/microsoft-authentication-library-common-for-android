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

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SDK-specific adapter that re-issues an interactive sign-in when a durable broker-install park is
 * resumed on a fresh process. Common's {@link BrokerInstallResumeAutoInitProvider} owns <em>when</em> to
 * resume (durable store, foreground observation, readiness polling, single-shot); the host SDK owns
 * <em>how</em> to issue the interactive request (e.g. OneAuth's {@code signInInteractively}), which is
 * what this interface abstracts.
 * <p>
 * The glue registers an implementation once at SDK init via
 * {@link BrokerInstallResumeAutoInitProvider#registerReDriver(IBrokerInstallResumeReDriver)}.
 */
public interface IBrokerInstallResumeReDriver {

    /**
     * @return {@code true} once the SDK is configured enough to issue an interactive request (e.g. an
     *         authenticator instance exists). The provider polls this after a foreground until ready.
     */
    boolean isReady();

    /**
     * Re-issue the parked interactive sign-in on the given foreground activity. Called at most once per
     * durable park, only after {@link #isReady()} returns {@code true} and Company Portal is a valid broker.
     *
     * @param activity  the current foreground activity to host the interactive UI.
     * @param loginHint the parked UPN / {@code login_hint} (may be empty).
     * @param target    the parked resource/scopes to request (may be empty; the impl supplies a default).
     */
    void reDriveInteractive(@NonNull Activity activity,
                            @Nullable String loginHint,
                            @Nullable String target);
}
