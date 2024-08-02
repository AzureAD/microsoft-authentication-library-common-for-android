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

import com.microsoft.identity.internal.test.keyvault.ApiClient;
import com.microsoft.identity.internal.test.keyvault.Configuration;
import com.microsoft.identity.internal.test.keyvault.api.SecretsApi;
import com.microsoft.identity.internal.test.keyvault.model.SecretBundle;

import androidx.annotation.NonNull;

/**
 * Query Labs KeyVaults to retrieve secrets.
 */
public class KeyVaultFetchHelper {

    private static final KeyVaultAuthHelper instance =  (KeyVaultAuthHelper) KeyVaultAuthHelper.getInstance();

    public static String getSecretForBuildAutomation(@NonNull final String secretName) {
        instance.setupApiClientWithAccessToken();
        ApiClient apiClient = Configuration.getBuildAutomationVaultApiClient();

        SecretsApi secretsApi = new SecretsApi(apiClient);
        SecretBundle secretBundleResponse;

        try {
            secretBundleResponse = secretsApi.getSecretWithHttpInfo(secretName, "", "7.4").getData();
        } catch (com.microsoft.identity.internal.test.keyvault.ApiException ex) {
            throw new RuntimeException("Error retrieving secret from lab.", ex);
        }

        return secretBundleResponse.getValue();
    }
}
