package com.microsoft.identity.common.internal.commands.parameters;

import com.google.gson.annotations.Expose;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SilentTokenCommandParameters extends TokenCommandParameters {

    @Expose()
    private boolean forceRefresh;
}
