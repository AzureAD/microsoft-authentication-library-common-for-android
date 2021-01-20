package com.microsoft.identity.common.internal.commands.parameters;

import com.google.gson.annotations.Expose;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public class CalculationCommandParameters extends CommandParameters {

    @Expose
    private float first;

    @Expose
    private float second;

    @Expose
    private char operator;


}
