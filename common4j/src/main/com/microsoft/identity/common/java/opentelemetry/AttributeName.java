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
package com.microsoft.identity.common.java.opentelemetry;

/**
 * Names of Open Telemetry Span Attributes we want to capture for broker's Spans.
 * NOTE : Any changes to this enum should also be made in the corresponding enum in Broker.
 */
public enum AttributeName {
    /**
     * The length of the response body returned from network request.
     */
    response_body_length,
    /**
     * Indicates if the JWT returned by eSTS is a valid JWT.
     */
    jwt_valid,
    /**
     * Indicates the algorithm for the JWE returned by eSTS.
     */
    jwt_alg,

    /**
     * Indicates name of the parent span.
     */
    parent_span_name,

    /**
     * Indicates the controller for crypto operation (in FIPS flows).
     */
    crypto_controller,

    /**
     * Indicates the crypto operation.
     */
    crypto_operation,

    /**
     * Indicates the stack trace from an crypto operation exception.
     */
    crypto_exception_stack_trace,

    /**
     * Indicates the request id value for cached credential service (if used) on server side
     */
    ccs_request_id,

    /**
     * Indicates which CertBasedAuthChallengeHandler was handling the CBA flow.
     */
    cert_based_auth_challenge_handler,

    /**
     * Indicates if PivProvider (part of YubiKit) was already present in the
     * Security static list prior to adding a new PivProvider.
     */
    cert_based_auth_existing_piv_provider_present,

    /**
     * Indicates which CBA flow the user intended to select.
     */
    cert_based_auth_user_choice,

    /**
     * Indicates the public key algorithm type of the selected certificate.
     */
    cert_based_auth_public_key_algo_type,

    /**
     * The type of the error. Generally the class name of an exception.
     */
    error_type,

    /**
     * An error code.
     */
    error_code,

    /**
     * The IPC strategy being used.
     */
    ipc_strategy,

    /**
     * The API ID of an MSAL PCA method.
     */
    public_api_id,

    /**
     * The name of the controller being used to process the request.
     */
    controller_name,

    /**
     * The name of the application making the request.
     */
    application_name,

    /**
     * The correlation id sent from client app
     */
    correlation_id,

    /**
     * The correlation id sent from client app.
     * This is a second attribute name to denote EUDB compliance.
     * We will only emit this value when we have a tenant id, or when we are in unauthenticated scenarios.
     */
    correlation_id_v2,

    /**
     * Indicates if token was return from token cache
     */
    is_serviced_from_cache,

    /**
     * The message accompanying an Exception.
     */
    error_message,

    /**
     * Indicates if device id claims were requested
     */
    is_device_id_claims_requested,

    /**
     * The content type of the response returned by eSTS for the request.
     */
    response_content_type,

    /**
     * The http status code of the operation.
     */
    http_status_code,

    /**
     * The number of unique cacheable silent requests currently being tracked in the executing command map.
     * This represents deduplicated commands that are either executing or waiting in the thread pool queue.
     */
    num_concurrent_silent_requests,

    /**
     * The number of tasks waiting in the silent request thread pool queue to be executed.
     * This does not include tasks currently being executed by worker threads.
     */
    silent_requests_queue_size,
    /**
     * The size of the silent request executor pool.
     */
    silent_executor_pool_size,

    /**
     * The outcome of cancellation signal processing for a timed-out ATS request.
     * Only emitted when the cancellation flight is enabled and a timeout triggers cancellation.
     */
    cancellation_outcome,

    /**
     * The time (in milliseconds) spent in executing the save method in OAuth2TokenCache.
     */
    elapsed_time_cache_save,

    /**
     * The time (in milliseconds) spent in executing the load method in OAuth2TokenCache.
     */
    elapsed_time_cache_load,

    /**
     * The time (in milliseconds) spent in executing the loadAggregatedAccountData method in OAuth2TokenCache.
     */
    elapsed_time_cache_load_aggregated_account_data,

    /**
     * The time (in milliseconds) spent in executing the loadWithAggregatedAccountData method in OAuth2TokenCache.
     */
    elapsed_time_cache_load_with_aggregated_account_data,

    /**
     * The time (in milliseconds) spent in executing the optimized saveAndLoadAggregatedAccountData method in OAuth2TokenCache.
     */
    elapsed_time_cache_save_and_load_aggregated_account_data,

    /**
     * The time (in milliseconds) spent in executing the deleteAccessTokensWithIntersectingScopes method in MsalOAuth2TokenCache.
     */
    elapsed_time_cache_delete_access_tokens_with_intersecting_scopes,

    /**
     * The time (in milliseconds) spent in executing the removeCredential method in OAuth2TokenCache.
     */
    elapsed_time_cache_remove_credential,

    /**
     * The time (in milliseconds) spent in executing the get account method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_account,

    /**
     * The time (in milliseconds) spent in executing the getAccountWithAggregatedAccountData method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_accounts_with_aggregated_account_data,

    /**
     * The time (in milliseconds) spent in executing the getAccountByLocalAccountId method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_account_by_local_account_id,

    /**
     * The time (in milliseconds) spent in executing the getAccountWithAggregatedAccountDataByLocalAccountId method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_account_with_aggregated_account_data_by_local_account_id,

    /**
     * The time (in milliseconds) spent in executing the getAccounts method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_accounts,

    /**
     * The time (in milliseconds) spent in executing the getAllTenantAccountsForAccountByClientId method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_all_tenant_accounts_for_account_by_client_id,

    /**
     * The time (in milliseconds) spent in executing the getIdTokensForAccountRecord method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_id_tokens_for_account_record,

    /**
     * The time (in milliseconds) spent in executing the getAccountByHomeAccountId method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_account_by_home_account_id,

    /**
     * The time (in milliseconds) spent in executing the removeAccount method in OAuth2TokenCache.
     */
    elapsed_time_cache_remove_account,

    /**
     * The time (in milliseconds) spent in executing the clearAll method in OAuth2TokenCache.
     */
    elapsed_time_cache_clear_all,

    /**
     * The time (in milliseconds) spent in executing the getAllClientIds method in OAuth2TokenCache.
     */
    elapsed_time_cache_get_all_client_ids,

    /**
     * The total number of account records in the in-memory cache at the time of the request.
     */
    number_of_accounts_in_cache,

    /**
     * The total number of credential records in the in-memory cache at the time of the request.
     */
    number_of_credentials_in_cache,

    /**
     * The time (in milliseconds) spent on network when acquiring PRT.
     */
    elapsed_time_network_acquire_prt,

    /**
     * The time (in milliseconds) spent on network when acquiring nonce.
     */
    elapsed_time_network_acquire_nonce,

    /**
     * The time (in milliseconds) spent on network when acquiring AT.
     */
    elapsed_time_network_acquire_at,

    /**
     * The broker operation name.
     */
    broker_operation_name,

    /**
     * Fido challenge handler type.
     */
    fido_challenge_handler,

    /**
     * Fido manager type.
     */
    fido_manager,

    /**
     * Indicates the request sequence used by cached credential service (if used) on server side
     */
    ccs_request_sequence,

    /**
     * Indicates names of the backup ipc that might be used in a given flow.
     */
    backup_ipc_used,

    /**
     * Indicates the package name of the app making the request to the broker.
     */
    calling_package_name,

    /**
     * Indicates the requested cloud in the request made to broker.
     */
    requested_cloud_name,

    /**
     * Indicates the prt's home authority.
     */
    home_cloud_name,

    /**
     * Specify the result (or error stack trace) when determining if RT should be returned with AT response.
     */
    stop_returning_rt_result,

    /**
     * Indicates the operation name for Android KeyStore.
     */
    keystore_operation,

    /**
     * Indicates the stack trace from a Android KeyStore operation exception.
     */
    keystore_exception_stack_trace,

    /**
     * Indicates the exception message from a Android KeyStore operation exception.
     */
    keystore_exception_message,

    /**
     * Indicates the error code from a Android KeyStore operation exception.
     */
    keystore_numeric_error_code,

    /**
     * Indicates the new nonce found in the eSTS request.
     */
    is_sso_nonce_found_in_ests_request,

    /**
     * Indicates the new refresh token credential header attached in the eSTS request.
     */
    is_new_refresh_token_cred_header_attached,

    /**
     * The time (in milliseconds) spent on generating a keypair.
     */
    elapsed_time_keypair_generation,

    /**
     * Indicates the successful method used to generate a keypair.
     */
    key_pair_gen_successful_method,

    /**
     * Indicates the exception in generating a keypair.
     */
    keypair_gen_exception,

    /**
     * Records the stacktrace for an out-of-memory exception.
     */
    out_of_memory_exception_stacktrace,

    /**
     * Records if current flow is a mam flow.
     */
    is_mam_flow,

    /**
     * Records if current flow is a switch browser protocol.
     */
    is_switch_browser_protocol,

    /**
     * Records the browser package name.
     */
    browser_package_name,

    /**
     * Records the if browser package name supports custom tabs.
     */
    is_custom_tabs_supported,

    /**
     * Records the if the broker handled a switch browser request,
     */
    is_switch_browser_request_handled,

    /**
     * Records the if the broker handled a switch browser resume,
     */
    is_switch_browser_resume_handled,

    /**
     * Records if the Auth Tab was used in the switch browser flow (boolean).
     */
    auth_tab_used,

    /**
     * Records the Android Activity result code returned by the Auth Tab in the switch browser flow (integer).
     */
    auth_tab_result_code,

    /**
     * Records if Auth Tab is supported for the switch browser flow (boolean).
     */
    is_auth_tab_supported,

    /**
     * Records if the Auth Tab fell back to custom tabs in the switch browser flow (boolean).
     */
    auth_tab_fallback_to_custom_tabs,

    /**
     * The tenant id for the home tenant of the account for which PRT is required.
     */
    tenant_id,

    /**
     * Indicates the type of account such as AAD or MSA.
     */
    account_type,

    /**
     * Indicates the broker app that emits the event.
     * The broker is not necessarily the active broker.
     * e.g. An inactive broker app might be invoked during OnUpgrade.
     * (It should be renamed, but that would mess up the dashboard)
     */
    active_broker_package_name,

    /**
     * Indicates the current broker package name processing the request.
     */
    current_broker_package_name,

    /**
     * Records if the webcp is enabled in webview.
     */
    is_webcp_in_webview_enabled,

    /**
     * Records the if webview received an SSL error and
     * corresponding primary error code.
     */
    web_view_ssl_primary_error_code,

    /**
     * Record action name from Webview JavaScript Payload
     */
    authux_js_action_name,

    /**
     * Record action component from Webview JavaScript Payload
     */
    authux_js_action_component,

    /**
     * Record operation name from Webview JavaScript Payload
     */
    authux_js_operation,

    /**
     * Record whether or not the request stored a number match entry.
     */
    stored_number_match_entry,

    /**
     * Records the time (in milliseconds) spent on flight check for webcp.
     */
    web_cp_flight_get_time,

    /**
     * Indicates the OpenID issuer returned in the discovery document.
     */
    openid_issuer,

    /**
     * Indicates the reason for an invalid OpenID issuer.
     */
    openid_issuer_invalid_reason,

    /**
     * Indicates the authority used to make the OpenID configuration request.
     */
    openid_config_request_authority,

    /**
     * Indicates if the request is a redirect to playstore launch from webcp.
     */
    is_redirect_to_playstore_launch_from_webcp,

    /**
     * Records if current flow is in webcp flow.
     */
    is_in_web_cp_flow,

    /**
     * Indicates whether the filter-then-clone optimization is enabled for in-memory cache
     * getCredentialsFilteredBy()/getAccountsFilteredBy() operations.
     */
    is_filter_then_clone_enabled,

    /**
     * Indicates whether a desync was detected between the in-memory cache and SharedPreferences
     * during removeCredential(). True means the key was found in SharedPreferences
     * (via keySet()) but not in the in-memory map.
     */
    cache_key_in_storage_but_not_in_memory,

    /**
     * Elapsed time (in milliseconds) spent in executing the load() method in BrokerOAuth2TokenCache for in memory cache.
     */
    elapsed_time_in_memory_cache_load,

    /**
     * Passkey operation type (e.g., registration, authentication).
     */
    passkey_operation_type,

    /**
     * Passkey DOM exception name (if any).
     */
    passkey_dom_exception_name,

    /**
     * Origin extracted from the WebAuthn clientDataJSON response.
     */
    passkey_origin,

    /**
     * AAGUID of the authenticator, extracted from the attestation authenticatorData (create flow only).
     */
    passkey_aaguid,

    /**
     *  Elapsed time (in milliseconds) spent in executing the save() method in BrokerOAuth2TokenCache.
     */
    elapsed_time_save_aggregated_account_data,

    /**
     *  Elapsed time (in milliseconds) spent in executing the loadAggregatedAccountData() method in BrokerOAuth2TokenCache.
     */
    elapsed_time_load_aggregated_account_data,

    /**
     * Indicates if account aggregation is skipped during saveTokenResult() call.
     */
    is_account_aggregation_skipped,

    /**
     * Indicates if the redirect URL in webview is opened in browser.
     */
    is_redirect_url_opened_in_browser,

    /**
     * Number of PRT accounts for which SSO token generation succeeded in a Browser SSO request.
     * DataClassification: SystemMetadata.
     */
    browser_sso_success_count,

    /**
     * Number of PRT accounts for which SSO token generation failed in a Browser SSO request.
     * DataClassification: SystemMetadata.
     */
    browser_sso_failure_count,

    /**
     * Indicates the number of retry attempts made in DRS discovery when the retry policy is enabled.
     */
    drs_discovery_retry_number,

    /**
     * Indicates the number of retry attempts made in TLS connection when the retry policy is enabled.
     */
    client_tls_retry_number,

    //region KeyPair generation

    /**
     * Describes the keypair generation operation.
     */
    key_pair_gen_description,

    /**
     * Indicates the algorithm used to generate a keypair.
     */
    key_pair_gen_algorithm,

    /**
     * Indicates the encryption paddings used to generate a keypair.
     */
    key_pair_gen_encryption_paddings,

    /**
     * Indicates the history of key generation failures with details of each failed attempt.
     */
    key_pair_gen_failure_history,

    /**
     * The time (in nanoseconds) spent on generating a keypair.
     */
    key_pair_gen_elapsed_time,

    /**
     * Indicates whether the conservative key generation spec for legacy devices (API &lt;= 30)
     * flight is enabled. Used to validate the rollout/effectiveness of the legacy-device fix.
     */
    key_pair_gen_conservative_spec_flight_enabled,

    //endregion

    //region Secret Key Wrapping

    /**
     * Indicates the supported paddings for key pair in the device.
     */
    key_pair_supported_paddings,

    /**
     * Records the prioritized list of cipher specifications used for secret key wrapping/unwrapping.
     */
    available_transformation_list,

    /**
     * Records the full Cipher transformation string (e.g., "AES/GCM/NoPadding").
     */
    elected_cipher_transformation,

    /**
     * Indicates the transformation used for wrapping/unwrapping the secret key.
     */
    secret_key_transformation,

    /**
     * Indicates the algorithm used for wrapping/unwrapping the secret key.
     */
    secret_key_algorithm,

    /**
     * Indicates the wrapped secret key serializer id.
     */
    secret_key_wrapping_serializer_id,

    /**
     * The size (in bits) of the secret key.
     */
    secret_key_size,

    /**
     * The time (in milliseconds) spent on secret key serialization/deserialization.
     */
    secret_key_serialization_duration,

    /**
     * Indicates which check in the secret-key read path triggered a wipe of existing key material
     * (e.g. an unrecoverable load error, or an orphaned wrapped-key file whose keystore key is gone).
     * Legitimate first-time reads (no keystore key and no wrapped-key file) are intentionally not recorded.
     */
    secret_key_wipe_reason,

    /**
     * The actual root cause (simple class name + message) of the exception that caused a secret-key
     * read failure (e.g. "IOException: /data/.../key (No space left on device)"). This is the
     * underlying failure the ClientException wraps, captured unconditionally so the real reason is
     * never lost behind the generic wrapper message, even for deeply nested cause chains.
     */
    secret_key_read_root_cause,

    /**
     * Whether the KeyStore failure that triggered a secret-key wipe is transient (retry may succeed)
     * or permanent. One of TRANSIENT, NOT_TRANSIENT (API 33+, derived from
     * KeyStoreException.isTransientFailure()), API_TOO_OLD (API < 33, cannot be determined), or
     * NOT_KEYSTORE_ERROR (no KeyStoreException in the cause chain).
     */
    keystore_error_transience,

    /**
     * Indicates if an external handler was found to handle the openid-vc:// URI.
     */
    is_openid_vc_handler_found,

    /**
     * Indicates whether a broker-install {@code intent://} request was blocked because its target
     * package was not the allow-listed store. Set on the {@link SpanName#ProcessBrokerInstallIntent}
     * span (emitted only when the validation flight is enabled).
     */
    is_broker_install_intent_blocked,
    
    //endregion

    //region Device Registration IPC Attributes

    /**
     * Indicates the status of content provider IPC strategy for device registration.
     */
    content_provider_status,

    /**
     * Indicates the status of bound service IPC strategy for device registration.
     */
    bound_service_status,

    /**
     * Indicates the status of Legacy Account Manager IPC strategy for device registration.
     */
    legacy_account_manager_status,

    /**
     * Indicates the name of the device registration protocol being executed.
     */
    device_registration_protocol_name,

    //endregion

    //region WebView target=_blank navigation

    /**
     * Indicates which routing path was taken for a target=_blank URL intercepted
     * by onCreateWindow: "null_url", "non_ssl", "non_tlr_inline", or "tlr_browser".
     */
    target_blank_navigation_route,

    //endregion

    //region x-ms-clientdata server telemetry attributes

    /**
     * The server-side error code returned in the x-ms-clientdata header or clientdata
     * query parameter from eSTS / MSA.
     */
    server_error,

    /**
     * The server-side sub-error code returned in the x-ms-clientdata header or clientdata
     * query parameter from eSTS / MSA.
     */
    server_sub_error,

    /**
     * The cloud instance returned in the x-ms-clientdata header or clientdata query
     * parameter from eSTS / MSA (e.g. "public", "usgov").
     */
    server_cloud_instance,

    /**
     * The caller data boundary returned in the x-ms-clientdata header or clientdata
     * query parameter from eSTS / MSA, indicating the data residency boundary.
     */
    server_caller_data_boundary,

    //endregion
}
