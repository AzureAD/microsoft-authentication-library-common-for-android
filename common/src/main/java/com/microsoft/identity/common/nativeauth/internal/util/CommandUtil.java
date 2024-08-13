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

import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASubmitChallengeCommandParameters;
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
     * Adds scopes to [SignInStartCommandParameters] object and returns a new
     * [SignInStartCommandParameters] object.
     * @param parameters input command parameter
     * @param defaultScopes scopes to be added
     * @return [SignInStartCommandParameters] object with scopes
     */
    public static SignInStartCommandParameters createSignInStartCommandParametersWithScopes(
            SignInStartCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .correlationId(parameters.getCorrelationId())
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
                .correlationId(parameters.getCorrelationId())
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
                .correlationId(parameters.getCorrelationId())
                .build();
    }

    /**
     * Adds scopes to [SignInSubmitPasswordCommandParameters] object and returns a new
     * [SignInSubmitCodeCommandParameters] object.
     * @param parameters input command parameter
     * @param correlationId correlationId to be used in the request
     * @param defaultScopes scopes to be added
     * @return [SignInSubmitPasswordCommandParameters] object with scopes
     */
    public static SignInSubmitPasswordCommandParameters createSignInSubmitPasswordCommandParametersWithScopes(
            SignInSubmitPasswordCommandParameters parameters,
            String correlationId,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Adds continuation token to [SignInStartCommandParameters] object and returns a new
     * [SignInSubmitPasswordCommandParameters] object.
     * @param parameters input command parameter
     * @param correlationId correlationId to be used in the request
     * @param continuationToken continuation token to be added
     * @return [SignInSubmitPasswordCommandParameters] object with continuation token
     */
    public static SignInSubmitPasswordCommandParameters createSignInSubmitPasswordCommandParameters(
            SignInStartCommandParameters parameters,
            String correlationId,
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
                        .correlationId(correlationId)
                        .challengeType(parameters.getChallengeType())
                        .build();

        return commandParameters;
    }

    /**
     * Adds scopes to [MFASubmitChallengeCommandParameters] object and returns a new
     * [MFASubmitChallengeCommandParameters] object.
     * @param parameters input command parameter
     * @param defaultScopes scopes to be added
     * @return [MFASubmitChallengeCommandParameters] object with scopes
     */
    public static MFASubmitChallengeCommandParameters createMFASubmitChallengeCommandParametersWithScopes(
            MFASubmitChallengeCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .correlationId(parameters.getCorrelationId())
                .build();
    }

    /**
     * Adds continuation token to [SignInStartCommandParameters] object and returns a new
     * [SignInSubmitPasswordCommandParameters] object.
     * @param parameters input command parameter
     * @return [SignInSubmitPasswordCommandParameters] object with continuation token
     */
    public static SignInSubmitCodeCommandParameters createSignInSubmitCodeCommandParameters(
            MFASubmitChallengeCommandParameters parameters
    ) {
        final SignInSubmitCodeCommandParameters commandParameters =
                SignInSubmitCodeCommandParameters.builder()
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
                        .continuationToken(parameters.continuationToken)
                        .code(parameters.challenge)
                        .scopes(parameters.getScopes())
                        .correlationId(parameters.getCorrelationId())
                        .challengeType(parameters.getChallengeType())
                        .build();

        return commandParameters;
    }
}
