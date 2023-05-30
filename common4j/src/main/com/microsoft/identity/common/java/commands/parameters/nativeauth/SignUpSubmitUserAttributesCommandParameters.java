package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignUpSubmitUserAttributesCommandParameters extends SignUpContinueCommandParameters {
    private static final String TAG = SignUpSubmitUserAttributesCommandParameters.class.getSimpleName();

    @NonNull
    public final String signupToken;
    @NonNull
    public final Map<String, String> userAttributes;
}
