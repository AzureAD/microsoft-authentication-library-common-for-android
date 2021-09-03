package com.microsoft.identity.labapi.utilities.constants;

public enum AppPlatform {
    WEB(LabConstants.AppPlatform.WEB),
    SPA(LabConstants.AppPlatform.SPA);

    final String value;

    AppPlatform(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
