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
package com.microsoft.identity.common.java;

import com.microsoft.identity.common.java.util.ported.PropertyBag;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class AuthenticationConstants {

    /**
     * The Constant UTF8.
     */
    public static final String ENCODING_UTF8_STRING = "UTF-8";

    /**
     * The Constant ENCODING_UTF8.
     */
    public static final Charset ENCODING_UTF8 = Charset.forName(ENCODING_UTF8_STRING);

    /**
     * The Constant CHARSET_UTF8.
     */
    public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    /**
     * The Constant ASCII.
     */
    public static final String ENCODING_ASCII_STRING = "ASCII";

    /**
     * The Constant CHARSET_ASCII.
     */
    public static final Charset CHARSET_ASCII = Charset.forName(ENCODING_ASCII_STRING);

    /**
     * Default access token expiration time in seconds.
     */
    public static final int DEFAULT_EXPIRATION_TIME_SEC = 3600;

    /**
     * HTTPS scheme.
     */
    public static final String HTTPS_PROTOCOL_STRING = "https";

    public static final String SP800_108_LABEL = "AzureAD-SecureConversation";

    /**
     * Default scopes for OAuth2.
     */
    public static final Set<String> DEFAULT_SCOPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            OAuth2Scopes.OPEN_ID_SCOPE,
            OAuth2Scopes.OFFLINE_ACCESS_SCOPE,
            OAuth2Scopes.PROFILE_SCOPE
    )));

    /**
     * Represents the request code.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class UIRequest {

        /**
         * Represents the request of browser flow.
         */
        public static final int BROWSER_FLOW = 1001;

        /**
         * Represents the request of token flow.
         */
        public static final int TOKEN_FLOW = 1002;

        /**
         * Represents the request of broker flow.
         */
        public static final int BROKER_FLOW = 1003;
    }

    /**
     * Represents the code for the Broker response (to be returned to MSAL).
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class BrokerResponse {

        /**
         * Represents that the operation is cancelled.
         */
        public static final int BROKER_OPERATION_CANCELLED = 2001;

        /**
         * Represents that an error on the client side (i.e. webview/browser) is returned.
         */
        public static final int BROKER_ERROR_RESPONSE = 2002;


        /**
         * Represents that broker successfully returns the response.
         */
        public static final int BROKER_SUCCESS_RESPONSE = 2004;
    }

    /**
     * Holding all the constant value involved in the webview/browser.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Browser {

        /**
         * Represents the request object used to construct request sent to authorize endpoint.
         */
        public static final String REQUEST_MESSAGE = "com.microsoft.aad.adal:BrowserRequestMessage";

        /**
         * Represents the error code returned from webview.
         */
        public static final String RESPONSE_ERROR_CODE = "com.microsoft.aad.adal:BrowserErrorCode";

        /**
         * Represents the exception returned from webview.
         */
        public static final String RESPONSE_EXCEPTION = "com.microsoft.aad.adal:AuthenticationException";

        /**
         * Represents the final url that webview receives.
         */
        public static final String RESPONSE_FINAL_URL = "com.microsoft.aad.adal:BrowserFinalUrl";

        /**
         * Sub error returned by server representing the user cancel the auth flow.
         */
        public static final String SUB_ERROR_UI_CANCEL = "cancel";
    }

    /**
     * Represents the constant value of oauth2 params.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class OAuth2 {

        /**
         * String of access token.
         */
        public static final String ACCESS_TOKEN = "access_token";

        /**
         * String of authority.
         */
        public static final String AUTHORITY = "authority";

        /**
         * String of authorization code.
         */
        public static final String AUTHORIZATION_CODE = "authorization_code";

        /**
         * String of client id.
         */
        public static final String CLIENT_ID = "client_id";

        /**
         * String of client secret.
         */
        public static final String CLIENT_SECRET = "client_secret";

        /**
         * String of client info.
         */
        public static final String CLIENT_INFO = "client_info";

        /**
         * String value used to indicate client_info is requested from the token endpoint.
         */
        public static final String CLIENT_INFO_TRUE = "1";

        /**
         * String of preferred user name.
         */
        public static final String AAD_PREFERRED_USERNAME = "preferred_username";

        /**
         * String of code.
         */
        public static final String CODE = "code";

        /**
         * String of error.
         */
        public static final String ERROR = "error";

        /**
         * String of suberror.
         */
        public static final String SUBERROR = "suberror";

        /**
         * String of error description.
         */
        public static final String ERROR_DESCRIPTION = "error_description";

        /**
         * String of error codes.
         */
        public static final String ERROR_CODES = "error_codes";

        /**
         * String of error code.
         */
        public static final String ERROR_CODE = "error_code";

        /**
         * String of error subcode.
         */
        public static final String ERROR_SUBCODE = "error_subcode";

        /**
         * String of expires in.
         */
        public static final String EXPIRES_IN = "expires_in";

        /**
         * String of grant type.
         */
        public static final String GRANT_TYPE = "grant_type";

        /**
         * String redirect uri.
         */
        public static final String REDIRECT_URI = "redirect_uri";

        /**
         * String of refresh token.
         */
        public static final String REFRESH_TOKEN = "refresh_token";

        /**
         * String of response type.
         */
        public static final String RESPONSE_TYPE = "response_type";

        /**
         * String of scope.
         */
        public static final String SCOPE = "scope";

        /**
         * String of state.
         */
        public static final String STATE = "state";

        /**
         * String of token type.
         */
        public static final String TOKEN_TYPE = "token_type";

        /**
         * String of http web response body.
         */
        public static final String HTTP_RESPONSE_BODY = "response_body";

        /**
         * String of http web response headers.
         */
        public static final String HTTP_RESPONSE_HEADER = "response_headers";

        /**
         * String of http web response status code.
         */
        public static final String HTTP_STATUS_CODE = "status_code";

        /**
         * String of id token.
         */
        public static final String ID_TOKEN = "id_token";

        /**
         * String of sub in the id token.
         */
        public static final String ID_TOKEN_SUBJECT = "sub";

        /**
         * String of tenant id in the id token.
         */
        public static final String ID_TOKEN_TENANTID = "tid";

        /**
         * String of UPN in the id token claim.
         */
        public static final String ID_TOKEN_UPN = "upn";

        /**
         * String of given name in the id token claim.
         */
        public static final String ID_TOKEN_GIVEN_NAME = "given_name";

        /**
         * String of family name in the id token claim.
         */
        public static final String ID_TOKEN_FAMILY_NAME = "family_name";

        /**
         * String of unique name.
         */
        public static final String ID_TOKEN_UNIQUE_NAME = "unique_name";

        /**
         * String of email in the id token.
         */
        public static final String ID_TOKEN_EMAIL = "email";

        /**
         * String of identity provider in the id token claim.
         */
        public static final String ID_TOKEN_IDENTITY_PROVIDER = "idp";

        /**
         * String of oid in the id token claim.
         */
        public static final String ID_TOKEN_OBJECT_ID = "oid";

        /**
         * String of password expiration in the id token claim.
         */
        public static final String ID_TOKEN_PASSWORD_EXPIRATION = "pwd_exp";

        /**
         * String of password change url in the id token claim.
         */
        public static final String ID_TOKEN_PASSWORD_CHANGE_URL = "pwd_url";

        /**
         * String of FoCI field returned in the JSON response from token endpoint.
         */
        public static final String ADAL_CLIENT_FAMILY_ID = "foci";

        /**
         * String of has_chrome sent as extra query param to hide back button in the webview.
         */
        public static final String HAS_CHROME = "haschrome";

        /**
         * String for extended expiration time.
         */
        public static final String EXT_EXPIRES_IN = "ext_expires_in";

        /**
         * String for claims.
         */
        public static final String CLAIMS = "claims";

        /**
         * String as JSON key to send client capabilities.
         */
        public static final String CLIENT_CAPABILITIES_CLAIMS_LIST = "xms_cc";

        /**
         * String as JSON key to send access token claims.
         */
        public static final String CLIENT_CAPABILITY_ACCESS_TOKEN = "access_token";

        /**
         * String for cloud instance host name.
         */
        public static final String CLOUD_INSTANCE_HOST_NAME = "cloud_instance_host_name";
        /**
         * session key JWE.
         */
        public static final String SESSION_KEY_JWE = "session_key_jwe";

        /**
         * String as Query parameter key to send a V1 request to V2 endpoint
         */
        public static final String IT_VER_PARAM = "itver";
    }

    /**
     * Represents the constants value for Azure Active Directory.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class AAD {

        /**
         * AAD OAuth2 extension strings.
         */
        public static final String RESOURCE = "resource";

        /**
         * AAD OAuth2 Challenge strings.
         */
        public static final String BEARER = "Bearer";

        /**
         * AAD Oauth2 authorization.
         */
        public static final String AUTHORIZATION = "Authorization";

        /**
         * AAD Oauth2 string of realm.
         */
        public static final String REALM = "realm";

        /**
         * String of login hint.
         */
        public static final String LOGIN_HINT = "login_hint";

        /**
         * String of correlation id.
         */
        public static final String CORRELATION_ID = "correlation_id";

        /**
         * String of client request id.
         */
        public static final String CLIENT_REQUEST_ID = "client-request-id";

        /**
         * String of return client request id.
         */
        public static final String RETURN_CLIENT_REQUEST_ID = "return-client-request-id";

        /**
         * String of prompt behavior as always.
         */
        public static final String QUERY_PROMPT_VALUE = "login";

        /**
         * String for request id returned from Evo.
         **/
        public static final String REQUEST_ID_HEADER = "x-ms-request-id";

        /**
         * String for the host app name
         */
        public static final String APP_PACKAGE_NAME = "x-app-name";

        /**
         * String for the host app version
         */
        public static final String APP_VERSION = "x-app-ver";

        /**
         * String of AAD version.
         */
        public static final String AAD_VERSION = "ver";

        /**
         * Constant for v1 endpoint
         */
        public static final String AAD_VERSION_V1 = "1.0";

        /**
         * Constant for v2 endpoint
         */
        public static final String AAD_VERSION_V2 = "2.0";

        /**
         * Redirect URI parameter key to get the upn that triggers WPJ.
         */
        public static final String UPN_TO_WPJ_KEY = "username";

        /**
         * Redirect URI parameter key to get link to launch/install app.
         */
        public static final String APP_LINK_KEY = "app_link";

        /**
         * Broker redirect prefix.
         */
        public static final String REDIRECT_PREFIX = "msauth";

        /**
         * Device Registration redirect url host name
         */
        public static final String DEVICE_REGISTRATION_REDIRECT_URI_HOSTNAME = "wpj";
    }

    /**
     * Represents the constants for broker.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Broker {

        /**
         * Default timeout for broker tasks/futures.
         */
        public static final long BROKER_TASK_DEFAULT_TIMEOUT_MILLISECONDS = TimeUnit.SECONDS.toMillis(30);

        /**
         * Timeout for DCF token request
         */
        public static final long DCF_TOKEN_REQUEST_TIMEOUT_MILLISECONDS = TimeUnit.MINUTES.toMillis(15);

        /**
         * String of challenge response header.
         */
        public static final String CHALLENGE_RESPONSE_HEADER = "Authorization";

        /**
         * String of challenge response type.
         */
        public static final String CHALLENGE_RESPONSE_TYPE = "PKeyAuth";

        /**
         * String of challenge request header.
         */
        public static final String CHALLENGE_REQUEST_HEADER = "WWW-Authenticate";

        /**
         * Value of pkeyauth sent in the header.
         *
         * By declaring this, we're telling the server to use PKeyAuth instead of client TLS
         * for device authentication (if there is any).
         */
        public static final String PKEYAUTH_HEADER = "x-ms-PKeyAuth";

        /**
         * Value of supported pkeyauth version.
         */
        public static final String PKEYAUTH_VERSION = "1.0";

        /**
         * Account type string.
         */
        public static final String BROKER_ACCOUNT_TYPE = "com.microsoft.workaccount";

        /**
         * String of broker client ID.
         */
        public static final String BROKER_CLIENT_ID = "29d9ed98-a469-4536-ade2-f981bc1d605e";

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class OAuth2Scopes {

        /**
         * Scope to get get open id connect ID token
         */
        public static final String OPEN_ID_SCOPE = "openid";

        /**
         * Scope to give the app access to get resources on behalf of user for an extended time.
         * App can receive refresh tokens using this scope.
         */
        public static final String OFFLINE_ACCESS_SCOPE = "offline_access";

        /**
         * Scope to get user profile information as a part Id token
         */
        public static final String PROFILE_SCOPE = "profile";

        /**
         * Custom scope used to get PRT
         */
        public static final String AZA_SCOPE = "aza";

        /**
         * Scope to get email claim as part of the ID Token
         */
        public static final String EMAIL_SCOPE = "email";

        /**
         * Used in the interrupt flow. See BrokerJoinedAccountController for more info.
         * The BRT request made with this resourceID will contain the updated claim acquired in the interrupt flow performed before it.
         */
        public static final String CLAIMS_UPDATE_RESOURCE = "urn:aad:tb:update:prt/.default";
    }

    /**
     * Sdk platform and Sdk version fields.
     */
    public static final class SdkPlatformFields {

        /**
         * The String representing the sdk platform.
         */
        public static final String PRODUCT = "x-client-SKU";

        /**
         * The String representing the sdk version.
         */
        public static final String VERSION = "x-client-Ver";

        /**
         * The String representing the MSAL SdkType.
         */
        public static final String PRODUCT_NAME_MSAL = "MSAL.Android";

        /**
         * The String representing the MSAL.CPP SdkType.
         */
        public static final String PRODUCT_NAME_MSAL_CPP = "MSAL.xplat.Android";

        /**
         * The String representing the MSAL.XPLAT.Linux SdkType.
         */
        public static final String PRODUCT_NAME_MSAL_XPLAT_LINUX = "MSAL.xplat.Linux";
    }

    /**
     * Aliases for broadcasting events
     * to be used with {@link LocalBroadcaster}
     */
    public static final class LocalBroadcasterAliases {

        /**
         * an alias specifying that the current authorization action should be cancelled.
         */
        public static final String CANCEL_AUTHORIZATION_REQUEST = "cancel_authorization_request";

        /**
         * An alias that the intent contains authorization results, was formerly RETURN_INTERACTIVE_REQUEST_RESULT.
         */
        public static final String RETURN_AUTHORIZATION_REQUEST_RESULT = "return_authorization_request_result";

        /**
         * For broadcasting an event where the broker has returned the interactive acquire token result.
         */
        public static final String RETURN_BROKER_INTERACTIVE_ACQUIRE_TOKEN_RESULT = "return_broker_interactive_acquire_token_result";
    }

    /**
     * Fields for the broadcast {@link PropertyBag}.
     */
    public static final class LocalBroadcasterFields {

        /**
         * The associated value should be taken from {@link UIRequest}
         *
         */
        public static final String REQUEST_CODE = "com.microsoft.identity.client.request.code";

        /**
         * The associated value should be taken from {@link BrokerResponse}
         */
        public static final String RESULT_CODE = "com.microsoft.identity.client.result.code";
    }
}
