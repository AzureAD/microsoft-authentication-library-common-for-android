package com.microsoft.identity.common.internal.controllers;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.internal.request.SdkType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public class CommandDispatcherConfiguration {

    @Expose()
    private int maxTheadPoolInteractive;

}
