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
package com.microsoft.identity.common.java.providers.microsoft;

import com.microsoft.identity.common.java.providers.oauth2.AuthorizationErrorResponse;

/**
 * Sub class of {@link AuthorizationErrorResponse}.
 * Encapsulates Microsoft specific Authorization Result errors in addition to standard OAuth2 errors.
 */
public class MicrosoftAuthorizationErrorResponse extends AuthorizationErrorResponse {

    /**
     * Error string to return for authorization failure.
     */
    public static final String AUTHORIZATION_FAILED = "authorization_failed";

    /**
     * Error string to return if the user cancelled the flow.
     */
    public static final String USER_CANCEL = "user_cancelled";

    /**
     * Error string to return if the request is cancelled by the SDK.
     */
    public static final String SDK_AUTH_CANCEL = "auth_cancelled_by_sdk";

    /**
     * Error string to return if intent passed is null.
     */
    public static final String NULL_INTENT = "Received null intent";

    /**
     * Error string to return for invalid response from Authorization server.
     */
    public static final String AUTHORIZATION_SERVER_INVALID_RESPONSE = "The authorization server returned an invalid response.";

    /**
     * Error description string to return if the user cancelled the flow.
     */
    public static final String USER_CANCELLED_FLOW = "User pressed device back button to cancel the flow.";

    /**
     * Error description string to return if the user cancelled the flow.
     */
    public static final String SDK_CANCELLED_FLOW = "Sdk cancelled the auth flow as the app launched a new interactive auth request.";

    /**
     * Error string to return if the state parameter from authorization endpoint doesn't match with the request state.
     */
    public static final String STATE_NOT_THE_SAME = "Returned state from authorize endpoint is not the same as the one sent";

    /**
     * Error string to return if the state parameter is not returned from authorization endpoint.
     */
    public static final String STATE_NOT_RETURNED = "State is not returned";

    /**
     * Error string to return for unknown error.
     */
    public static final String UNKNOWN_ERROR = "Unknown error";

    /**
     * Error string to return for unknown result code.
     */
    public static final String UNKNOWN_RESULT_CODE = "Unknown result code returned ";

    /**
     * Error string to return if the broker is not installed.
     */
    public static final String BROKER_NEEDS_TO_BE_INSTALLED = "broker_needs_to_be_installed";
    public static final String BROKER_NEEDS_TO_BE_INSTALLED_ERROR_DESCRIPTION= "Device needs to have broker installed";

    /**
     * Error string to indicate that the device needs to be registered
     */
    public static final String DEVICE_REGISTRATION_NEEDED = "device_registration_needed";
    public static final String DEVICE_REGISTRATION_NEEDED_ERROR_DESCRIPTION = "Device needs to be registered to access the resource";

    /**
     * Error string to indicate that the device needs to be managed
     */
    public static final String DEVICE_NEEDS_TO_BE_MANAGED = "device_needs_to_be_managed";
    public static final String DEVICE_NEEDS_TO_BE_MANAGED_ERROR_DESCRIPTION = "Device needs to be managed to access the resource";

    /**
     * Error string to indicate that the device registration is not sufficient and needs
     * to be upgraded with strong keys
     */
    public static final String INSUFFICIENT_DEVICE_REGISTRATION = "insufficient_device_registration";
    public static final String INSUFFICIENT_DEVICE_REGISTRATION_ERROR_DESCRIPTION = "Device registration needs to be upgraded with strong keys";

    /**
     * Constructor of {@link MicrosoftAuthorizationErrorResponse}.
     *
     * @param error            error string returned from the Authorization Server.
     * @param errorDescription description of the error.
     */
    public MicrosoftAuthorizationErrorResponse(String error, String errorDescription) {
        super(error, errorDescription);
    }
}
