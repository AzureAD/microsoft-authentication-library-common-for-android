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
package com.microsoft.identity.common.opentelemetry;

/**
 * Names of Open Telemetry Span Attributes we want to capture for broker's Spans.
 */
public enum AttributeName {
    /**
     * The correlation id being used for the request. This can used to correlate the data with the
     * server side.
     */
    correlation_id,
    /**
     * The sku that made the request to the broker.
     */
    client_sku,
    /**
     * The version of the SKU that made the request to the broker.
     */
    sku_version,
    /**
     * The version of the broker being used in the request.
     */
    broker_version,
    /**
     * The content type of the response returned by eSTS for the request.
     */
    response_content_type,
    /**
     * The http status code of the operation.
     */
    http_status_code,
    /**
     * Indicates if PRT is present in the response returned by eSTS as part of requesting a PRT.
     */
    prt_response_rt_present,
    /**
     * Indicates if ID Token is present in the response returned by eSTS as part of requesting a PRT.
     */
    prt_response_id_present,
    /**
     * Indicates if Session Key JWT is present in the response returned by eSTS as part of
     * requesting a PRT.
     */
    prt_response_session_key_jwe_present,
    /**
     * Indicates the length of the IV returned by eSTS when returning token response.
     */
    iv_decoded_length,
    /**
     * Indicates the length of the derivedKeyCtx returned by eSTS when returning token response.
     */
    payload_ciphertext_length,
    /**
     * Indicates the length of the payloadCipherText returned by eSTS when returning token response.
     */
    derived_key_ctx_length,
    /**
     * The tenant id for the home tenant of the account for which PRT is required.
     */
    tenant_id,
    /**
     * Indicates the package name of the app making the request to the broker.
     */
    calling_package_name,
    /**
     * Indicates the version of the application making the request to the broker.
     */
    calling_package_version,
    /**
     * Indicates the minimum required broker protocol version specified by the client.
     */
    required_broker_protocol_version,
    /**
     * Indicates the broker protocol version negotiated between client and Broker.
     */
    negotiated_broker_protocol_version,
    /**
     * Indicates if the token was being force refreshed.
     */
    force_refresh,
    /**
     * Indicates the name of the Authentication Scheme being used for the request.
     * Possible Values: Bearer, PoP
     */
    auth_scheme_name,
    /**
     * Indicates the claims that were requested as part of requesting the token.
     */
    claims_request_json,
    /**
     * Indicates if the request was made from a shared device or not.
     */
    is_shared_device,
    /**
     * The Registration Source that describes how the device was registered.
     */
    reg_source,
    /**
     * The Registration Source that describes how the device was registered.
     */
    reg_type,
    /**
     * Indicates if the request was made as part of the interrupt flow executed on the client.
     */
    is_interrupt_flow,
}
