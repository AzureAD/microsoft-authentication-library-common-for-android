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
package com.microsoft.identity.common.exception;

public final class ErrorStrings {

    private ErrorStrings() {
        // Utility class.
    }

    /**
     * Access token doesn't exist and there is no refresh token can be found to redeem access token.
     */
    public static final String NO_TOKENS_FOUND = "no_tokens_found";

    /**
     * The supplied Account cannot be found in the cache.
     */
    public static final String NO_ACCOUNT_FOUND = "no_account_found";

    /**
     * There are multiple cache entries found, the sdk cannot pick the correct access token
     * or refresh token from the cache. Likely it's a bug in the sdk when caching tokens or authority
     * is not proviced in the silent request and multiple tokens were found.
     */
    public static final String MULTIPLE_MATCHING_TOKENS_DETECTED = "multiple_matching_tokens_detected";

    /**
     * No active network is available on the device.
     */
    public static final String DEVICE_NETWORK_NOT_AVAILABLE = "device_network_not_available";

    /**
     * Network is available but device is in the doze mode.
     */
    public static final String NO_NETWORK_CONNECTION_POWER_OPTIMIZATION = "device_network_not_available_doze_mode";

    /**
     * The sdk failed to parse the Json format.
     */
    public static final String JSON_PARSE_FAILURE = "json_parse_failure";

    /**
     * IOException happened, could be the device/network errors.
     */
    public static final String IO_ERROR = "io_error";

    /**
     * SocketTimeoutException happened, connection flow timed out, stalled or broken.
     */
    public static final String SOCKET_TIMEOUT = "socket_timeout";

    /**
     * The url is malformed.  Likely caused when constructing the auth request, authority, or redirect URI.
     */
    public static final String MALFORMED_URL = "malformed_url";

    /**
     * The encoding is not supported by the device.
     */
    public static final String UNSUPPORTED_ENCODING = "unsupported_encoding";

    /**
     * The algorithm used to generate pkce challenge is not supported.
     */
    public static final String NO_SUCH_ALGORITHM = "no_such_algorithm";

    /**
     * JWT returned by the server is not valid, empty or malformed.
     */
    public static final String INVALID_JWT = "invalid_jwt";

    /**
     * State from authorization response did not match the state in the authorization request.
     * For authorization requests, the sdk will verify the state returned from redirect and the one sent in the request.
     */
    public static final String STATE_MISMATCH = "state_mismatch";

    /**
     * The intent to launch Activity is not resolvable by the OS or the intent doesn't contain the required data.
     */
    public static final String UNRESOLVABLE_INTENT = "unresolvable_intent";

    /**
     * Unsupported url, cannot perform adfs authority validation.
     */
    public static final String UNSUPPORTED_URL = "unsupported_url";

    /**
     * The authority is not supported for authority validation. The sdk supports b2c authority, but we don't support b2c authority validation yet.
     * Only well-known host will be supported.
     */
    public static final String AUTHORITY_VALIDATION_NOT_SUPPORTED = "authority_validation_not_supported";

    /**
     * chrome_not_installed: Chrome is not installed on the device. The sdk uses chrome custom tab for
     * authorization requests if available, and will fall back to chrome browser.
     */
    public static final String CHROME_NOT_INSTALLED = "chrome_not_installed";

    /**
     * The user provided in the acquire token request doesn't match the user returned from server.
     */
    public static final String USER_MISMATCH = "user_mismatch";

    /**
     * Extra query parameters set by the client app is already sent by the sdk.
     */
    public static final String DUPLICATE_QUERY_PARAMETER = "duplicate_query_parameter";

    /**
     * Temporary non-exposed error code to indicate that ADFS authority validation fails. ADFS as authority is not supported
     * for preview.
     */
    static final String ADFS_AUTHORITY_VALIDATION_FAILED = "adfs_authority_validation_failed";

    /**
     * Failed to unwrap with the android keystore.
     */
    public static final String ANDROIDKEYSTORE_FAILED = "android_keystore_failed";

    /**
     * The authority url is invalid.
     */
    public static final String AUTHORITY_URL_NOT_VALID = "authority_url_not_valid";

    /**
     * Encounter errors during encryption.
     */
    public static final String ENCRYPTION_ERROR = "encryption_error";

    /**
     * Encounter errors during decryption.
     */
    public static final String DECRYPTION_ERROR = "decryption_error";

    /**
     * This request is missing a required parameter, includes an invalid parameter, includes a parameter more than
     * once, or is otherwise malformed.
     */
    public static final String INVALID_REQUEST = "invalid_request";

    /**
     * The client is not authorized to request an authorization code.
     */
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";

    /**
     * The resource owner or authorization server denied the request.
     */
    public static final String ACCESS_DENIED = "access_denied";

    /**
     * The request scope is invalid, unknown or malformed.
     */
    public static final String INVALID_SCOPE = "invalid_scope";

    /**
     * Represents 500/503/504 error codes.
     */
    public static final String SERVICE_NOT_AVAILABLE = "service_not_available";

    /**
     * Represents {@link java.net.SocketTimeoutException}.
     */
    public static final String REQUEST_TIMEOUT = "request_timeout";

    /**
     * Authority validation failed.
     */
    public static final String INVALID_INSTANCE = "invalid_instance";

    /**
     * Request to server failed, but no error and error_description is returned back from the service.
     */
    public static final String UNKNOWN_ERROR = "unknown_error";

    /**
     * Account is missing schema-required fields.
     */
    public static final String ACCOUNT_IS_SCHEMA_NONCOMPLIANT = "Account is missing schema-required fields.";

    /**
     * Credential is missing schema-required fields.
     */
    public static final String CREDENTIAL_IS_SCHEMA_NONCOMPLIANT = "Credential is missing schema-required fields.";

    /**
     * Device certificate request is invalid.
     */
    public static final String DEVICE_CERTIFICATE_REQUEST_INVALID = "Device certificate request is invalid";

    /**
     * Certificate encoding is not generated.
     */
    public static final String CERTIFICATE_ENCODING_ERROR = "Certificate encoding is not generated";

    /**
     * Key Chain private key exception.
     */
    public static final String KEY_CHAIN_PRIVATE_KEY_EXCEPTION = "Key Chain private key exception";

    /**
     * Signature exception.
     */
    public static final String SIGNATURE_EXCEPTION = "Signature exception";

    /**
     * Device certificate API has exception.
     */
    public static final String DEVICE_CERTIFICATE_API_EXCEPTION = "Device certificate API has exception";

    /**
     * The redirectUri for broker is invalid.
     */
    public static final String DEVELOPER_REDIRECTURI_INVALID = "The redirectUri for broker is invalid";

    /**
     * WebView  redirect url is not SSL protected.
     */
    public static final String WEBVIEW_REDIRECTURL_NOT_SSL_PROTECTED = "Redirect url scheme not SSL protected";

    /**
     * Package name is not resolved.
     */
    public static final String PACKAGE_NAME_NOT_FOUND = "Package name is not resolved";

    /**
     * Calling app could not be verified.
     */
    public static final String BROKER_APP_VERIFICATION_FAILED = "Calling app could not be verified";

    /**
     * App package name is not found in the package manager.
     */
    public static final String APP_PACKAGE_NAME_NOT_FOUND = "App package name is not found in the package manager";

    /**
     * Signature could not be verified.
     */
    public static final String BROKER_VERIFICATION_FAILED = "Signature could not be verified";

    /**
     * The broker app is not responding.
     */
    public static final String BROKER_APP_NOT_RESPONDING = "Broker application is not responding";

    /**
     * Failed to bind the service in broker app.
     */
    public static final String BROKER_BIND_SERVICE_FAILED = "Failed to bind the service in broker app";

    /**
     * Could not retrieve capabilities from broker apps.
     */
    public static final String FAILED_TO_GET_CAPABILITIES = "Could not get the capabilities";

    /**
     * Empty Context.
     */
    public static final String ANDROID_CONTEXT_IS_NULL = "Android Context is null.";

    /**
     * Empty Authorization Intent.
     */
    public static final String AUTHORIZATION_INTENT_IS_NULL = "Authorization intent is null.";

    /**
     * No available browser installed on the device.
     */
    public static final String NO_AVAILABLE_BROWSER_FOUND = "No available browser installed on the device.";

    /**
     * Refresh token request failed.
     */
    public static final String AUTH_REFRESH_FAILED = "Refresh token request failed";

    /**
     * STK patching failed.
     */
    public static final String STK_PATCHING_FAILED = "STK patching failed";

    /**
     * Primary refresh token request failed.
     */
    public static final String BROKER_PRT_REFRESH_FAILED = "Failed to refresh PRT";

    /**
     * Broker RT is invalid
     */
    public static final String INVALID_BROKER_REFRESH_TOKEN = "Broker refresh token is invalid";

    /**
     * Failed to retreive device state.
     */
    public static final String ERROR_RETRIEVING_DEVICE_STATE = "Error retrieving device state";

    /**
     * Device registration failed.
     */
    public static final String DEVICE_REGISTRATION_FAILED = "Device registration failed";

    /**
     * Request Cancelled for unknown reasons.
     */
    public static final String BROKER_REQUEST_CANCELLED = "Broker request cancelled";

    /**
     * User Cancelled the request.
     */
    public static final String USER_CANCELLED = "User cancelled";

    /**
     * The calling app is not supported by the broker.
     */
    public static final String UNSUPPORTED_BROKER_VERSION = "unsupported_broker_version";

    /**
     * Decryption failed.
     */
    public static final String DECRYPTION_FAILED = "decryption_failed";

    /**
     * Caller of the request is not a known instance.
     */
    public static final String UNKNOWN_CALLER = "unknown_caller";

    /**
     * The key cannot be found.
     */
    public static final String KEY_NOT_FOUND = "key_not_found";

    /**
     * AccountMode in configuration is set to multiple. However, the device is marked as shared (which requires single account mode).
     */
    public static final String MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_CODE = "multiple_account_pca_init_fail_on_shared_device";
    public static final String MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_MESSAGE = "AccountMode in configuration is set to multiple. However, the device is marked as shared (which requires single account mode).";

    /**
     * Multiple account PublicClientApplication could not be created for unknown reasons
     */
    public static final String MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE = "multiple_account_pca_init_fail_unknown_reason";
    public static final String MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE = "Multiple account PublicClientApplication could not be created for unknown reasons";

    /**
     * AccountMode in configuration is not set to multiple. Cannot initialize multiple account PublicClientApplication.
     */
    public static final String MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE = "multiple_account_pca_init_fail_account_mode";
    public static final String MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE = "AccountMode in configuration is not set to multiple. Cannot initialize multiple account PublicClientApplication.";

    /**
     * AccountMode in configuration is not set to single. Cannot initialize single account PublicClientApplication.
     */
    public static final String SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE = "single_account_pca_init_fail_account_mode";
    public static final String SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE = "AccountMode in configuration is not set to single. Cannot initialize single account PublicClientApplication.";

    /**
     * A single account public client application could not be created for unknown reasons.
     */
    public static final String SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE = "single_account_pca_init_fail_unknown_reason";
    public static final String SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE = "A single account public client application could not be created for unknown reasons.";

    /**
     * Some or all requested scopes where declined by the server. Developer should decide whether to continue
     * authentication with the granted scopes or end the authentication process.
     */
    public static final String DECLINED_SCOPE_ERROR_CODE = "declined_scope_error";
    public static final String DECLINED_SCOPE_ERROR_MESSAGE = "Some or all requested scopes have been declined by the Server";

}
