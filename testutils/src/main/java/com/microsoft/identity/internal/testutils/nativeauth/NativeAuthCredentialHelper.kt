package com.microsoft.identity.internal.testutils.nativeauth

import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.internal.testutils.BuildConfig

object NativeAuthCredentialHelper {
    val nativeAuthSignInUsername: String
        get() {
            val username = BuildConfig.NATIVE_AUTH_SIGNIN_TEST_USERNAME
            return if (StringUtil.isNullOrEmpty(username)) {
                throw IllegalStateException("env var NATIVE_AUTH_SIGNIN_TEST_USERNAME value not set")
            } else {
                username
            }
        }

    val nativeAuthSignInPassword: String
        get() {
            val password = BuildConfig.NATIVE_AUTH_SIGNIN_TEST_PASSWORD
            return if (StringUtil.isNullOrEmpty(password)) {
                throw IllegalStateException("env var NATIVE_AUTH_SIGNIN_TEST_PASSWORD value not set")
            } else {
                password
            }
        }

    val nativeAuthSSPRUsername: String
        get() {
            val username = BuildConfig.NATIVE_AUTH_SSPR_TEST_USERNAME
            return if (StringUtil.isNullOrEmpty(username)) {
                throw IllegalStateException("env var NATIVE_AUTH_SSPR_TEST_USERNAME value not set")
            } else {
                username
            }
        }
}
