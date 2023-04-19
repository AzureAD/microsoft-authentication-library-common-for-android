package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignUpContinueCommandParameters extends BaseNativeAuthCommandParameters {
	private static final String TAG = SignUpContinueCommandParameters.class.getSimpleName();

	public final String password;
	public final String oob;
	// TODO @EqualsAndHashCode.Exclude?
	public final UserAttributes userAttributes;
}

