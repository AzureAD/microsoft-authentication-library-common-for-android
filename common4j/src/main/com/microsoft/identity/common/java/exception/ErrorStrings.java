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
package com.microsoft.identity.common.java.exception;

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
     * Error occurred while deserializing JSON string.
     */
    public static final String JSON_DESERIALIZATION_FAILURE = "json_deserialization_failure";

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
     * The request's Redirect URI is not matching the Redirect URI configured for the application.
     */
    public static final String INVALID_CLIENT = "invalid_client";

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
     * Signature exception.
     */
    public static final String SIGNATURE_EXCEPTION = "Signature exception";

    /**
     * The redirectUri for broker is invalid.
     */
    public static final String DEVELOPER_REDIRECTURI_INVALID = "The redirectUri for broker is invalid";

    /**
     * The uri from WebCP is invalid.
     */
    public static final String WEBCP_URI_INVALID = "webcp_uri_invalid";

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
    public static final String APP_PACKAGE_NAME_NOT_FOUND = "App package name is not found in the package manager.";

    /**
     * Signature could not be verified.
     */
    public static final String BROKER_VERIFICATION_FAILED = "Signature could not be verified";

    /**
     * Failed to bind the service in broker app.
     */
    public static final String BROKER_BIND_SERVICE_FAILED = "Failed to bind the service in broker app";

    /**
     * Empty Authorization Intent.
     */
    public static final String AUTHORIZATION_INTENT_IS_NULL = "Authorization intent is null.";
    /**
     * Primary refresh token request failed.
     */
    public static final String BROKER_PRT_REFRESH_FAILED = "Failed to refresh PRT";

    /**
     * Broker RT is invalid
     */
    public static final String INVALID_BROKER_REFRESH_TOKEN = "Broker refresh token is invalid";

    /**
     * Device registration failed.
     */
    public static final String DEVICE_REGISTRATION_FAILED = "Device registration failed";

    /**
     * Device registration upgrade failed.
     */
    public static final String UPGRADE_DEVICE_REGISTRATION_FAILED = "Upgrading Device registration failed";

    /**
     * Strong Device registration not enabled.
     */
    public static final String STRONG_DEVICE_REGISTRATION_NOT_ENABLED_ERROR_CODE = "strong_device_registration_not_enabled";
    public static final String STRONG_DEVICE_REGISTRATION_NOT_ENABLED_ERROR_MESSAGE = "Strong device registration required, but is not yet enabled in broker. Please contact your administrator for more information. Learn more here https://go.microsoft.com/fwlink/?linkid=2280772";

    /**
     * Device registration upgrade not supported in shared device.
     */
    public static final String DEVICE_REGISTRATION_UPGRADE_NOT_SUPPORTED_IN_SHARED_DEVICE_ERROR_CODE = "device_registration_upgrade_not_supported_in_shared_device";
    public static final String DEVICE_REGISTRATION_UPGRADE_NOT_SUPPORTED_IN_SHARED_DEVICE_ERROR_MESSAGE = "Token Binding CA policy is not supported in shared device. Please contact your administrator for more information. Learn more here https://go.microsoft.com/fwlink/?linkid=2280772";

    /**
     * Device registration upgrade not supported due to user mismatch.
     */
    public static final String DEVICE_REGISTRATION_UPGRADE_NOT_SUPPORTED_USER_MISMATCH_ERROR_CODE = "device_registration_upgrade_not_supported_user_mismatch";
    public static final String DEVICE_REGISTRATION_UPGRADE_NOT_SUPPORTED_USER_MISMATCH_ERROR_MESSAGE = "The currently signed in user is not a registered owner for this device. Please contact your administrator for more information. Learn more here https://go.microsoft.com/fwlink/?linkid=2280772";

    /**
     * Device unregistration/leave failed.
     */
    public static final String DEVICE_LEAVE_FAILED = "device_leave_failed";

    /**
     * Request Cancelled for unknown reasons.
     */
    public static final String BROKER_REQUEST_CANCELLED = "Broker request cancelled";

    /**
     * User Cancelled the request.
     */
    public static final String USER_CANCELLED = "User cancelled";

    /**
     * The broker app is too old to support the calling MSAL.
     */
    public static final String UNSUPPORTED_BROKER_VERSION_ERROR_CODE = "unsupported_broker_version";
    public static final String UNSUPPORTED_BROKER_VERSION_ERROR_MESSAGE = "Please update Intune Company Portal and/or Microsoft Authenticator to the latest version.";
    public static final String VID_INTERRUPT = "User will be redirected to VerifiedId endpoint to recover account";

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
    public static final String MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_MESSAGE =
            "This application is not supported in the shared device mode. " +
            "Please contact application developer to update the app.";

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

    /**
     * The device is in the shared mode, and its registration was deleted by the admin.
     * This is an irrecoverable error, and the admin has to re-prep the device.
     * */
    public static final String REGISTERED_SHARED_DEVICE_DELETED_ON_SERVER_ERROR_CODE =
            "registered_shared_device_deleted_on_server";

    public static final String DEVICE_DELETED_ON_SERVER_IRRECOVERABLE_ERROR_MESSAGE =
            "This device was deleted from the tenant. " +
                    "This is an irrecoverable error. Only tenant administrator can re-register this device.";

    /**
     * Home tenant of the BRT acccount doesn't match with WPJ account's tenant.
     */
    public static final String BRT_TENANT_MISMATCH_ERROR_MESSAGE =
            "Requested account is from a different organization. " +
                    "Please make sure to use your organizational account. " +
                    "If that doesn’t help, please return the device to your administrator.";

    /**
     * Device Code Flow only.
     * Device Code Flow (DCF) is not supported in broker exception
     */
    public static final String DEVICE_CODE_FLOW_NOT_SUPPORTED = "dcf_not_supported";

    /**
     * Device Code Flow only.
     * Authorization has not been completed yet.
     */
    public final static String DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_ERROR_CODE = "authorization_pending";

    /**
     * Device Code Flow only.
     * Authorization was declined by the user during Device Code Flow.
     */
    public final static String DEVICE_CODE_FLOW_AUTHORIZATION_DECLINED_ERROR_CODE = "authorization_declined";
    public final static String DEVICE_CODE_FLOW_AUTHORIZATION_DECLINED_ERROR_MESSAGE = "The end user has denied the authorization request. Re-run the Device Code Flow Protocol with another user.";

    /**
     * Device Code Flow only.
     * The token expired before the user authenticated with the user code.
     */
    public final static String DEVICE_CODE_FLOW_EXPIRED_TOKEN_ERROR_CODE = "expired_token";
    public final static String DEVICE_CODE_FLOW_EXPIRED_TOKEN_ERROR_MESSAGE = "The token has expired, therefore authentication is no longer possible with this flow attempt. Re-run the Device Code Flow Protocol to try again.";

    /**
     * Device Vode Flow only.
     * The token request sent a device code that was not recognized.
     */
    public final static String DEVICE_CODE_FLOW_BAD_VERIFICATION_ERROR_CODE = "bad_verification_code";
    public final static String DEVICE_CODE_FLOW_BAD_VERIFICATION_ERROR_MESSAGE = "The token request contains a device code that was not recognized. Verify that the client is sending the right device code.";

    /**
     * Device Code Flow only.
     * The token was polled again after it was already received.
     * Use error code in AuthenticationConstants.OAuth2ErrorCode
     */
    public final static String DEVICE_CODE_FLOW_INVALID_GRANT_ERROR_MESSAGE = "The token for this device code has already been redeemed. To receive another access token, please re-run the Device Code Flow protocol.";

    /**
     * Device Code Flow only.
     * The scope attached to the request was not valid, either formatted wrong or some scopes did not exist.
     */
    public final static String DEVICE_CODE_FLOW_INVALID_SCOPE_ERROR_MESSAGE = "The scope attached to the device code flow request is invalid. Please re-try with a valid scope.";

    /**
     * Device Code Flow only.
     * Use this message for when Device Code Flow fails with an error code that doesn't match any of the pre-defined Device Code Flow codes.
     */
    public final static String DEVICE_CODE_FLOW_DEFAULT_ERROR_MESSAGE = "Device Code Flow has failed with an unexpected error. The error code shown was received from the result object.";

    /**
     * Unexpected content type received in http response.
     */
    public static final String UNEXPECTED_HTTP_RESPONSE_CONTENT_TYPE = "unexpected_http_response_content_type";

    /**
     * An object was unexpectedly null. Used for explicit null checks, whereas NULL_POINTER_ERROR classifies NPEs.
     */
    public static final String NULL_OBJECT = "null_object";

    /**
     * Received a result code other than "ok" from an activity.
     */
    public static final String BAD_ACTIVITY_RESULT_CODE = "bad_activity_result_code";

    /**
     * Error occurred while attempting to get PendingIntent.
     */
    public static final String GET_PENDING_INTENT_ERROR = "get_pending_intent_error";

    /**
     * Operation cancelled while attempting to get PendingIntent.
     */
    public static final String GET_PENDING_INTENT_CANCELED = "get_pending_intent_canceled";

    /**
     * Error occurred while attempting to parse a URI due to a syntax error.
     */
    public static final String URI_SYNTAX_ERROR = "uri_syntax_error";

    /**
     * Error occurred if the activity can not be found to execute the given Intent
     */
    public static final String ACTIVITY_NOT_FOUND = "activity_not_found";

    /**
     * All web app sign out attempts failed.
     */
    public static final String ALL_WEBAPP_SIGN_OUTS_FAILED = "all_webapp_sign_outs_failed";

    /**
     * A specific web app error for Edge.
     */
    public static final String UI_NOT_ALLOWED = "ui_not_allowed";

    /**
     * The requested feature flight is disabled.
     */
    public static final String FLIGHT_DISABLED = "flight_disabled";

    /**
     * A generic error code used when no other error code is applicable.
     */
    public static final String UNEXPECTED_ERROR = "unexpected_error";

    /**
     * The operation is only allowed for Broker applications.
     */
    public static final String BROKER_ONLY_OPERATION = "broker_only_operation";

    /**
     * More than one app is registered to handle the custom URL scheme used for the redirect URI
     * of BrowserTabActivity. Only the current application should be listening on this scheme.
     */
    public static final String MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME = "multiple_apps_listening_url_scheme";

    /**
     * Switch browser flow: no browser launch strategy is available to handle the request.
     */
    public static final String SWITCH_BROWSER_NO_LAUNCH_STRATEGY = "no_launch_strategy";

    /**
     * Switch browser flow: a new browser switch request was received while one is already in progress.
     */
    public static final String SWITCH_BROWSER_ALREADY_IN_PROGRESS = "already_in_progress";

    /**
     * Switch browser flow: an intent was received that does not match any expected pattern.
     */
    public static final String SWITCH_BROWSER_UNEXPECTED_INTENT = "unexpected_intent";

    /**
     * Auth Tab flow: PROCESS_URI is missing from the intent extras.
     */
    public static final String AUTH_TAB_PROCESS_URI_MISSING = "PROCESS_URI is required to launch Auth Tab";

    /**
     * Auth Tab flow: BROKER_REDIRECT_URI is missing from the intent extras.
     */
    public static final String AUTH_TAB_REDIRECT_URI_MISSING = "BROKER_REDIRECT_URI is required to launch Auth Tab";

    /**
     * Auth Tab result code: the user cancelled authentication.
     */
    public static final String AUTH_TAB_CANCELED = "CANCELED";

    /**
     * Auth Tab result message: the user cancelled authentication.
     */
    public static final String AUTH_TAB_CANCELED_MESSAGE = "User cancelled authentication by returning from browser";

    /**
     * Auth Tab result code: browser redirect verification failed.
     */
    public static final String AUTH_TAB_VERIFICATION_FAILED = "VERIFICATION_FAILED";

    /**
     * Auth Tab result message: browser redirect verification failed.
     */
    public static final String AUTH_TAB_VERIFICATION_FAILED_MESSAGE = "Failed to verify the authenticity of the browser redirect";

    /**
     * Auth Tab result code: browser redirect verification timed out.
     */
    public static final String AUTH_TAB_VERIFICATION_TIMED_OUT = "VERIFICATION_TIMED_OUT";

    /**
     * Auth Tab result message: browser redirect verification timed out.
     */
    public static final String AUTH_TAB_VERIFICATION_TIMED_OUT_MESSAGE = "Timed out while waiting for browser redirect verification";
}
