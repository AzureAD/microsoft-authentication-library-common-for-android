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

package com.microsoft.identity.common.java.flighting;

import static com.microsoft.identity.common.java.commands.SilentTokenCommand.ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS;
import static com.microsoft.identity.common.java.net.UrlConnectionHttpClient.DEFAULT_CONNECT_TIME_OUT_MS;
import static com.microsoft.identity.common.java.net.UrlConnectionHttpClient.DEFAULT_READ_TIME_OUT_MS;

import lombok.NonNull;

/**
 * List of Active Common flights.
 */
public enum CommonFlight implements IFlightConfig {
    /**
     * Flight to control whether or not to use Network capability for performing network check.
     */
    USE_NETWORK_CAPABILITY_FOR_NETWORK_CHECK("UseNetworkCapabilityForNetworkCheck", false),
    /**
     * Flight to control whether to expose the CCS (CachedCredService) request ID in TokenResponse.
     * This flight is default-on 
     */
    EXPOSE_CCS_REQUEST_ID_IN_TOKENRESPONSE("ExposeCcsRequestIdInTokenResponse", true),
    /**
     * Flight to control whether to expose the CCS (CachedCredService) request sequence in TokenResponse.
     * This flight is default-on 
     */
    EXPOSE_CCS_REQUEST_SEQUENCE_IN_TOKENRESPONSE("ExposeCcsRequestSequenceInTokenResponse", true),

    /**
     * Flight to control the timeout duration for Acquire Token Silent Calls
     * The default value is set to ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS.
     */
    ACQUIRE_TOKEN_SILENT_TIMEOUT_MILLISECONDS("AcquireTokenSilentTimeoutMilliSeconds", ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS),

    /**
     * Flight to enable passkey registration feature.
     */
    ENABLE_PASSKEY_REGISTRATION("EnablePasskeyRegistration", false),

    /**
     * Flight to control the timeout duration for UrlConnection connect timeout.
     */
    URL_CONNECTION_CONNECT_TIME_OUT("UrlConnectionConnectTimeOut", DEFAULT_CONNECT_TIME_OUT_MS),

    /**
     * Flight to control the timeout duration for UrlConnection read timeout.
     */
    URL_CONNECTION_READ_TIME_OUT("UrlConnectionReadTimeOut", DEFAULT_READ_TIME_OUT_MS),

    /**
     * Flight to disable the network connectivity check.
     */
    DISABLE_NETWORK_CONNECTIVITY_CHECK("DisableNetworkConnectivityCheck", true),

    /**
     * Flight to stop returning AAD RT back to calling apps.
     */
    STOP_RETURNING_AAD_RT_BACK_TO_CALLING_APP("StopReturningAadRtBackToCallingApp", false),

    /**
     * Flight to enable the legacy FIDO security key additional logic. Default is true for common.
     */
    ENABLE_LEGACY_FIDO_SECURITY_KEY_LOGIC("EnableLegacyFidoSecurityKeyLogic", true),

    /**
     * Flight to enable the re-attachment of new PRT header logic. Default is true.
     */
    ENABLE_ATTACH_NEW_PRT_HEADER_WHEN_NONCE_EXPIRED("EnableAttachNewPrtHeaderWhenNonceExpired", true),

    /**
     * Flight to enable the new key generation spec for wrap key using PURPOSE_WRAP_KEY in key gen spec. Default is true.
     * This is applicable for API >= 28
     */
    ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY("EnableNewKeyGenSpecForWrapWithPurposeWrapKey", true),

    /**
     * Flight to enable the attachment of PRT header in cross cloud requests. Default is true.
     */
    ENABLE_ATTACH_PRT_HEADER_WHEN_CROSS_CLOUD("EnableAttachPrtHeaderWhenCrossCloud", true),

    /**
     * Flight to make the state parameter required for the switch browser protocol.
     */
    SWITCH_BROWSER_PROTOCOL_REQUIRES_STATE("SwitchBrowserProtocolRequiresState", false),

    /**
     * Flight to enable adding x-client-MN and x-client-WPAvailable extra query parameters
     */
    ENABLE_AM_API_WORKPROFILE_EXTRA_QUERY_PARAMETERS("EnableAmApiWorkProfileExtraQueryParameters", true),

    /** Flight to enable the new key generation without PURPOSE_WRAP_KEY. Default is true.
     * This is applicable for API >= 23
     */
    ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY("EnableNewKeyGenSpecForWrapWithoutPurposeWrapKey", true),

    /**
     * Flight to enable exposing the JavaScript API for AuthUx requests
     */
    ENABLE_JS_API_FOR_AUTHUX("EnableJsApiForAuthUx", true),

    /**
     * Flight to enable the new KEK algorithm for encryption/decryption of keys.
     */
    ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING("EnableOAEPWithSHAAndMGF1Padding", false),

    /**
     * Flight to enable the new KEK algorithm for encryption/decryption of keys.
     */
    ENABLE_KEYSTORE_BACKED_SECRET_KEY_PROVIDER("EnableKeyStoreBackedSecretKeyProvider", true),

    /**
     * Flight to control the WrappedSecretKey serializer version
     */
    WRAPPED_SECRET_KEY_SERIALIZER_VERSION("WrappedSecretKeySerializerVersion", 0),

    /**
     * Flight to enable handling the UI in edge to edge mode
     */
    ENABLE_HANDLING_FOR_EDGE_TO_EDGE("EnableHandlingEdgeToEdge", true),

    /**
     * Flight to enable the Web CP in WebView.
     */
    ENABLE_WEB_CP_IN_WEBVIEW("EnableWebCpInWebView", false),

    /**
     * Flight to enable the Playstore URL launch for broker apps.
     */
    ENABLE_PLAYSTORE_URL_LAUNCH("EnablePlaystoreUrlLaunch", false),

    /**
     * Flight to enable the WebView flow to not cancel and preserve WebView flow on SSL errors.
     * The web resource running into SSL will itself not be loaded.
     */
    SHOULD_PRESERVE_WEBVIEW_FLOW_ON_SSL_ERROR("ShouldPreserveWebViewFlowOnSslError", true),

    /**
     * Flight to enable adding username field in broker request for UiRequiredException from broker.
     */
    ADD_USERNAME_IN_UI_REQUIRED_EXCEPTION_BROKER_RESULT("AddUsernameInUiRequiredExceptionBrokerResult", true),

    /**
     * Flight to control the timeout to wait for tenant based flight in WebCP.
     */
    WEB_CP_WAIT_TIMEOUT_FOR_FLIGHTS("WebCpWaitTimeoutForFlights", 3000),

    /**
     * Flight to enable WebView security settings to prevent unauthorized access.
     */
    ENABLE_WEBVIEW_SECURITY_SETTINGS("EnableWebViewSecuritySettings", false),

    /**
     * Flight to enable OpenID issuer validation code which validates issuer against the open id well known
     * config endpoint and only reports the failure result.
     */
    ENABLE_OPENID_ISSUER_VALIDATION_REPORTING("EnableOpenIdIssuerValidationReporting", true),

    /**
     * Flight to disable Web Apps API.
     */
    DISABLE_WEB_APPS_API("DisableWebAppsApi", false),

    /**
     * Flight to control whether or not to use in memory cache for accounts and credentials.
     */
    USE_IN_MEMORY_CACHE_FOR_ACCOUNTS_AND_CREDENTIALS("UseInMemoryCacheForAccountsAndCredentials", false),

    /**
     * Flight to control whether or not to use the optimized saveAndLoadAggregatedAccountData() method.
     */
    CALL_REFACTORED_SAVE_AND_LOAD_AGGREGATED_ACCOUNT_METHOD("UseRefactoredSaveAndLoadAggregatedAccountMethod", false),

    /**
     * Flight to disable the unnecessary crypto operation purposes in device pop manager like encrypt, decrypt and wrap.
     */
    DISABLE_UNNECESSARY_CRYPTO_PURPOSES_FROM_DEVICE_POP_MANAGER ("DisableUnnecessaryCryptoPurposesFromDevicePopManager", false),

    /**
     * Flight to re-enable validating signing certificate chain for broker validation
     * We want to disable the check by default but have the ability to bring it back just in case.
     */
    RE_ENABLE_VALIDATE_SIGNING_CERT_CHAIN_BROKER_APPS("ReEnableValidateSigningCertChainBrokerApps", false),

    /**
     * Flight to enable the use of locks in name value storage to prevent concurrent access issues.
     */
    USE_LOCKS_IN_NAME_VALUE_STORAGE("UseLocksInNameValueStorage", false),
    /**
     * Flight to enable increased thread pool size for silent requests.
     * When true, uses 12 threads. When false, uses legacy 5 threads.
     */
    USE_INCREASED_DEFAULT_SILENT_REQUEST_THREAD_POOL_SIZE("UseIncreasedSilentRequestThreadPoolSize", false),

    /**
     * Flight to enable AuthTab as the authorization strategy for MSAL browser-based interactive flows.
     * Requires Chrome 137+ and AndroidX Browser 1.9.0+.
     */
    ENABLE_AUTH_TAB("EnableAuthTab", false);

    private String key;
    private Object defaultValue;
    CommonFlight(@NonNull String key, @NonNull Object defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }
}
