package com.microsoft.identity.common.java.commands.parameters.nativeauth;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SsprChallengeCommandParameters extends BaseNativeAuthCommandParameters {
    private static final String TAG = SsprChallengeCommandParameters.class.getSimpleName();
}
