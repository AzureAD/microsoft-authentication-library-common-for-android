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

    val nativeAuthSSPRUsername: String
        get() {
            val username = BuildConfig.NATIVE_AUTH_SSPR_TEST_USERNAME
            return if (StringUtil.isNullOrEmpty(username)) {
                throw IllegalStateException("env var NATIVE_AUTH_SSPR_TEST_USERNAME value not set")
            } else {
                username
            }
        }

    val nativeAuthLabsEmailPasswordAppId: String
        get() {
            val appId = BuildConfig.NATIVE_AUTH_LABS_EMAIL_PW_APP_ID
            return if (StringUtil.isNullOrEmpty(appId)) {
                throw IllegalStateException("env var NATIVE_AUTH_LABS_EMAIL_PW_APP_ID value not set")
            } else {
                appId
            }
        }

    val nativeAuthLabsAuthorityUrl: String
        get() {
            val authorityUrl = BuildConfig.NATIVE_AUTH_LABS_AUTHORITY_URL
            return if (StringUtil.isNullOrEmpty(authorityUrl)) {
                throw IllegalStateException("env var NATIVE_AUTH_LABS_AUTHORITY_URL value not set")
            } else {
                authorityUrl
            }
        }
}
