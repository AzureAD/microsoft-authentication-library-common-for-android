package com.microsoft.identity.common.internal.util;

import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.AcquireTokenNoFixedScopesCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartUsingPasswordCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInWithSLTCommandParameters;
import com.microsoft.identity.common.java.request.SdkType;

import java.util.List;

/**
 * This is a Java utils class that helps Kotlin files with accessing Lombok's builder (as this isn't
 * fully compatible with Kotlin).
 */
public class CommandUtil {
    public static SignInStartUsingPasswordCommandParameters createSignInStartUsingPasswordCommandParametersWithScopes(
            SignInStartUsingPasswordCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .build();
    }

    public static SignInStartCommandParameters createSignInStartCommandParametersWithScopes(
            SignInStartCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .build();
    }

    public static SignInWithSLTCommandParameters createSignInWithSLTCommandParametersWithScopes(
            SignInWithSLTCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .build();
    }

    public static SignInSubmitCodeCommandParameters createSignInSubmitCodeCommandParametersWithScopes(
            SignInSubmitCodeCommandParameters parameters,
            List<String> defaultScopes
    ) {
        return parameters.toBuilder()
                .scopes(defaultScopes)
                .build();
    }

	public static SignInSubmitPasswordCommandParameters createSignInSubmitPasswordCommandParametersWithScopes(
			SignInSubmitPasswordCommandParameters parameters,
			List<String> defaultScopes
	) {
		return parameters.toBuilder()
				.scopes(defaultScopes)
				.build();
	}

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
				.claimsRequestJson(parameters.getClaimsRequestJson())
				.forceRefresh(parameters.isForceRefresh())
				.account(parameters.getAccount())
				.authenticationScheme(parameters.getAuthenticationScheme())
				.powerOptCheckEnabled(parameters.isPowerOptCheckEnabled())
				.correlationId(parameters.getCorrelationId())
				.build();

		return commandParameters;
	}
}
