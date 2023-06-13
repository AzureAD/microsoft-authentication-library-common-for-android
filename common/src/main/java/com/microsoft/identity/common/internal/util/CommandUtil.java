package com.microsoft.identity.common.internal.util;

import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartUsingPasswordCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters;

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
}
