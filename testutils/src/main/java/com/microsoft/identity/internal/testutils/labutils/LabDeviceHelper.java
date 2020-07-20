package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.DeleteDeviceApi;
import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;

public class LabDeviceHelper {

    public static boolean deleteDevice(final String upn, final String deviceId) {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        DeleteDeviceApi deleteDeviceApi = new DeleteDeviceApi();

        final CustomSuccessResponse customSuccessResponse;
        try {
            customSuccessResponse = deleteDeviceApi.delete(upn, deviceId);

            return customSuccessResponse.getResult().contains(
                    deviceId + ", successfully deleted from AAD."
            );
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
