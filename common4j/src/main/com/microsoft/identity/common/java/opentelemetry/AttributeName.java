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
     * The tenant id for the home tenant of the account for which PRT is required.
     */
    tenant_id,
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
     * The size of the silent command executor queue when starting to process an ATS request.
     */
    num_concurrent_silent_requests,

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
     * The time (in milliseconds) spent in executing the saveAndLoadAggregatedAccountData method in OAuth2TokenCache.
     */
    elapsed_time_cache_save_and_load_aggregated_account_data,

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
     * Records if the request is a webcp authorize request.
     */
    is_webcp_authorize_request,

    /**
     * Records if the request is a webcp enrollment request.
     */
    is_webcp_enrollment_request,

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
     * Indicates if ests telemetry was skipped.
     */
    skipped_ests_telemetry,

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
}
