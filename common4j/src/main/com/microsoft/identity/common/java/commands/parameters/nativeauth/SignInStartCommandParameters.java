package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignInStartCommandParameters extends BaseSignInStartCommandParameters {
    private static final String TAG = SignInStartCommandParameters.class.getSimpleName();

}