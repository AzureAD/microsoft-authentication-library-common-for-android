package com.microsoft.identity.common.shadows;

import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthChoice;
import com.microsoft.identity.common.java.opentelemetry.CertBasedAuthTelemetryHelper;
import com.microsoft.identity.common.logging.Logger;

import org.robolectric.annotation.Implements;

@Implements(CertBasedAuthTelemetryHelper.class)
public class ShadowCertBasedAuthTelemetryHelper {

    public void setCertBasedAuthChallengeHandler(String challengeHandlerName) {
        Logger.info("hello", "testing");
    }

    public void setExistingPivProviderPresent(boolean present) {}

    public void setResultSuccess() {}

    public void setResultFailure(String message) {}

    public void setResultFailure(Exception exception) {}

    public void setUserChoice(CertBasedAuthChoice choice) {}
}
