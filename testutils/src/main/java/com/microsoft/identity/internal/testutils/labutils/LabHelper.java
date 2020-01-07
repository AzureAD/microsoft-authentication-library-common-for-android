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

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.LabApi;
import com.microsoft.identity.internal.test.labapi.api.LabUserSecretApi;
import com.microsoft.identity.internal.test.labapi.model.LabInfo;
import com.microsoft.identity.internal.test.labapi.model.SecretResponse;

public class LabHelper {

    // this can be used to get tenant id for a guest tenant
    public static String getLabTenantId(final String labName) {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        LabApi labApi = new LabApi();
        LabInfo labInfo;
        try {
            // return the first object
            labInfo = labApi.getLabName(labName).get(0);
        } catch (ApiException ex) {
            throw new RuntimeException("Error retrieving lab info", ex);
        }

        return labInfo.getTenantId();
    }

    public static String getPasswordForLab(final String labName) {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
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
