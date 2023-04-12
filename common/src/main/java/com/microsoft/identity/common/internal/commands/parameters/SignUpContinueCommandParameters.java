package com.microsoft.identity.common.internal.commands.parameters;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
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

