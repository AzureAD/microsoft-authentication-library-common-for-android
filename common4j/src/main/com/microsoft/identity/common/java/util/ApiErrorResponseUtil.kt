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

internal fun String?.isRedirect(): Boolean {
    return this.contentEquals(other = "redirect", ignoreCase = true)
}

internal fun String?.isOOB(): Boolean {
    return this.contentEquals(other = "oob", ignoreCase = true)
}

internal fun String?.isPassword(): Boolean {
    return this.contentEquals(other = "password", ignoreCase = true)
}

internal fun String?.isCredentialRequired(): Boolean {
    return this.contentEquals(other = "credential_required", ignoreCase = true)
}

internal fun String?.isInvalidGrant(): Boolean {
    return this.contentEquals(other = "invalid_grant", ignoreCase = true)
}

internal fun String?.isInvalidRequest(): Boolean {
    return this.contentEquals(other = "invalid_request", ignoreCase = true)
}

internal fun String?.isPasswordTooWeak(): Boolean {
    return this.contentEquals(other = "password_too_weak", ignoreCase = true)
}

internal fun String?.isPasswordTooShort(): Boolean {
    return this.contentEquals(other = "password_too_short", ignoreCase = true)
}

internal fun String?.isPasswordTooLong(): Boolean {
    return this.contentEquals(other = "password_too_long", ignoreCase = true)
}

internal fun String?.isPasswordRecentlyUsed(): Boolean {
    return this.contentEquals(other = "password_recently_used", ignoreCase = true)
}

internal fun String?.isPasswordBanned(): Boolean {
    return this.contentEquals(other = "password_banned", ignoreCase = true)
}

internal fun String?.isPollInProgress(): Boolean {
    return this.contentEquals(other = "in_progress", ignoreCase = true)
}

internal fun String?.isPollSucceeded(): Boolean {
    return this.contentEquals(other = "succeeded", ignoreCase = true)
}

internal fun String?.isExplicitUserNotFound(): Boolean {
    return this.contentEquals(other = "user_not_found", ignoreCase = true)
}

internal fun String?.isUnsupportedChallengeType(): Boolean {
    return this.contentEquals(other = "unsupported_challenge_type", ignoreCase = true)
}

internal fun String?.isExpiredToken(): Boolean {
    return this.contentEquals(other = "expired_token", ignoreCase = true)
}

internal fun Int?.isUserNotFound(): Boolean {
    return this == 50034
}

internal fun Int?.isInvalidCredentials(): Boolean {
    return this == 50126
}

internal fun Int?.isOtpCodeIncorrect(): Boolean {
    return arrayOf(50181, 50184, 501811).contains(this)
}

internal fun Int?.isInvalidAuthenticationType(): Boolean {
    return this == 400002
}

fun Int?.isMFARequired(): Boolean {
    return this == 50076
}

internal fun String?.isVerificationRequired(): Boolean {
    return this.contentEquals(other = "verification_required", ignoreCase = true)
}

internal fun String?.isAttributeValidationFailed(): Boolean {
    return this.contentEquals(other = "attribute_validation_failed", ignoreCase = true)
}

internal fun String?.isAttributesRequired(): Boolean {
    return this.contentEquals(other = "attributes_required", ignoreCase = true)
}

internal fun String?.isUserAlreadyExists(): Boolean {
    return this.contentEquals(other = "user_already_exists", ignoreCase = true)
}

internal fun String?.isAuthNotSupported(): Boolean {
    return this.contentEquals(other = "auth_not_supported", ignoreCase = true) || this.contentEquals(other = "unsupported_auth_method", ignoreCase = true)
}

internal fun String?.isInvalidOOBValue(): Boolean {
    return this.contentEquals(other = "invalid_oob_value", ignoreCase = true)
}

internal fun List<Map<String, String>>.toAttributeList(): List<String> {
    val result = mutableListOf<String>()
    this.forEach { iterable ->
        iterable["name"]?.let { value ->
            result.add(value)
        }
    }
    return result.toList()
}

