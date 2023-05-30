package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignUpSubmitCodeCommandParameters extends SignUpContinueCommandParameters {
    private static final String TAG = SignUpSubmitCodeCommandParameters.class.getSimpleName();

    @NonNull
    public final String signupToken;
    @NonNull
    public final String code;
}
