package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SsprContinueCommandParameters extends BaseNativeAuthCommandParameters {
    private static final String TAG = SsprContinueCommandParameters.class.getSimpleName();

    @NonNull
    public final String oobCode;
}