/*
 * Azure Identity Labs API
 * No description provided (generated by Swagger Codegen https://github.com/swagger-api/swagger-codegen)
 *
 * OpenAPI spec version: 1.0.0.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.microsoft.identity.internal.test.labapi.api;

import com.microsoft.identity.internal.test.labapi.ApiCallback;
import com.microsoft.identity.internal.test.labapi.ApiClient;
import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.ApiResponse;
import com.microsoft.identity.internal.test.labapi.Configuration;
import com.microsoft.identity.internal.test.labapi.Pair;
import com.microsoft.identity.internal.test.labapi.ProgressRequestBody;
import com.microsoft.identity.internal.test.labapi.ProgressResponseBody;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;


import com.microsoft.identity.internal.test.labapi.model.CustomErrorResponse;
import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabPolicyInfoApi {
    private ApiClient apiClient;

    public LabPolicyInfoApi() {
        this(Configuration.getDefaultApiClient());
    }

    public LabPolicyInfoApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for apiLabPolicyInfoGet
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call apiLabPolicyInfoGetCall(final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/api/LabPolicyInfo";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {
            "application/json"
        };
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {
            
        };
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if(progressListener != null) {
            apiClient.getHttpClient().networkInterceptors().add(new com.squareup.okhttp.Interceptor() {
                @Override
                public com.squareup.okhttp.Response intercept(com.squareup.okhttp.Interceptor.Chain chain) throws IOException {
                    com.squareup.okhttp.Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
                }
            });
        }

        String[] localVarAuthNames = new String[] {  };
        return apiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call apiLabPolicyInfoGetValidateBeforeCall(final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        com.squareup.okhttp.Call call = apiLabPolicyInfoGetCall(progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Will give you the different Policies available to be used with Create Temp User / Enable/Disable Policy API
     * Policy will be listed as GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. Use the LabUserInfo endpoint to query the user policy info.
     * @return CustomSuccessResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public CustomSuccessResponse apiLabPolicyInfoGet() throws ApiException {
        ApiResponse<CustomSuccessResponse> resp = apiLabPolicyInfoGetWithHttpInfo();
        return resp.getData();
    }

    /**
     * Will give you the different Policies available to be used with Create Temp User / Enable/Disable Policy API
     * Policy will be listed as GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. Use the LabUserInfo endpoint to query the user policy info.
     * @return ApiResponse&lt;CustomSuccessResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<CustomSuccessResponse> apiLabPolicyInfoGetWithHttpInfo() throws ApiException {
        com.squareup.okhttp.Call call = apiLabPolicyInfoGetValidateBeforeCall(null, null);
        Type localVarReturnType = TypeToken.get(CustomSuccessResponse.class).getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Will give you the different Policies available to be used with Create Temp User / Enable/Disable Policy API (asynchronously)
     * Policy will be listed as GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. Use the LabUserInfo endpoint to query the user policy info.
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call apiLabPolicyInfoGetAsync(final ApiCallback<CustomSuccessResponse> callback) throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener = new ProgressResponseBody.ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    callback.onDownloadProgress(bytesRead, contentLength, done);
                }
            };

            progressRequestListener = new ProgressRequestBody.ProgressRequestListener() {
                @Override
                public void onRequestProgress(long bytesWritten, long contentLength, boolean done) {
                    callback.onUploadProgress(bytesWritten, contentLength, done);
                }
            };
        }

        com.squareup.okhttp.Call call = apiLabPolicyInfoGetValidateBeforeCall(progressListener, progressRequestListener);
        Type localVarReturnType = TypeToken.get(CustomSuccessResponse.class).getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
