package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignInSubmitPasswordCommandParameters extends BaseSignInTokenCommandParameters {
	private static final String TAG = SignInSubmitCodeCommandParameters.class.getSimpleName();

	@NonNull
	public final String password;
	@NonNull
	public final String credentialToken;
}