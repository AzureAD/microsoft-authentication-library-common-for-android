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
     * Indicates the request sequence used by cached credential service (if used) on server side
     */
    ccs_request_sequence,

    /**
     * Indicates names of the backup ipc that might be used in a given flow.
     */
    backup_ipc_used
}
