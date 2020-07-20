package com.microsoft.identity.internal.testutils.labutils;

import androidx.annotation.NonNull;

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.ResetApi;
import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;

public class LabResetHelper {

    public static boolean resetPassword(@NonNull final String upn) {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        ResetApi resetApi = new ResetApi();

        final CustomSuccessResponse customSuccessResponse;

        try {
            customSuccessResponse = resetApi.putResetInfo(upn, LabConstants.ResetOperation.PASSWORD);
            final String expectedResult = ("Password reset successful for user : " + upn)
                    .toLowerCase();
            return customSuccessResponse.getResult().toLowerCase().contains(expectedResult);
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean resetMfa(@NonNull final String upn) {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        ResetApi resetApi = new ResetApi();

        final CustomSuccessResponse customSuccessResponse;

        try {
            customSuccessResponse = resetApi.putResetInfo(upn, LabConstants.ResetOperation.MFA);
            return customSuccessResponse.getResult().contains(
                    "MFA reset successful for user : " + upn
            );
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
