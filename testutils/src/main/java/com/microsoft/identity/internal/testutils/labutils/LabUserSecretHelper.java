package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.api.LabUserSecretApi;
import com.microsoft.identity.internal.test.labapi.model.SecretResponse;

public class LabUserSecretHelper {

    public static String getPasswordForLab(final String labName) {
        LabAuthenticationHelper.setupApiClientWithAccessToken();
        LabUserSecretApi labUserSecretApi = new LabUserSecretApi();
        SecretResponse secretResponse;

        try {
            secretResponse = labUserSecretApi.getLabUserSecret(labName);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving lab password", ex);
        }

        return secretResponse.getValue();
    }
}
