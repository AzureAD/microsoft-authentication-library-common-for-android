package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignInStartUsingPasswordCommandParameters extends BaseSignInStartCommandParameters {
    private static final String TAG = SignInStartUsingPasswordCommandParameters.class.getSimpleName();

    @NonNull
    public final String password;
}