package com.microsoft.identity.labapi.utilities.constants;

import lombok.NonNull;

public enum ResetOperation {
    MFA(LabConstants.ResetOperation.MFA),
    PASSWORD(LabConstants.ResetOperation.PASSWORD);

    final String value;

    ResetOperation(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ResetOperation fromName(@NonNull final String name) {
        return valueOf(ResetOperation.class, name.toUpperCase());
    }
}
