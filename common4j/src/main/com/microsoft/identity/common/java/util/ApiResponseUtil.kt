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
package com.microsoft.identity.common.java.util

fun String?.isRedirect(): Boolean {
    return this.contentEquals(other = "redirect", ignoreCase = true)
}

fun String?.isOOB(): Boolean {
    return this.contentEquals(other = "oob", ignoreCase = true)
}

fun String?.isPassword(): Boolean {
    return this.contentEquals(other = "password", ignoreCase = true)
}

fun String?.isCredentialRequired(): Boolean {
    return this.contentEquals(other = "credential_required", ignoreCase = true)
}

fun String?.isInvalidGrant(): Boolean {
    return this.contentEquals(other = "invalid_grant", ignoreCase = true)
}

fun String?.isPasswordTooWeak(): Boolean {
    return this.contentEquals(other = "password_too_weak", ignoreCase = true)
}

// TODO replace with error code matching
fun String?.isPasswordTooShort(): Boolean {
    return this.contentEquals(other = "password_too_short", ignoreCase = true)
}

// TODO replace with error code matching
fun String?.isPasswordTooLong(): Boolean {
    return this.contentEquals(other = "password_too_long", ignoreCase = true)
}

// TODO replace with error code matching
fun String?.isPasswordRecentlyUsed(): Boolean {
    return this.contentEquals(other = "password_recently_used", ignoreCase = true)
}

// TODO replace with error code matching
fun String?.isPasswordBanned(): Boolean {
    return this.contentEquals(other = "password_banned", ignoreCase = true)
}

fun String?.isPollInProgress(): Boolean {
    return this.contentEquals(other = "in_progress", ignoreCase = true)
}

fun String?.isPollSucceeded(): Boolean {
    return this.contentEquals(other = "succeeded", ignoreCase = true)
}

fun String?.isExplicitUserNotFound(): Boolean {
    return this.contentEquals(other = "user_not_found", ignoreCase = true)
}

fun Int?.isUserAccountDoesNotExist(): Boolean {
    return this == 50034
}

fun Int?.isPasswordIncorrect(): Boolean {
    return this == 50126
}

fun Int?.isOtpCodeIncorrect(): Boolean {
    return this == 50181
}

fun String?.isVerificationRequired(): Boolean {
    return this.contentEquals(other = "verification_required", ignoreCase = true)
}

fun String?.isAttributeValidationFailed(): Boolean {
    return this.contentEquals(other = "attribute_validation_failed", ignoreCase = true)
}

fun String?.isAttributesRequired(): Boolean {
    return this.contentEquals(other = "attributes_required", ignoreCase = true)
}

fun String?.isAuthenticationRequired(): Boolean {
    return this.contentEquals(other = "authentication_required", ignoreCase = true)
}

fun String?.isInvalidRequest(): Boolean {
    return this.contentEquals(other = "invalid_request", ignoreCase = true)
}

fun String?.isUserAlreadyExists(): Boolean {
    return this.contentEquals(other = "user_already_exists", ignoreCase = true)
}

fun String?.isAuthNotSupported(): Boolean {
    return this.contentEquals(other = "auth_not_supported", ignoreCase = true)
}

fun String?.isInvalidOOBValue(): Boolean {
    return this.contentEquals(other = "invalid_oob_value", ignoreCase = true)
}
