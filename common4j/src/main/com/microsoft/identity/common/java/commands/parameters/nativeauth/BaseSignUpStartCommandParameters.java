package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class BaseSignUpStartCommandParameters extends BaseNativeAuthCommandParameters {
    private static final String TAG = BaseSignUpStartCommandParameters.class.getSimpleName();

    @NonNull
    public final String username;
    @EqualsAndHashCode.Exclude
    @Nullable
    public final Map<String, String> userAttributes;
}
