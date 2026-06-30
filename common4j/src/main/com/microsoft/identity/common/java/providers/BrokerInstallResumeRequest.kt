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
package com.microsoft.identity.common.java.providers

/**
 * Snapshot of an in-flight interactive request, persisted by the 1P app immediately before it is
 * redirected to install a broker (Company Portal) for a Conditional-Access policy. After install,
 * the request is resumed silently in broker context (see design: broker-install request resume).
 *
 * Keyed by [correlationId]. Intentionally carries **no secrets** — no tokens, refresh tokens, or
 * client secrets — so it is safe to persist on-device and reference via an install-referrer pointer.
 *
 * The snapshot carries the **full** set of interactive request parameters (not just a subset) so
 * the resumed request faithfully reproduces the original — matching the production controller-level
 * resume rather than a lossy reconstruction.
 *
 * @property correlationId Single-use key carried as the install-referrer pointer; must be unique.
 * @property authority Authority URL of the original request.
 * @property clientId Calling app's client id.
 * @property redirectUri Redirect URI used to return the user to the originating app.
 * @property scopes Requested scopes; empty when none.
 * @property extraScopesToConsent Additional scopes to consent to alongside [scopes]; empty when none.
 * @property loginHint Optional login hint; stays on-device, never placed in the referrer.
 * @property claims Optional claims-request JSON carried on the original request.
 * @property prompt Optional prompt behavior (e.g. SELECT_ACCOUNT) of the original request.
 * @property extraQueryParameters Optional extra query parameters (URL-encoded form) of the original request.
 * @property createdAtMs Epoch millis when the request was persisted.
 * @property ttlMs Time-to-live in millis; defaults to [DEFAULT_TTL_MS].
 */
data class BrokerInstallResumeRequest(
    val correlationId: String,
    val authority: String,
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String> = emptyList(),
    val extraScopesToConsent: List<String> = emptyList(),
    val loginHint: String? = null,
    val claims: String? = null,
    val prompt: String? = null,
    val extraQueryParameters: String? = null,
    val createdAtMs: Long,
    val ttlMs: Long = DEFAULT_TTL_MS
) {
    /**
     * @return true if the request is older than its TTL relative to [nowMs] and must not be resumed.
     */
    fun isExpired(nowMs: Long): Boolean = nowMs - createdAtMs >= ttlMs

    companion object {
        /** Default resume window: 5 minutes. Within the 5–10 min bound agreed for this feature. */
        const val DEFAULT_TTL_MS: Long = 5 * 60 * 1000L
    }
}
