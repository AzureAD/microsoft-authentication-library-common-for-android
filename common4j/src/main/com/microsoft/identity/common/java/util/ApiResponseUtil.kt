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

fun Int?.isUserAccountDoesNotExist(): Boolean {
    return this == 50034
}

fun Int?.isPasswordIncorrect(): Boolean {
    return this == 50126
}

fun Int?.isOtpCodeIncorrect(): Boolean {
    return this == 50181
}
