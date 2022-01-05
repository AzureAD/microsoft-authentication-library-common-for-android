/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.internal.testutils.labutils;

import androidx.annotation.NonNull;

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.LabApi;
import com.microsoft.identity.internal.test.labapi.api.LabSecretApi;
import com.microsoft.identity.internal.test.labapi.model.LabInfo;
import com.microsoft.identity.internal.test.labapi.model.SecretResponse;

/**
 * Query the Lab Api to get lab specific info such as lab tenant, secret etc.
 */
public class LabHelper {

    private static final ConfidentialClientHelper instance = LabAuthenticationHelper.getInstance();

    /**
     * Get the tenant id of a lab from the LAB API.
     *
     * @param labName the lab for which to get the tenant id
     * @return a String representing the tenant id associated to this LAB
     */
    public static String getLabTenantId(final String labName) {
        instance.setupApiClientWithAccessToken();
        LabApi labApi = new LabApi();
        LabInfo labInfo;
        try {
            // return the first object
            labInfo = labApi.apiLabLabnameGet(labName).get(0);
        } catch (ApiException ex) {
            throw new RuntimeException("Error retrieving lab info", ex);
        }

        return labInfo.getTenantId();
    }

    /**
     * Get the password of the supplied lab.
     *
     * @param credentialVaultKeyName the vault key name for the lab
     * @return a String represent the password of the lab
     */
    public static String getPasswordForLab(final String credentialVaultKeyName) {
        final String secretName = getLabSecretName(credentialVaultKeyName);
        return getSecret(secretName);
    }

    /**
     * Get the value of the supplied secret from the LAB.
     *
     * @param secretName the secret to pull
     * @return a String representing secret value
     */
    public static String getSecret(@NonNull final String secretName) {
        instance.setupApiClientWithAccessToken();
        LabSecretApi labSecretApi = new LabSecretApi();
        SecretResponse secretResponse;

        try {
            secretResponse = labSecretApi.apiLabSecretGet(secretName);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving secret from lab.", ex);
        }

        return secretResponse.getValue();
    }

    private static String getLabSecretName(final String credentialVaultKeyName) {
        final String[] parts = credentialVaultKeyName.split("/");
        return parts[parts.length - 1];
    }
}
