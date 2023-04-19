package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignInCommandParameters extends BaseNativeAuthCommandParameters {
    private static final String TAG = SignInCommandParameters.class.getSimpleName();

    @NonNull
    public final String username;
    public final String password;
    public final String oob;
    public final String scope;
}