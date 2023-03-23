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

public class ClientException extends BaseException {

    // This is needed for backward compatibility with older versions of MSAL (pre 3.0.0)
    // When MSAL converts the result bundle it looks for this value to know about exception type
    // We moved the exception class to a new package with refactoring work,
    // but need to keep this value to older package name to avoid breaking older MSAL clients.
    public static final String sName = "com.microsoft.identity.common.exception.ClientException";

    private static final long serialVersionUID = -2318746536590284648L;

    /**
     * Indicates that a request was made for a specific environment, and this information was not
     * present on the device.
     */
    public static final String ENVIRONMENT_NOT_PRESENT = "environment_token_not_present";

    /**
     * Indicates that an invalid algorithm parameter is in use.
     */
    public static final String INVALID_ALG_PARAMETER = "invalid_algorithm_parameter";

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
     * Experienced a failure when attempting to execute PKCE.
     */
    public static final String PKCE_FAILURE = "pkce_failure";

    /**
     * Experienced a failure when attempting to generate PKCS.
     */
    public static final String PKCS_FAILURE = "pkcs_failure";

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
     * Emitted when the device Token is not present in successful response.
     */
    public static final String DEVICE_TOKEN_EMPTY_OR_NULL = "device_token_empty_or_null";

    /**
     * The sdk failed to parse the Json format.
     */
    public static final String JSON_PARSE_FAILURE = "json_parse_failure";

    /**
     * IOException happened, could be the device/network errors.
     */
    public static final String IO_ERROR = "io_error";

    /**
     * Emitted when a particular padding mechanism is requested but is not available in the environment.
     */
    public static final String NO_SUCH_PADDING = "no_such_padding";

    /**
     * Emitted when a the padding on a particular cipher is incorrect.
     */
    public static final String BAD_PADDING = "bad_padding";

    /**
     * Emitted when a the block size specified by a cipher is invalid.
     */
    public static final String INVALID_BLOCK_SIZE = "invalid_block_size";

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
     * The designated crypto alg is not supported.
     */
    public static final String NO_SUCH_ALGORITHM = "no_such_algorithm";

    public static final String NO_SUCH_PROVIDER = "no_such_provider";

    public static final String INVALID_KEY_SPEC = "invalid_key_spec";

    public static final String INVALID_CERTIFICATE_REQUEST = "invalid_cert_request";

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
     * Emitted when the KeyStore generates a certificate that does not match the designated key size.
     * Due to a bug in some versions of Android, keySizes may not be exactly as specified
     * To generate a 2048-bit key, two primes of length 1024 are multiplied -- this product
     * may be 2047 in length in some cases which causes Nimbus to crash. To avoid this,
     * check the keysize prior to returning the generated KeyPair.
     */
    public static final String BAD_KEY_SIZE = "keystore_produced_invalid_cert";

    /**
     * Emitted when the requested crypto provider is unavailable in the device environment.
     */
    public static final String ANDROID_KEYSTORE_UNAVAILABLE = "android_keystore_unavailable";

    /**
     * Emitted when the KeyStore fails to initialize due to unsupported arguments.
     */
    public static final String INVALID_ALG = "keystore_initialization_failed";

    /**
     * Emitted when the target KeyStore has not been initialized (loaded).
     */
    public static final String KEYSTORE_NOT_INITIALIZED = "keystore_not_initialized";

    /**
     * Emitted if any of the certificates in the keystore fail to load.
     */
    public static final String CERTIFICATE_LOAD_FAILURE = "certificate_load_failure";

    /**
     * Emitted when the Protection Params provided to the KeyStore are invalid or insufficient.
     * This error will be emitted if the underlying key material has been cleared or removed from
     * the keystore.
     */
    public static final String INVALID_PROTECTION_PARAMS = "protection_params_invalid";

    /**
     * Invalid key: cannot be used due to invalid encoding, wrong length, uninitialized, etc.
     */
    public static final String INVALID_KEY = "invalid_key";

    /**
     * Private key material cannot be loaded for use.
     */
    public static final String INVALID_KEY_MISSING = INVALID_KEY + "_private_key_missing";

    /**
     * Emitted when the target certificate's thumbprint cannot be computed due to lack of support for
     * SHA-256.
     */
    public static final String THUMBPRINT_COMPUTATION_FAILURE = "failed_to_compute_thumbprint_with_sha256";

    /**
     * Emitted when the requested export format of our public key is unknown or unsupported.
     */
    public static final String UNKNOWN_EXPORT_FORMAT = "unknown_public_key_export_format";

    /**
     * Emitted when the Android subsystem emits errors thrown while constructing new JSON objects.
     */
    public static final String JSON_CONSTRUCTION_FAILED = "json_construction_failed";

    /**
     * The current thread of execution was interrupted.
     */
    public static final String INTERRUPTED_OPERATION = "operation_interrupted";

    /**
     * Generic signing failure.
     */
    public static final String SIGNING_FAILURE = "failed_to_sign";

    /**
     * Generic decryption failure.
     */
    public static final String DECRYPTION_FAILURE = "failed_to_decrypt";

    /**
     * Emitted when an error is encountered during signing.
     */
    public static final String JWT_SIGNING_FAILURE = SIGNING_FAILURE + "_jwt";

    /**
     * Emitted if the STS returns an unexpected/incorrect token_type.
     * <p>
     * Example: Client requests a PoP token, but a Bearer token is returned.
     */
    public static final String AUTH_SCHEME_MISMATCH = "auth_scheme_mismatch";

    /**
     * The requested auth scheme for token requests is not supported
     * by the required broker protocol version.
     */
    public static final String AUTH_SCHEME_NOT_SUPPORTED = "auth_scheme_not_supported";

    /**
     * Nested app authentication is not supported
     * by the required broker protocol version.
     */
    public static final String NESTED_APP_AUTH_NOT_SUPPORTED = "nested_app_auth_not_supported";

    /**
     * Bound service is unavailable or not supported.
     */
    public static final String BOUND_SERVICE_UNAVAILABLE_OR_NOT_SUPPORTED = "bound_service_unavaliable_or_not_supported";

    /**
     * The returned bundle does not contain the expected data.
     */
    public static final String INVALID_BROKER_BUNDLE = "invalid_broker_bundle";

    /**
     * An account manager operation failed.
     */
    public static final String ACCOUNT_MANAGER_OPERATION_ERROR = "account_manager_operation_error";

    /**
     * An expected parameter is missing.
     */
    public static final String MISSING_PARAMETER = "missing_parameter";

    /**
     * A required account cannot be found.
     */
    public static final String ACCOUNT_NOT_FOUND = "account_not_found";

    /**
     * An access to perform a given operation is denied.
     */
    public static final String ACCESS_DENIED = "access_denied";

    /**
     * Tokens missing
     */
    public static final String TOKENS_MISSING = "tokens_missing";

    /**
     * The HMAC being verified doesn't have the expected length.
     */
    public static final String UNEXPECTED_HMAC_LENGTH = "unexpected_hmac_length";

    /**
     * The HMAC being verified doesn't match with the expected one.
     */
    public static final String HMAC_MISMATCH = "hmac_mismatch";

    /**
     * The data is malformed.
     */
    public static final String DATA_MALFORMED = "data_malformed";

    /**
     * The Keyring write operation failed.
     */
    public static final String KEY_RING_WRITE_FAILURE = "storage_keyring_write_failure";

    /**
     * The Keyring read operation failed.
     */
    public static final String KEY_RING_READ_FAILURE = "storage_keyring_read_failure";

    /**
     * The powerLift log upload operation failed.
     */
    public static final String LOG_UPLOAD_FAILURE = "log_upload_failure";

    /**
     * The powerLift api key is invalid (empty/null).
     */
    public static final String INVALID_POWERLIFT_API_KEY = "invalid_powerlift_api_key";

    /**
     * The broker log upload feature is disabled.
     */
    public static final String LOG_UPLOAD_TO_POWERLIFT_FEATURE_DISABLED = "log_upload_to_powerlift_feature_disabled";

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

    @Override
    public String getExceptionName() {
        return sName;
    }
}
