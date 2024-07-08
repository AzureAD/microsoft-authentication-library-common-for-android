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

import androidx.annotation.NonNull;

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.LabSecretApi;
import com.microsoft.identity.internal.test.labapi.api.ResetApi;
import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;
import com.microsoft.identity.internal.test.labapi.model.SecretResponse;
import com.microsoft.identity.labapi.utilities.client.LabClient;

/**
 * Utilities to interact with Lab {@link ResetApi}.
 */
public class LabResetHelper {

    public static final ConfidentialClientHelper INSTANCE = LabAuthenticationHelper.getInstance();

    /**
     * Reset the password for the supplied account.
     *
     * @param upn the upn of the user for which to reset password
     * @return a boolean indicating if password reset was successful
     */
    public static boolean resetPassword(@NonNull final String upn) {
        INSTANCE.setupApiClientWithAccessToken();

        final ResetApi resetApi = new ResetApi();

        try {
            final CustomSuccessResponse resetResponse = resetApi.apiResetPut(upn, LabConstants.ResetOperation.PASSWORD);

            if (resetResponse == null) {
                return false;
            }

            final String expectedResult = ("Password reset for user: " + upn).toLowerCase();
            return resetResponse.toString().toLowerCase().contains(expectedResult);
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Resets the MFA for the supplied user account.
     *
     * @param upn the upn of the user for which to reset MFA
     * @return a boolean indicating if MFA reset was successful
     */
    public static boolean resetMfa(@NonNull final String upn) {
        INSTANCE.setupApiClientWithAccessToken();

        final ResetApi resetApi = new ResetApi();

        try {
            final CustomSuccessResponse resetResponse = resetApi.apiResetPut(upn, LabConstants.ResetOperation.MFA);

            if (resetResponse == null) {
                return false;
            }

            return resetResponse.toString().contains(
                    "MFA reset for user: " + upn
            );
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
