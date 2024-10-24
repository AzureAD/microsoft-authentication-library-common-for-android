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

import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnablePolicyApi {
    private ApiClient apiClient;

    public EnablePolicyApi() {
        this(Configuration.getDefaultApiClient());
    }

    public EnablePolicyApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for apiEnablePolicyPut
     * @param upn Enter a valid Locked User UPN (optional)
     * @param policy Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call apiEnablePolicyPutCall(String upn, String policy, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/api/EnablePolicy";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (upn != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("upn", upn));
        if (policy != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("policy", policy));

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
        return apiClient.buildCall(localVarPath, "PUT", localVarQueryParams, localVarCollectionQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarAuthNames, progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call apiEnablePolicyPutValidateBeforeCall(String upn, String policy, final ProgressResponseBody.ProgressListener progressListener, final ProgressRequestBody.ProgressRequestListener progressRequestListener) throws ApiException {

        com.squareup.okhttp.Call call = apiEnablePolicyPutCall(upn, policy, progressListener, progressRequestListener);
        return call;
    }

    /**
     * lets you enable CA/Special Policies for any Locked User
     * Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.   Also test users can have more than 1 policy assigned to the same user.
     * @param upn Enter a valid Locked User UPN (optional)
     * @param policy Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. (optional)
     * @return CustomSuccessResponse
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public CustomSuccessResponse apiEnablePolicyPut(String upn, String policy) throws ApiException {
        ApiResponse<CustomSuccessResponse> resp = apiEnablePolicyPutWithHttpInfo(upn, policy);
        return resp.getData();
    }

    /**
     * lets you enable CA/Special Policies for any Locked User
     * Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.   Also test users can have more than 1 policy assigned to the same user.
     * @param upn Enter a valid Locked User UPN (optional)
     * @param policy Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. (optional)
     * @return ApiResponse&lt;CustomSuccessResponse&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<CustomSuccessResponse> apiEnablePolicyPutWithHttpInfo(String upn, String policy) throws ApiException {
        com.squareup.okhttp.Call call = apiEnablePolicyPutValidateBeforeCall(upn, policy, null, null);
        Type localVarReturnType = TypeToken.get(CustomSuccessResponse.class).getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * lets you enable CA/Special Policies for any Locked User (asynchronously)
     * Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO.   Also test users can have more than 1 policy assigned to the same user.
     * @param upn Enter a valid Locked User UPN (optional)
     * @param policy Enable Policy can be used for GlobalMFA, MAMCA, MDMCA, MFAONSPO, MFAONEXO. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call apiEnablePolicyPutAsync(String upn, String policy, final ApiCallback<CustomSuccessResponse> callback) throws ApiException {

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

        com.squareup.okhttp.Call call = apiEnablePolicyPutValidateBeforeCall(upn, policy, progressListener, progressRequestListener);
        Type localVarReturnType = TypeToken.get(CustomSuccessResponse.class).getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}