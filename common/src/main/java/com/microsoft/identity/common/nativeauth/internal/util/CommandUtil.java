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
package com.microsoft.identity.common.nativeauth.internal.util;

import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.AcquireTokenNoFixedScopesCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters;
import com.microsoft.identity.common.java.request.SdkType;

import java.util.List;

/**
 * This is a Java utils class that helps Kotlin files with accessing Lombok's builder (as this isn't
 * fully compatible with Kotlin).
 */
public class CommandUtil {

    /**
     * Adds scopes to [SignInStartUsingPasswordCommandParameters] object and returns a new
     * [SignInStartUsingPasswordCommandParameters] object.
     * @param parameters input command parameter
     * @param defaultScopes scopes to be added
     * @return [SignInStartUsingPasswordCommandParameters] object with scopes
     */
    public static SignInStartCommandParameters createSignInStartCommandParametersWithScopes(
            SignInStartCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .build();
    }

    /**
     * Adds scopes to [SignInWithContinuationTokenCommandParameters] object and returns a new
     * [SignInWithContinuationTokenCommandParameters] object.
     * @param parameters input command parameter
     * @param defaultScopes scopes to be added
     * @return [SignInWithContinuationTokenCommandParameters] object with scopes
     */
    public static SignInWithContinuationTokenCommandParameters createSignInWithContinuationTokenCommandParametersWithScopes(
            SignInWithContinuationTokenCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .build();
    }

    /**
     * Adds scopes to [SignInSubmitCodeCommandParameters] object and returns a new
     * [SignInSubmitCodeCommandParameters] object.
     * @param parameters input command parameter
     * @param defaultScopes scopes to be added
     * @return [SignInSubmitCodeCommandParameters] object with scopes
     */
    public static SignInSubmitCodeCommandParameters createSignInSubmitCodeCommandParametersWithScopes(
            SignInSubmitCodeCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .build();
    }

    /**
     * Adds scopes to [SignInSubmitPasswordCommandParameters] object and returns a new
     * [SignInSubmitCodeCommandParameters] object.
     * @param parameters input command parameter
     * @param defaultScopes scopes to be added
     * @return [SignInSubmitPasswordCommandParameters] object with scopes
     */
    public static SignInSubmitPasswordCommandParameters createSignInSubmitPasswordCommandParametersWithScopes(
            SignInSubmitPasswordCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .build();
    }

    /**
     * Adds continuation token to [SignInStartUsingPasswordCommandParameters] object and returns a new
     * [SignInSubmitPasswordCommandParameters] object.
     * @param parameters input command parameter
     * @param continuationToken continuation token to be added
     * @return [SignInStartUsingPasswordCommandParameters] object with continuation token
     */
    public static SignInSubmitPasswordCommandParameters createSignInSubmitPasswordCommandParameters(
            SignInStartCommandParameters parameters,
            String continuationToken
    ) {
        final SignInSubmitPasswordCommandParameters commandParameters =
                SignInSubmitPasswordCommandParameters.builder()
                        .platformComponents(parameters.getPlatformComponents())
                        .applicationName(parameters.getApplicationName())
                        .applicationVersion(parameters.getApplicationVersion())
                        .clientId(parameters.getClientId())
                        .isSharedDevice(parameters.isSharedDevice())
                        .redirectUri(parameters.getRedirectUri())
                        .oAuth2TokenCache(parameters.getOAuth2TokenCache())
                        .requiredBrokerProtocolVersion(parameters.getRequiredBrokerProtocolVersion())
                        .sdkType(SdkType.MSAL)
                        .sdkVersion(parameters.getSdkVersion())
                        .powerOptCheckEnabled(parameters.isPowerOptCheckEnabled())
                        .authority(parameters.getAuthority())
                        .continuationToken(continuationToken)
                        .password(parameters.getPassword())
                        .scopes(parameters.getScopes())
                        .challengeType(parameters.getChallengeType())
                        .build();

        return commandParameters;
    }

    /**
     * Converts to [AcquireTokenNoFixedScopesCommandParameters] object to a new
     * [SilentTokenCommandParameters] object.
     * @param parameters input command parameter
     * @return [SilentTokenCommandParameters] object
     */
    public static SilentTokenCommandParameters convertAcquireTokenNoFixedScopesCommandParameters(
            AcquireTokenNoFixedScopesCommandParameters parameters
    ) {
        final SilentTokenCommandParameters commandParameters = SilentTokenCommandParameters
                .builder()
                .platformComponents(parameters.getPlatformComponents())
                .applicationName(parameters.getApplicationName())
                .applicationVersion(parameters.getApplicationVersion())
                .clientId(parameters.getClientId())
                .isSharedDevice(parameters.isSharedDevice())
                .oAuth2TokenCache(parameters.getOAuth2TokenCache())
                .redirectUri(parameters.getRedirectUri())
                .requiredBrokerProtocolVersion(parameters.getRequiredBrokerProtocolVersion())
                .sdkType(SdkType.MSAL)
                .sdkVersion(parameters.getSdkVersion())
                .authority(parameters.authority)
                .forceRefresh(parameters.isForceRefresh())
                .account(parameters.getAccount())
                .authenticationScheme(parameters.getAuthenticationScheme())
                .powerOptCheckEnabled(parameters.isPowerOptCheckEnabled())
                .correlationId(parameters.getCorrelationId())
                .build();

        return commandParameters;
    }
}
