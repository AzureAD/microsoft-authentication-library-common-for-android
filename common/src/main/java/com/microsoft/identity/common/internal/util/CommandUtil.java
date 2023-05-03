package com.microsoft.identity.common.internal.util;

import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartWithPasswordCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters;

import java.util.List;

public class CommandUtil {
	public static SignInStartWithPasswordCommandParameters createSignInStartWithPasswordCommandParametersWithScopes(
			SignInStartWithPasswordCommandParameters parameters,
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
}
