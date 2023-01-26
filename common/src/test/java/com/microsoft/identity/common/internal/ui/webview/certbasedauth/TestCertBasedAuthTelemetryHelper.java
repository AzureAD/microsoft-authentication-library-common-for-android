package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthChoice;
import com.microsoft.identity.common.java.opentelemetry.ICertBasedAuthTelemetryHelper;

class TestCertBasedAuthTelemetryHelper implements ICertBasedAuthTelemetryHelper {

    @Override
    public void setCertBasedAuthChallengeHandler(String challengeHandlerName) {

    }

    @Override
    public void setExistingPivProviderPresent(boolean present) {

    }

    @Override
    public void setResultSuccess() {

    }

    @Override
    public void setResultFailure(String message) {

    }

    @Override
    public void setResultFailure(Exception exception) {

    }

    @Override
    public void setUserChoice(CertBasedAuthChoice choice) {

    }
}
