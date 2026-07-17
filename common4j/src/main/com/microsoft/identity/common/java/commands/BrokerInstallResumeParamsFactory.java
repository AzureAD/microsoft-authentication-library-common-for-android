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

package com.microsoft.identity.common.java.commands;

import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.util.StringUtil;

import lombok.NonNull;

/**
 * Projects a parked interactive request's parameters into the silent-request parameters used to replay
 * it through the freshly installed broker on resume (PBI-4).
 * <p>
 * Both {@link InteractiveTokenCommandParameters} and {@link SilentTokenCommandParameters} extend
 * {@code TokenCommandParameters}, but they are distinct concrete builder types, so the shared fields are
 * copied explicitly. The one intentional change is that {@code login_hint} is set to the WPJ username
 * (UPN) captured at park time, so the silent broker retry can identify the account without interaction.
 */
public final class BrokerInstallResumeParamsFactory {

    private BrokerInstallResumeParamsFactory() {
    }

    /**
     * Builds silent-request parameters equivalent to the parked interactive request, with
     * {@code login_hint} set to the supplied UPN.
     *
     * @param interactive the parked interactive request parameters.
     * @param upn         the WPJ username (UPN) to use as {@code login_hint}; if null/blank, the
     *                    interactive request's existing {@code login_hint} is preserved.
     * @return the projected silent-request parameters.
     */
    @NonNull
    public static SilentTokenCommandParameters toSilentParameters(
            @NonNull final InteractiveTokenCommandParameters interactive,
            final String upn) {
        final String loginHint = StringUtil.isNullOrEmpty(upn) ? interactive.getLoginHint() : upn;

        return SilentTokenCommandParameters.builder()
                // CommandParameters (base)
                .platformComponents(interactive.getPlatformComponents())
                .oAuth2TokenCache(interactive.getOAuth2TokenCache())
                .isSharedDevice(interactive.isSharedDevice())
                .applicationName(interactive.getApplicationName())
                .applicationVersion(interactive.getApplicationVersion())
                .requiredBrokerProtocolVersion(interactive.getRequiredBrokerProtocolVersion())
                .sdkType(interactive.getSdkType())
                .sdkVersion(interactive.getSdkVersion())
                .clientId(interactive.getClientId())
                .redirectUri(interactive.getRedirectUri())
                .childClientId(interactive.getChildClientId())
                .childRedirectUri(interactive.getChildRedirectUri())
                .powerOptCheckEnabled(interactive.isPowerOptCheckEnabled())
                .callerPackageName(interactive.getCallerPackageName())
                .callerSignature(interactive.getCallerSignature())
                .correlationId(interactive.getCorrelationId())
                .spanContext(interactive.getSpanContext())
                .flightInformation(interactive.getFlightInformation())
                // TokenCommandParameters
                .account(interactive.getAccount())
                .scopes(interactive.getScopes())
                .authority(interactive.getAuthority())
                .claimsRequestJson(interactive.getClaimsRequestJson())
                .authenticationScheme(interactive.getAuthenticationScheme())
                .mamEnrollmentId(interactive.getMamEnrollmentId())
                .forceRefresh(interactive.isForceRefresh())
                .loginHint(loginHint)
                .domainHint(interactive.getDomainHint())
                .extraOptions(interactive.getExtraOptions())
                .extraTokenBodyParameters(interactive.getExtraTokenBodyParameters())
                .build();
    }
}
