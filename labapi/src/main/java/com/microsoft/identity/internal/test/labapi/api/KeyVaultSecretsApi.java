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
package com.microsoft.identity.internal.test.labapi.api;

import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.internal.test.labapi.ApiClient;
import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.ApiResponse;
import com.microsoft.identity.internal.test.labapi.Configuration;
import com.microsoft.identity.internal.test.labapi.Pair;
import com.microsoft.identity.internal.test.labapi.ProgressRequestBody;
import com.microsoft.identity.internal.test.labapi.ProgressResponseBody;
import com.microsoft.identity.internal.test.labapi.model.SecretBundle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to facilitate getting secrets from Azure Key Vault.
 * The default key vault used is MSIDLABS, but a custom vault URL can be provided.
 */
public class KeyVaultSecretsApi {
    private final ApiClient apiClient;
    private final String DEFAULT_VAULT_URL = "https://msidlabs.vault.azure.net";
    private final String mVaultUrl;
    private final String DEFAULT_API_VERSION = "2025-07-01";

    public KeyVaultSecretsApi() {
        apiClient = Configuration.getKeyVaultApiClient();
        mVaultUrl = DEFAULT_VAULT_URL;
    }

    public KeyVaultSecretsApi(final String basePath) {
        apiClient = Configuration.getKeyVaultApiClient();
        mVaultUrl = basePath;
    }

    /**
     * Build call for getSecret
     *
     * @param pathToSecretInKeyVault   The name of the secret. (required)
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    private com.squareup.okhttp.Call getSecretCall(final String pathToSecretInKeyVault) throws ApiException {
        Object localVarPostBody = null;

        final String url = mVaultUrl + "/secrets/" + pathToSecretInKeyVault;

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        localVarQueryParams.addAll(apiClient.parameterToPair("api-version", DEFAULT_API_VERSION));

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
                "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
                "application/json"
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        String[] localVarAuthNames = new String[]{ };
        return apiClient.buildCall(url, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, null);
    }

    /**
     * Get a specified secret from a given key vault.
     * The GET operation is applicable to any secret stored in Azure Key Vault. This operation requires the secrets/get permission.
     *
     * @param secretName    The name of the secret. (required)
     * @param secretVersion The version of the secret. (required)
     * @return SecretBundle
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SecretBundle getKeyVaultSecret(String secretName, String secretVersion) throws ApiException {
        // create path and map variables
        String pathToSecretInKeyVault = secretName + "/" + secretVersion + "/";
        ApiResponse<SecretBundle> resp = getSecretWithHttpInfo(pathToSecretInKeyVault);
        return resp.getData();
    }

    /**
     * Get a specified secret from a given key vault.
     * The GET operation is applicable to any secret stored in Azure Key Vault. This operation requires the secrets/get permission.
     *
     * @param pathToSecretInKeyVault path to the secret in the key vault (required)
     * @return ApiResponse&lt;SecretBundle&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SecretBundle> getSecretWithHttpInfo(String pathToSecretInKeyVault) throws ApiException {
        com.squareup.okhttp.Call call = getSecretCall(pathToSecretInKeyVault);
        Type localVarReturnType = TypeToken.get(SecretBundle.class).getType();
        return apiClient.execute(call, localVarReturnType);
    }

}
