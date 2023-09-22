package com.microsoft.identity.common.java.controllers;

import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;

public class AcquireTokenAndSaveResultStrategyFactory {
    public static IAcquireTokenAndSaveResultStrategy createATAndSaveResultStrategy(TokenCommandParameters parameters) {
        if (parameters.hasNestedAppParameters()) {
            return new BaseNAAImplementation();
        } else {
            return new AcquireTokenAndSaveResultStrategy();
        }
    }
}
