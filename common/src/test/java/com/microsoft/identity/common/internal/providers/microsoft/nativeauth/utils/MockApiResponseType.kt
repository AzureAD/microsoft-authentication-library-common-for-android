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

package com.microsoft.identity.common.internal.providers.microsoft.nativeauth.utils

enum class MockApiResponseType(val stringValue: String) {
    INVALID_REQUEST("InvalidRequest"),
    INVALID_TOKEN("InvalidToken"),
    INVALID_CLIENT("InvalidClient"),
    INVALID_GRANT("InvalidGrant"),
    INVALID_SCOPE("InvalidScope"),
    EXPIRED_TOKEN("ExpiredToken"),
    INVALID_PURPOSE_TOKEN("InvalidPurposeToken"),
    AUTH_NOT_SUPPORTED("AuthNotSupported"),
    USER_ALREADY_EXISTS("UserAlreadyExists"),
    USER_NOT_FOUND("UserNotFound"),
    EXPLICIT_USER_NOT_FOUND("ExplicityUserNotFound"),
    SLOW_DOWN("SlowDown"),
    SIGNIN_INVALID_PASSWORD("InvalidPassword"),
    INVALID_OOB_VALUE("InvalidOOBValue"),
    EXPLICIT_INVALID_OOB_VALUE("ExplicitInvalidOOBValue"),
    PASSWORD_TOO_WEAK("PasswordTooWeak"),
    PASSWORD_TOO_SHORT("PasswordTooShort"),
    PASSWORD_TOO_LONG("PasswordTooLong"),
    PASSWORD_RECENTLY_USED("PasswordRecentlyUsed"),
    PASSWORD_BANNED("PasswordBanned"),
    AUTHORIZATION_PENDING("AuthorizationPending"),
    CHALLENGE_TYPE_PASSWORD("ChallengeTypePassword"),
    CHALLENGE_TYPE_OOB("ChallengeTypeOOB"),
    UNSUPPORTED_CHALLENGE_TYPE("UnsupportedChallengeType"),
    CHALLENGE_TYPE_REDIRECT("ChallengeTypeRedirect"),
    CREDENTIAL_REQUIRED("CredentialRequired"),
    INITIATE_SUCCESS("InitiateSuccess"),
    TOKEN_SUCCESS("TokenSuccess"),
    ATTRIBUTES_REQUIRED("AttributesRequired"),
    VERIFICATION_REQUIRED("VerificationRequired"),
    ATTRIBUTE_VALIDATION_FAILED("AttributeValidationFailed"),
    SIGNUP_CONTINUE_SUCCESS("SignUpContinueSuccess"),
    SSPR_START_SUCCESS("SSPRStartSuccess"),
    SSPR_CONTINUE_SUCCESS("SSPRContinueSuccess"),
    SSPR_SUBMIT_SUCCESS("SSPRSubmitSuccess"),
    SSPR_POLL_SUCCESS("SSPRPollSuccess"),
    SSPR_POLL_IN_PROGRESS("SSPRPollInProgress"),
    SSPR_POLL_FAILED("SSPRPollFailed"),
    EXPLICITLY_USER_NOT_FOUND("ExplicityUserNotFound")
}
