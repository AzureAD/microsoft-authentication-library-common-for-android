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

public class ClientException extends BaseException {

    /**
     * Indicates that a token was not found in the internal/adal/tokensharing ssostateserializer.
     */
    public static final String TOKEN_CACHE_ITEM_NOT_FOUND = "token_cache_item_not_found";

    /**
     * Deserialization error loading the token shared library item.
     */
    public static final String TOKEN_SHARING_DESERIALIZATION_ERROR = "token_sharing_deserialization_error";

    public static final String TOKEN_SHARING_MSA_PERSISTENCE_ERROR = "failed_to_persist_msa_credential";

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
     * A scope is required when making a token request
     */
    public static final String SCOPE_EMPTY_OR_NULL = "scope_empty_or_null";

    /**
     * The sdk failed to parse the Json format.
     */
    public static final String JSON_PARSE_FAILURE = "json_parse_failure";

    /**
     * IOException happened, could be the device/network errors.
     */
    public static final String IO_ERROR = "io_error";

    /**
     * The url is malformed.  Likely caused when constructing the auth request, authority, or redirect URI.
     */
    public static final String MALFORMED_URL = "malformed_url";

    /**
     * The authority is unknown.  Occurs when the authority is not part of configuration or the authority host is not recognized by Microsoft.
     */
    public static final String UNKNOWN_AUTHORITY = "unknown_authority";

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
     * Note: after MSAL 0.2.0 this error is no longer relevant.
     * chrome_not_installed: Chrome is not installed on the device. The sdk uses chrome custom tab for
     * authorization requests if available, and will fall back to chrome browser.
     */
    public static final String CHROME_NOT_INSTALLED = "chrome_not_installed";

    /**
     * The user provided in the acquire token request doesn't match the user returned from server.
     */
    public static final String USER_MISMATCH = "user_mismatch";

    /**
     * Home tenant of the BRT acccount doesn't match with WPJ account's tenant.
     */
    public static final String BRT_TENANT_MISMATCH = "brt_tenant_mismatch";

    /**
     * Extra query parameters set by the client app is already sent by the sdk.
     */
    public static final String DUPLICATE_QUERY_PARAMETER = "duplicate_query_parameter";

    /**
     * Extra query parameters set by the client app is already sent by the sdk.
     */
    public static final String UNKNOWN_ERROR = "unknown_error";

    /**
     * Temporary non-exposed error code to indicate that ADFS authority validation fails. ADFS as authority is not supported
     * for preview.
     */
    static final String ADFS_AUTHORITY_VALIDATION_FAILED = "adfs_authority_validation_failed";

    /**
     * Duplicate command.  The same command is already be processed.
     */
    public static final String DUPLICATE_COMMAND = "duplicate_command";

    /**
     * Constructor of ClientException.
     *
     * @param errorCode String
     */
    public ClientException(final String errorCode) {
        super(errorCode);
    }

    /**
     * Constructor of ClientException.
     *
     * @param errorCode    String
     * @param errorMessage String
     */
    public ClientException(final String errorCode, final String errorMessage) {
        super(errorCode, errorMessage);
    }

    /**
     * Constructor of ClientException.
     *
     * @param errorCode    String
     * @param errorMessage String
     * @param throwable    Throwable
     */
    public ClientException(final String errorCode, final String errorMessage, final Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }
}
