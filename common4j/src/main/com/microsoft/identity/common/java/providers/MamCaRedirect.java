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

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.Map;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Reads the MAM-CA markers off the broker-install redirect
 * ({@code msauth://wpj/?username=<upn>&app_link=<play link>&intuneAppProtection=1}).
 * <p>
 * Not every broker install is a MAM Conditional Access install: the same {@code msauth://wpj}
 * redirect also drives ordinary device-registration installs. The server marks the MAM-CA path with
 * {@code intuneAppProtection=1}, and that marker is what the MAM-CA behaviors key off - so an
 * ordinary broker install keeps behaving exactly as it does today.
 * <p>
 * <b>Before the server marker ships.</b> Until the server emits {@code intuneAppProtection=1},
 * {@link CommonFlight#ENABLE_MAM_CA_INSTALL_WITHOUT_MARKER} (default off) treats an unmarked
 * broker-install redirect as MAM-CA, so the client behavior can be validated and rolled out ahead
 * of the server change. Turn it off once the marker is live to get the intended
 * MAM-CA-only scoping back.
 */
public final class MamCaRedirect {

    private static final String TAG = MamCaRedirect.class.getSimpleName();

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
     * Whether the broker-install redirect is marked as the MAM Conditional Access path, honouring
     * {@link CommonFlight#ENABLE_MAM_CA_INSTALL_WITHOUT_MARKER} while the server marker is rolling
     * out.
     *
     * @param redirectParameters query parameters of the {@code msauth://wpj} redirect.
     * @return {@code true} when the MAM-CA behaviors should apply to this install.
     */
    public static boolean isMamCaInstall(@Nullable final Map<String, String> redirectParameters) {
        final String methodTag = TAG + ":isMamCaInstall";

        if (hasIntuneAppProtectionMarker(redirectParameters)) {
            return true;
        }

        if (CommonFlightsManager.INSTANCE.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_MAM_CA_INSTALL_WITHOUT_MARKER)) {
            Logger.info(methodTag, "No " + KEY_INTUNE_APP_PROTECTION
                    + " marker on the broker-install redirect; treating it as MAM-CA because "
                    + CommonFlight.ENABLE_MAM_CA_INSTALL_WITHOUT_MARKER.getKey() + " is on.");
            return true;
        }

        Logger.info(methodTag, "Broker-install redirect is not marked "
                + KEY_INTUNE_APP_PROTECTION + "=" + VALUE_INTUNE_APP_PROTECTION_ENABLED
                + "; leaving the install flow unchanged.");
        return false;
    }

    /**
     * Whether the redirect literally carries {@code intuneAppProtection=1}, ignoring any flight.
     *
     * @param redirectParameters query parameters of the {@code msauth://wpj} redirect.
     * @return {@code true} only if the server marker is present.
     */
    public static boolean hasIntuneAppProtectionMarker(
            @Nullable final Map<String, String> redirectParameters) {
        return redirectParameters != null
                && VALUE_INTUNE_APP_PROTECTION_ENABLED.equals(
                        redirectParameters.get(KEY_INTUNE_APP_PROTECTION));
    }

    /**
     * The UPN of the blocked user, as carried on the broker-install redirect.
     *
     * @param redirectParameters query parameters of the {@code msauth://wpj} redirect.
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
     * user data, and they are what tells us in the field whether the server is emitting the
     * {@link #KEY_INTUNE_APP_PROTECTION} marker yet.
     *
     * @param callerTag          the caller's method tag, used as the log tag.
     * @param redirectParameters query parameters of the {@code msauth://wpj} redirect.
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
