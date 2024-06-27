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

public class DeleteDeviceApi {
    private ApiClient apiClient;

    public DeleteDeviceApi() {
        this(Configuration.getDefaultApiClient());
    }

    public DeleteDeviceApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for apiDeleteDeviceDelete
     * @param upn Please enter a valid lab upn (optional)
     * @param deviceid Please enter the 32 digit Device ID in GUID Format (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call apiDeleteDeviceDeleteCall(String upn, String deviceid, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;
        
        // create path and map variables
        String localVarPath = "/api/DeleteDevice";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (upn != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("upn", upn));

        if (deviceid != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("deviceid", deviceid));

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
        return apiClient.buildCall(localVarPath, "POST", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }
    
    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call apiDeleteDeviceDeleteValidateBeforeCall(String upn, String deviceid, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        
        com.squareup.okhttp.Call call = apiDeleteDeviceDeleteCall(upn, deviceid, progressListener, progressRequestListener);
        return call;

        
        
        
        
    }

    /**
     * Delete&#x27;s a Device from AAD.   You need to provide the UPN and the Device ID (in GUID format) in Query String.
     * Provides generic error messages
     * @param upn Please enter a valid lab upn (optional)
     * @param deviceid Please enter the 32 digit Device ID in GUID Format (optional)
     * @return CustomSuccessResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public CustomSuccessResponse apiDeleteDeviceDelete(String upn, String deviceid) throws ApiException {
        ApiResponse<CustomSuccessResponse> resp = apiDeleteDeviceDeleteWithHttpInfo(upn, deviceid);
        return resp.getData();
    }

    /**
     * Delete&#x27;s a Device from AAD.   You need to provide the UPN and the Device ID (in GUID format) in Query String.
     * Provides generic error messages
     * @param upn Please enter a valid lab upn (optional)
     * @param deviceid Please enter the 32 digit Device ID in GUID Format (optional)
     * @return ApiResponse&lt;CustomSuccessResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<CustomSuccessResponse> apiDeleteDeviceDeleteWithHttpInfo(String upn, String deviceid) throws ApiException {
        com.squareup.okhttp.Call call = apiDeleteDeviceDeleteValidateBeforeCall(upn, deviceid, null, null);
        Type localVarReturnType = TypeToken.get(CustomSuccessResponse.class).getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Delete&#x27;s a Device from AAD.   You need to provide the UPN and the Device ID (in GUID format) in Query String. (asynchronously)
     * Provides generic error messages
     * @param upn Please enter a valid lab upn (optional)
     * @param deviceid Please enter the 32 digit Device ID in GUID Format (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call apiDeleteDeviceDeleteAsync(String upn, String deviceid, final ApiCallback<CustomSuccessResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = apiDeleteDeviceDeleteValidateBeforeCall(upn, deviceid, progressListener, progressRequestListener);
        Type localVarReturnType = TypeToken.get(CustomSuccessResponse.class).getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
