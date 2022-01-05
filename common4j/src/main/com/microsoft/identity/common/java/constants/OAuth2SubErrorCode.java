/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.constants;

/**
 * Represents the oauth2 sub error code.
 */
public final class OAuth2SubErrorCode {

    /**
     * Oauth2 suberror code for unauthorized_client.
     * <p>
     * Suberror code when Intune App Protection Policy is required.
     */
    public static final String PROTECTION_POLICY_REQUIRED = "protection_policy_required";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * Suberror code when token is expired or invalid for all resources
     * and scopes and shouldn't be retried again as-is.
     */
    public static final String BAD_TOKEN = "bad_token";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * Suberror code when failed to do device authentication during a token request.
     * Broker should make a request to DRS to get the current device status and act accordingly.
     */
    public static final String DEVICE_AUTHENTICATION_FAILED = "device_authentication_failed";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * SubError code for cases when client not in the Microsoft first party family group
     * redeems auth code or refresh token given to a client in the family.
     */
    public static final String CLIENT_MISMATCH = "client_mismatch";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * Conditional access suberror code when a policy enforces token lifetime.
     */
    public static final String TOKEN_EXPIRED = "token_expired";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * Conditional access suberror code which indicates a simple action is required by the end user, like MFA.
     */
    public static final String BASIC_ACTION = "basic_action";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * Conditional access suberror code which indicates additional action is
     * required that is in the user control, but is outside of the sign in session.
     * For example, enroll in MDM or register install an app that uses Intune app protection.
     */
    public static final String ADDITIONAL_ACTION = "additional_action";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * Conditional access suberror code where user will be shown an informational
     * message with no immediate remediation steps.
     * For example access was blocked due to location or the device is not domain joined.
     */
    public static final String MESSAGE_ONLY = "message_only";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * OpenId connect suberror code, where user consent is required.
     */
    public static final String CONSENT_REQUIRED = "consent_required";

    /**
     * Oauth2 suberror code for invalid_grant.
     * <p>
     * Custom sub error that notifies the user that their password has expired.
     */
    public static final String USER_PASSWORD_EXPIRED = "user_password_expired";
}
