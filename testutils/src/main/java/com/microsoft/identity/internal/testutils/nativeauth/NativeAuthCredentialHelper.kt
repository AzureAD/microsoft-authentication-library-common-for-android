package com.microsoft.identity.internal.testutils.nativeauth

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.internal.testutils.BuildConfig
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig

object NativeAuthCredentialHelper {
    val nativeAuthTestConfig:  NativeAuthTestConfig
        get() {
            val configs = BuildConfig.NATIVE_AUTH_TEST_CONFIG
            return if (StringUtil.isNullOrEmpty(configs)) {
                throw IllegalStateException("env var NATIVE_AUTH_CONFIG value not set")
            } else {
                val type = object : TypeToken<Map<String, NativeAuthTestConfig.Config>>() {}.type
                Gson().fromJson(configs, type)
            }
        }
}
