package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignUpSubmitPasswordCommandParameters extends SignUpContinueCommandParameters {
    private static final String TAG = SignUpSubmitPasswordCommandParameters.class.getSimpleName();

    @NonNull
    public final String signupToken;
    @NonNull
    public final String password;
}