//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.java.providers;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.Map;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Reads the MAM-CA marker off the broker-install redirect
 * ({@code ...?wpj=1&username=<upn>&app_link=<play link>&intuneAppProtection=1}).
 * <p>
 * Not every broker install is a MAM Conditional Access install: the same broker-install redirect
 * also drives ordinary device-registration installs. The server appends
 * {@code intuneAppProtection=1} to the install link when it fails the request with AADSTS50127
 * (broker app not present, for MAM), and that marker is what the MAM-CA behaviors key off - so an
 * ordinary broker install keeps behaving exactly as it does today.
 * <p>
 * The marker is a top-level query parameter on the redirect, appended after {@code app_link};
 * it is not part of the {@code app_link} value.
 */
public final class MamCaRedirect {

    /** Query-parameter name the server uses to mark the MAM Conditional Access install path. */
    public static final String KEY_INTUNE_APP_PROTECTION = "intuneAppProtection";

    /** The value of {@link #KEY_INTUNE_APP_PROTECTION} that marks a redirect as MAM-CA. */
    public static final String VALUE_INTUNE_APP_PROTECTION_ENABLED = "1";

    /** Query-parameter name carrying the UPN of the user who was blocked. */
    public static final String KEY_USERNAME = "username";

    private MamCaRedirect() {
        // Utility class.
    }

    /**
     * Whether the broker-install redirect is marked as the MAM Conditional Access path.
     * <p>
     * This is a pure read of what the server sent: an unmarked redirect is an ordinary
     * device-registration install and must keep its existing behavior.
     *
     * @param redirectParameters query parameters of the broker-install redirect.
     * @return {@code true} when the MAM-CA behaviors should apply to this install.
     */
    public static boolean isMamCaInstall(@Nullable final Map<String, String> redirectParameters) {
        return redirectParameters != null
                && VALUE_INTUNE_APP_PROTECTION_ENABLED.equals(
                        redirectParameters.get(KEY_INTUNE_APP_PROTECTION));
    }

    /**
     * The UPN of the blocked user, as carried on the broker-install redirect.
     *
     * @param redirectParameters query parameters of the broker-install redirect.
     * @return the UPN, or {@code null} if the redirect did not carry one.
     */
    @Nullable
    public static String getUsername(@Nullable final Map<String, String> redirectParameters) {
        if (redirectParameters == null) {
            return null;
        }
        final String username = redirectParameters.get(KEY_USERNAME);
        return StringUtil.isNullOrEmpty(username) ? null : username;
    }

    /**
     * Logs which parameters the broker-install redirect carried, by <em>name</em> only.
     * <p>
     * The redirect carries the user's UPN, so the URL itself is never logged. The key names are not
     * user data, and they are what tells us in the field whether a given install was marked with
     * {@link #KEY_INTUNE_APP_PROTECTION}.
     *
     * @param callerTag          the caller's method tag, used as the log tag.
     * @param redirectParameters query parameters of the broker-install redirect.
     */
    public static void logRedirectParameterNames(@NonNull final String callerTag,
                                                 @Nullable final Map<String, String> redirectParameters) {
        if (redirectParameters == null) {
            return;
        }
        // Sorted so the line is stable and diffable across runs.
        Logger.info(callerTag, "Broker-install redirect carried parameters: "
                + new TreeSet<>(redirectParameters.keySet()));
    }
}
