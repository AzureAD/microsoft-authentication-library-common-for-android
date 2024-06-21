package com.microsoft.identity.internal.testutils.nativeauth

import com.google.gson.Gson
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.internal.testutils.BuildConfig
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig

object NativeAuthCredentialHelper {
    val nativeAuthTestConfig:  NativeAuthTestConfig
        get() {
            val config = BuildConfig.NATIVE_AUTH_TEST_CONFIG
            return if (StringUtil.isNullOrEmpty(config)) {
                throw IllegalStateException("env var NATIVE_AUTH_CONFIG value not set")
            } else {
                Gson().fromJson(config, NativeAuthTestConfig::class.java)
            }
        }
}
