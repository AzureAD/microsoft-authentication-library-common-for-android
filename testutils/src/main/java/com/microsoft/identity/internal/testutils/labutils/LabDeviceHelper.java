//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.DeleteDeviceApi;
import com.microsoft.identity.internal.test.labapi.api.LabSecretApi;
import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;
import com.microsoft.identity.internal.test.labapi.model.SecretResponse;
import com.microsoft.identity.labapi.utilities.client.LabClient;

/**
 * Utilities to interact with Lab {@link DeleteDeviceApi}.
 */
public class LabDeviceHelper {

    public static final ConfidentialClientHelper INSTANCE = LabAuthenticationHelper.getInstance();

    /**
     * Deletes the provided device from the directory.
     *
     * @param upn      the upn to whom this device is associated
     * @param deviceId the device id of the device to delete
     * @return a boolean indicating if the device has been deleted successfully
     */
    public static boolean deleteDevice(final String upn, final String deviceId) throws LabApiException {
        INSTANCE.setupApiClientWithAccessToken();
        final DeleteDeviceApi deleteDeviceApi = new DeleteDeviceApi();

        try {
            final CustomSuccessResponse customSuccessResponse;
            customSuccessResponse = deleteDeviceApi.apiDeleteDeviceDelete(upn, deviceId);

            if (customSuccessResponse == null) {
                return false;
            }

            final String expectedResult = "Device removed Successfully.";
            return expectedResult.equalsIgnoreCase(customSuccessResponse.getMessage());
        } catch (final ApiException e) {
            throw new LabApiException(e);
        }
    }

    private static String getSecret(final String secretName) {
        final LabSecretApi labSecretApi = new LabSecretApi();

        try {
            final SecretResponse secretResponse = labSecretApi.apiLabSecretGet(secretName);
            return secretResponse.getValue();
        } catch (final com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Failed to fetch secret", ex);
        }
    }
}
