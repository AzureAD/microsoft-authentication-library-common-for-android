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

import com.microsoft.identity.common.java.logging.Logger
import java.util.TreeSet
import java.util.regex.Pattern

/**
 * Reads the MAM-CA marker off the broker-install redirect
 * (`...?wpj=1&username=<upn>&app_link=<play link>&intuneAppProtection=1`).
 *
 * Not every broker install is a MAM Conditional Access install: the same broker-install redirect
 * also drives ordinary device-registration installs. The server appends `intuneAppProtection=1`
 * to the install link when it fails the request with AADSTS50127 (broker app not present, for
 * MAM), and that marker is what the MAM-CA behaviors key off - so an ordinary broker install keeps
 * behaving exactly as it does today.
 *
 * The marker is a top-level query parameter on the redirect, appended after `app_link`;
 * it is not part of the `app_link` value.
 *
 * **Shared between the two Phase 1 pull requests.** This file is byte-identical in the
 * install-referrer PR and the UPN-hint PR so that whichever merges second does not conflict.
 * Each PR therefore lands one accessor whose caller arrives with its sibling:
 * [getUsername] is read by `MamUpnHintStore`, and [logRedirectParameterNames] by
 * `MamInstallReferrerBuilder`. Both callers exist by the end of Phase 1.
 */
object MamCaRedirect {

    /** Query-parameter name the server uses to mark the MAM Conditional Access install path. */
    const val KEY_INTUNE_APP_PROTECTION = "intuneAppProtection"

    /** The value of [KEY_INTUNE_APP_PROTECTION] that marks a redirect as MAM-CA. */
    const val VALUE_INTUNE_APP_PROTECTION_ENABLED = "1"

    /** Query-parameter name carrying the UPN of the user who was blocked. */
    const val KEY_USERNAME = "username"

    /**
     * The shape of an ordinary query-parameter name. Used to keep values that the parser mistook
     * for names - a trailing token with no `=` - out of the logs, since one of them could be a UPN.
     */
    private val PARAMETER_NAME = Pattern.compile("[A-Za-z0-9_.\\-]{1,64}")

    /**
     * Whether the broker-install redirect is marked as the MAM Conditional Access path.
     *
     * This is a pure read of what the server sent: an unmarked redirect is an ordinary
     * device-registration install and must keep its existing behavior.
     *
     * @param redirectParameters query parameters of the broker-install redirect.
     * @return `true` when the MAM-CA behaviors should apply to this install.
     */
    @JvmStatic
    fun isMamCaInstall(redirectParameters: Map<String, String>?): Boolean =
        redirectParameters?.get(KEY_INTUNE_APP_PROTECTION) == VALUE_INTUNE_APP_PROTECTION_ENABLED

    /**
     * The UPN of the blocked user, as carried on the broker-install redirect.
     *
     * @param redirectParameters query parameters of the broker-install redirect.
     * @return the UPN, or `null` if the redirect did not carry one.
     */
    @JvmStatic
    fun getUsername(redirectParameters: Map<String, String>?): String? =
        redirectParameters?.get(KEY_USERNAME)?.takeIf { it.isNotEmpty() }

    /**
     * Logs which parameters the broker-install redirect carried, by *name* only.
     *
     * The redirect carries the user's UPN, so the URL itself is never logged. Names alone are not
     * user data, and they are what tells us in the field whether a given install was marked with
     * [KEY_INTUNE_APP_PROTECTION].
     *
     * Names are not simply trusted to *be* names, though. The query parser turns a token with no
     * `=` into a key with a null value, so a redirect ending in a bare `?user@contoso.com` would
     * present the UPN itself as a key. Only names matching an ordinary parameter shape are printed;
     * anything else is counted rather than logged.
     *
     * @param callerTag          the caller's method tag, used as the log tag.
     * @param redirectParameters query parameters of the broker-install redirect.
     */
    @JvmStatic
    fun logRedirectParameterNames(callerTag: String, redirectParameters: Map<String, String>?) {
        if (redirectParameters == null) {
            return
        }

        val names = printableParameterNames(redirectParameters.keys)
        val withheld = redirectParameters.size - names.size
        val suffix = if (withheld > 0) " ($withheld withheld)" else ""

        Logger.info(callerTag, "Broker-install redirect carried parameters: $names$suffix")
    }

    /**
     * The subset of [keys] that is safe to log, sorted so the line is stable across runs.
     *
     * @param keys parameter names as the query parser produced them.
     * @return only those matching an ordinary parameter-name shape.
     */
    internal fun printableParameterNames(keys: Set<String?>): Set<String> {
        val printable = TreeSet<String>()
        for (key in keys) {
            if (key != null && PARAMETER_NAME.matcher(key).matches()) {
                printable.add(key)
            }
        }
        return printable
    }
}
