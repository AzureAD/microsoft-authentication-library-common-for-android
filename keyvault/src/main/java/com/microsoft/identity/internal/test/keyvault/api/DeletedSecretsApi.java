/*
 * KeyVaultClient
 * The key vault client performs cryptographic key operations and vault operations against the Key Vault service.
 *
 * OpenAPI spec version: 2016-10-01
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.microsoft.identity.internal.test.keyvault.api;

import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.internal.test.keyvault.ApiCallback;
import com.microsoft.identity.internal.test.keyvault.ApiClient;
import com.microsoft.identity.internal.test.keyvault.ApiException;
import com.microsoft.identity.internal.test.keyvault.ApiResponse;
import com.microsoft.identity.internal.test.keyvault.Configuration;
import com.microsoft.identity.internal.test.keyvault.Pair;
import com.microsoft.identity.internal.test.keyvault.ProgressRequestBody;
import com.microsoft.identity.internal.test.keyvault.ProgressResponseBody;
import com.microsoft.identity.internal.test.keyvault.model.DeletedSecretBundle;
import com.microsoft.identity.internal.test.keyvault.model.DeletedSecretListResult;
import com.microsoft.identity.internal.test.keyvault.model.SecretBundle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeletedSecretsApi {
    private ApiClient apiClient;

    public DeletedSecretsApi() {
        this(Configuration.getDefaultApiClient());
    }

    public DeletedSecretsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Build call for getDeletedSecret
     * @param secretName The name of the secret. (required)
     * @param apiVersion Client API version. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDeletedSecretCall(
            String secretName,
            String apiVersion,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener)
            throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath =
                "/deletedsecrets/{secret-name}"
                        .replaceAll(
                                "\\{" + "secret-name" + "\\}",
                                apiClient.escapeString(secretName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (apiVersion != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("api-version", apiVersion));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {"application/json"};
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {"application/json"};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if (progressListener != null) {
            apiClient
                    .getHttpClient()
                    .networkInterceptors()
                    .add(
                            new com.squareup.okhttp.Interceptor() {
                                @Override
                                public com.squareup.okhttp.Response intercept(
                                        com.squareup.okhttp.Interceptor.Chain chain)
                                        throws IOException {
                                    com.squareup.okhttp.Response originalResponse =
                                            chain.proceed(chain.request());
                                    return originalResponse
                                            .newBuilder()
                                            .body(
                                                    new ProgressResponseBody(
                                                            originalResponse.body(),
                                                            progressListener))
                                            .build();
                                }
                            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(
                localVarPath,
                "GET",
                localVarQueryParams,
                localVarCollectionQueryParams,
                localVarPostBody,
                localVarHeaderParams,
                localVarFormParams,
                localVarAuthNames,
                progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getDeletedSecretValidateBeforeCall(
            String secretName,
            String apiVersion,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener)
            throws ApiException {

        // verify the required parameter 'secretName' is set
        if (secretName == null) {
            throw new ApiException(
                    "Missing the required parameter 'secretName' when calling getDeletedSecret(Async)");
        }

        // verify the required parameter 'apiVersion' is set
        if (apiVersion == null) {
            throw new ApiException(
                    "Missing the required parameter 'apiVersion' when calling getDeletedSecret(Async)");
        }

        com.squareup.okhttp.Call call =
                getDeletedSecretCall(
                        secretName, apiVersion, progressListener, progressRequestListener);
        return call;
    }

    /**
     * Gets the specified deleted secret.
     * The Get Deleted Secret operation returns the specified deleted secret along with its attributes. This operation requires the secrets/get permission.
     * @param secretName The name of the secret. (required)
     * @param apiVersion Client API version. (required)
     * @return DeletedSecretBundle
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DeletedSecretBundle getDeletedSecret(String secretName, String apiVersion)
            throws ApiException {
        ApiResponse<DeletedSecretBundle> resp =
                getDeletedSecretWithHttpInfo(secretName, apiVersion);
        return resp.getData();
    }

    /**
     * Gets the specified deleted secret.
     * The Get Deleted Secret operation returns the specified deleted secret along with its attributes. This operation requires the secrets/get permission.
     * @param secretName The name of the secret. (required)
     * @param apiVersion Client API version. (required)
     * @return ApiResponse&lt;DeletedSecretBundle&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DeletedSecretBundle> getDeletedSecretWithHttpInfo(
            String secretName, String apiVersion) throws ApiException {
        com.squareup.okhttp.Call call =
                getDeletedSecretValidateBeforeCall(secretName, apiVersion, null, null);
        Type localVarReturnType = TypeToken.get(DeletedSecretBundle.class).getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Gets the specified deleted secret. (asynchronously)
     * The Get Deleted Secret operation returns the specified deleted secret along with its attributes. This operation requires the secrets/get permission.
     * @param secretName The name of the secret. (required)
     * @param apiVersion Client API version. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDeletedSecretAsync(
            String secretName, String apiVersion, final ApiCallback<DeletedSecretBundle> callback)
            throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener =
                    new ProgressResponseBody.ProgressListener() {
                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                            callback.onDownloadProgress(bytesRead, contentLength, done);
                        }
                    };

            progressRequestListener =
                    new ProgressRequestBody.ProgressRequestListener() {
                        @Override
                        public void onRequestProgress(
                                long bytesWritten, long contentLength, boolean done) {
                            callback.onUploadProgress(bytesWritten, contentLength, done);
                        }
                    };
        }

        com.squareup.okhttp.Call call =
                getDeletedSecretValidateBeforeCall(
                        secretName, apiVersion, progressListener, progressRequestListener);
        Type localVarReturnType = TypeToken.get(DeletedSecretBundle.class).getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for getDeletedSecrets
     * @param apiVersion Client API version. (required)
     * @param maxresults Maximum number of results to return in a page. If not specified the service will return up to 25 results. (optional)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call getDeletedSecretsCall(
            String apiVersion,
            Integer maxresults,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener)
            throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath = "/deletedsecrets";

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (maxresults != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("maxresults", maxresults));
        if (apiVersion != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("api-version", apiVersion));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {"application/json"};
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {"application/json"};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if (progressListener != null) {
            apiClient
                    .getHttpClient()
                    .networkInterceptors()
                    .add(
                            new com.squareup.okhttp.Interceptor() {
                                @Override
                                public com.squareup.okhttp.Response intercept(
                                        com.squareup.okhttp.Interceptor.Chain chain)
                                        throws IOException {
                                    com.squareup.okhttp.Response originalResponse =
                                            chain.proceed(chain.request());
                                    return originalResponse
                                            .newBuilder()
                                            .body(
                                                    new ProgressResponseBody(
                                                            originalResponse.body(),
                                                            progressListener))
                                            .build();
                                }
                            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(
                localVarPath,
                "GET",
                localVarQueryParams,
                localVarCollectionQueryParams,
                localVarPostBody,
                localVarHeaderParams,
                localVarFormParams,
                localVarAuthNames,
                progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call getDeletedSecretsValidateBeforeCall(
            String apiVersion,
            Integer maxresults,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener)
            throws ApiException {

        // verify the required parameter 'apiVersion' is set
        if (apiVersion == null) {
            throw new ApiException(
                    "Missing the required parameter 'apiVersion' when calling getDeletedSecrets(Async)");
        }

        com.squareup.okhttp.Call call =
                getDeletedSecretsCall(
                        apiVersion, maxresults, progressListener, progressRequestListener);
        return call;
    }

    /**
     * Lists deleted secrets for the specified vault.
     * The Get Deleted Secrets operation returns the secrets that have been deleted for a vault enabled for soft-delete. This operation requires the secrets/list permission.
     * @param apiVersion Client API version. (required)
     * @param maxresults Maximum number of results to return in a page. If not specified the service will return up to 25 results. (optional)
     * @return DeletedSecretListResult
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public DeletedSecretListResult getDeletedSecrets(String apiVersion, Integer maxresults)
            throws ApiException {
        ApiResponse<DeletedSecretListResult> resp =
                getDeletedSecretsWithHttpInfo(apiVersion, maxresults);
        return resp.getData();
    }

    /**
     * Lists deleted secrets for the specified vault.
     * The Get Deleted Secrets operation returns the secrets that have been deleted for a vault enabled for soft-delete. This operation requires the secrets/list permission.
     * @param apiVersion Client API version. (required)
     * @param maxresults Maximum number of results to return in a page. If not specified the service will return up to 25 results. (optional)
     * @return ApiResponse&lt;DeletedSecretListResult&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<DeletedSecretListResult> getDeletedSecretsWithHttpInfo(
            String apiVersion, Integer maxresults) throws ApiException {
        com.squareup.okhttp.Call call =
                getDeletedSecretsValidateBeforeCall(apiVersion, maxresults, null, null);
        Type localVarReturnType = TypeToken.get(DeletedSecretListResult.class).getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Lists deleted secrets for the specified vault. (asynchronously)
     * The Get Deleted Secrets operation returns the secrets that have been deleted for a vault enabled for soft-delete. This operation requires the secrets/list permission.
     * @param apiVersion Client API version. (required)
     * @param maxresults Maximum number of results to return in a page. If not specified the service will return up to 25 results. (optional)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call getDeletedSecretsAsync(
            String apiVersion,
            Integer maxresults,
            final ApiCallback<DeletedSecretListResult> callback)
            throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener =
                    new ProgressResponseBody.ProgressListener() {
                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                            callback.onDownloadProgress(bytesRead, contentLength, done);
                        }
                    };

            progressRequestListener =
                    new ProgressRequestBody.ProgressRequestListener() {
                        @Override
                        public void onRequestProgress(
                                long bytesWritten, long contentLength, boolean done) {
                            callback.onUploadProgress(bytesWritten, contentLength, done);
                        }
                    };
        }

        com.squareup.okhttp.Call call =
                getDeletedSecretsValidateBeforeCall(
                        apiVersion, maxresults, progressListener, progressRequestListener);
        Type localVarReturnType = TypeToken.get(DeletedSecretListResult.class).getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
    /**
     * Build call for purgeDeletedSecret
     * @param secretName The name of the secret. (required)
     * @param apiVersion Client API version. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call purgeDeletedSecretCall(
            String secretName,
            String apiVersion,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener)
            throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath =
                "/deletedsecrets/{secret-name}"
                        .replaceAll(
                                "\\{" + "secret-name" + "\\}",
                                apiClient.escapeString(secretName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (apiVersion != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("api-version", apiVersion));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {"application/json"};
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {"application/json"};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if (progressListener != null) {
            apiClient
                    .getHttpClient()
                    .networkInterceptors()
                    .add(
                            new com.squareup.okhttp.Interceptor() {
                                @Override
                                public com.squareup.okhttp.Response intercept(
                                        com.squareup.okhttp.Interceptor.Chain chain)
                                        throws IOException {
                                    com.squareup.okhttp.Response originalResponse =
                                            chain.proceed(chain.request());
                                    return originalResponse
                                            .newBuilder()
                                            .body(
                                                    new ProgressResponseBody(
                                                            originalResponse.body(),
                                                            progressListener))
                                            .build();
                                }
                            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(
                localVarPath,
                "DELETE",
                localVarQueryParams,
                localVarCollectionQueryParams,
                localVarPostBody,
                localVarHeaderParams,
                localVarFormParams,
                localVarAuthNames,
                progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call purgeDeletedSecretValidateBeforeCall(
            String secretName,
            String apiVersion,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener)
            throws ApiException {

        // verify the required parameter 'secretName' is set
        if (secretName == null) {
            throw new ApiException(
                    "Missing the required parameter 'secretName' when calling purgeDeletedSecret(Async)");
        }

        // verify the required parameter 'apiVersion' is set
        if (apiVersion == null) {
            throw new ApiException(
                    "Missing the required parameter 'apiVersion' when calling purgeDeletedSecret(Async)");
        }

        com.squareup.okhttp.Call call =
                purgeDeletedSecretCall(
                        secretName, apiVersion, progressListener, progressRequestListener);
        return call;
    }

    /**
     * Permanently deletes the specified secret.
     * The purge deleted secret operation removes the secret permanently, without the possibility of recovery. This operation can only be enabled on a soft-delete enabled vault. This operation requires the secrets/purge permission.
     * @param secretName The name of the secret. (required)
     * @param apiVersion Client API version. (required)
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public void purgeDeletedSecret(String secretName, String apiVersion) throws ApiException {
        purgeDeletedSecretWithHttpInfo(secretName, apiVersion);
    }

    /**
     * Permanently deletes the specified secret.
     * The purge deleted secret operation removes the secret permanently, without the possibility of recovery. This operation can only be enabled on a soft-delete enabled vault. This operation requires the secrets/purge permission.
     * @param secretName The name of the secret. (required)
     * @param apiVersion Client API version. (required)
     * @return ApiResponse&lt;Void&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<Void> purgeDeletedSecretWithHttpInfo(String secretName, String apiVersion)
            throws ApiException {
        com.squareup.okhttp.Call call =
                purgeDeletedSecretValidateBeforeCall(secretName, apiVersion, null, null);
        return apiClient.execute(call);
    }

    /**
     * Permanently deletes the specified secret. (asynchronously)
     * The purge deleted secret operation removes the secret permanently, without the possibility of recovery. This operation can only be enabled on a soft-delete enabled vault. This operation requires the secrets/purge permission.
     * @param secretName The name of the secret. (required)
     * @param apiVersion Client API version. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call purgeDeletedSecretAsync(
            String secretName, String apiVersion, final ApiCallback<Void> callback)
            throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener =
                    new ProgressResponseBody.ProgressListener() {
                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                            callback.onDownloadProgress(bytesRead, contentLength, done);
                        }
                    };

            progressRequestListener =
                    new ProgressRequestBody.ProgressRequestListener() {
                        @Override
                        public void onRequestProgress(
                                long bytesWritten, long contentLength, boolean done) {
                            callback.onUploadProgress(bytesWritten, contentLength, done);
                        }
                    };
        }

        com.squareup.okhttp.Call call =
                purgeDeletedSecretValidateBeforeCall(
                        secretName, apiVersion, progressListener, progressRequestListener);
        apiClient.executeAsync(call, callback);
        return call;
    }
    /**
     * Build call for recoverDeletedSecret
     * @param secretName The name of the deleted secret. (required)
     * @param apiVersion Client API version. (required)
     * @param progressListener Progress listener
     * @param progressRequestListener Progress request listener
     * @return Call to execute
     * @throws ApiException If fail to serialize the request body object
     */
    public com.squareup.okhttp.Call recoverDeletedSecretCall(
            String secretName,
            String apiVersion,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener)
            throws ApiException {
        Object localVarPostBody = null;

        // create path and map variables
        String localVarPath =
                "/deletedsecrets/{secret-name}/recover"
                        .replaceAll(
                                "\\{" + "secret-name" + "\\}",
                                apiClient.escapeString(secretName.toString()));

        List<Pair> localVarQueryParams = new ArrayList<Pair>();
        List<Pair> localVarCollectionQueryParams = new ArrayList<Pair>();
        if (apiVersion != null)
            localVarQueryParams.addAll(apiClient.parameterToPair("api-version", apiVersion));

        Map<String, String> localVarHeaderParams = new HashMap<String, String>();

        Map<String, Object> localVarFormParams = new HashMap<String, Object>();

        final String[] localVarAccepts = {"application/json"};
        final String localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        if (localVarAccept != null) localVarHeaderParams.put("Accept", localVarAccept);

        final String[] localVarContentTypes = {"application/json"};
        final String localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);
        localVarHeaderParams.put("Content-Type", localVarContentType);

        if (progressListener != null) {
            apiClient
                    .getHttpClient()
                    .networkInterceptors()
                    .add(
                            new com.squareup.okhttp.Interceptor() {
                                @Override
                                public com.squareup.okhttp.Response intercept(
                                        com.squareup.okhttp.Interceptor.Chain chain)
                                        throws IOException {
                                    com.squareup.okhttp.Response originalResponse =
                                            chain.proceed(chain.request());
                                    return originalResponse
                                            .newBuilder()
                                            .body(
                                                    new ProgressResponseBody(
                                                            originalResponse.body(),
                                                            progressListener))
                                            .build();
                                }
                            });
        }

        String[] localVarAuthNames = new String[] {};
        return apiClient.buildCall(
                localVarPath,
                "POST",
                localVarQueryParams,
                localVarCollectionQueryParams,
                localVarPostBody,
                localVarHeaderParams,
                localVarFormParams,
                localVarAuthNames,
                progressRequestListener);
    }

    @SuppressWarnings("rawtypes")
    private com.squareup.okhttp.Call recoverDeletedSecretValidateBeforeCall(
            String secretName,
            String apiVersion,
            final ProgressResponseBody.ProgressListener progressListener,
            final ProgressRequestBody.ProgressRequestListener progressRequestListener)
            throws ApiException {

        // verify the required parameter 'secretName' is set
        if (secretName == null) {
            throw new ApiException(
                    "Missing the required parameter 'secretName' when calling recoverDeletedSecret(Async)");
        }

        // verify the required parameter 'apiVersion' is set
        if (apiVersion == null) {
            throw new ApiException(
                    "Missing the required parameter 'apiVersion' when calling recoverDeletedSecret(Async)");
        }

        com.squareup.okhttp.Call call =
                recoverDeletedSecretCall(
                        secretName, apiVersion, progressListener, progressRequestListener);
        return call;
    }

    /**
     * Recovers the deleted secret to the latest version.
     * Recovers the deleted secret in the specified vault. This operation can only be performed on a soft-delete enabled vault. This operation requires the secrets/recover permission.
     * @param secretName The name of the deleted secret. (required)
     * @param apiVersion Client API version. (required)
     * @return SecretBundle
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public SecretBundle recoverDeletedSecret(String secretName, String apiVersion)
            throws ApiException {
        ApiResponse<SecretBundle> resp = recoverDeletedSecretWithHttpInfo(secretName, apiVersion);
        return resp.getData();
    }

    /**
     * Recovers the deleted secret to the latest version.
     * Recovers the deleted secret in the specified vault. This operation can only be performed on a soft-delete enabled vault. This operation requires the secrets/recover permission.
     * @param secretName The name of the deleted secret. (required)
     * @param apiVersion Client API version. (required)
     * @return ApiResponse&lt;SecretBundle&gt;
     * @throws ApiException If fail to call the API, e.g. server error or cannot deserialize the response body
     */
    public ApiResponse<SecretBundle> recoverDeletedSecretWithHttpInfo(
            String secretName, String apiVersion) throws ApiException {
        com.squareup.okhttp.Call call =
                recoverDeletedSecretValidateBeforeCall(secretName, apiVersion, null, null);
        Type localVarReturnType = TypeToken.get(SecretBundle.class).getType();
        return apiClient.execute(call, localVarReturnType);
    }

    /**
     * Recovers the deleted secret to the latest version. (asynchronously)
     * Recovers the deleted secret in the specified vault. This operation can only be performed on a soft-delete enabled vault. This operation requires the secrets/recover permission.
     * @param secretName The name of the deleted secret. (required)
     * @param apiVersion Client API version. (required)
     * @param callback The callback to be executed when the API call finishes
     * @return The request call
     * @throws ApiException If fail to process the API call, e.g. serializing the request body object
     */
    public com.squareup.okhttp.Call recoverDeletedSecretAsync(
            String secretName, String apiVersion, final ApiCallback<SecretBundle> callback)
            throws ApiException {

        ProgressResponseBody.ProgressListener progressListener = null;
        ProgressRequestBody.ProgressRequestListener progressRequestListener = null;

        if (callback != null) {
            progressListener =
                    new ProgressResponseBody.ProgressListener() {
                        @Override
                        public void update(long bytesRead, long contentLength, boolean done) {
                            callback.onDownloadProgress(bytesRead, contentLength, done);
                        }
                    };

            progressRequestListener =
                    new ProgressRequestBody.ProgressRequestListener() {
                        @Override
                        public void onRequestProgress(
                                long bytesWritten, long contentLength, boolean done) {
                            callback.onUploadProgress(bytesWritten, contentLength, done);
                        }
                    };
        }

        com.squareup.okhttp.Call call =
                recoverDeletedSecretValidateBeforeCall(
                        secretName, apiVersion, progressListener, progressRequestListener);
        Type localVarReturnType = TypeToken.get(SecretBundle.class).getType();
        apiClient.executeAsync(call, localVarReturnType, callback);
        return call;
    }
}
