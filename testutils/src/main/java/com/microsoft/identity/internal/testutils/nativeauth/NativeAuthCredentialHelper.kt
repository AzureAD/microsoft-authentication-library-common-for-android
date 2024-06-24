package com.microsoft.identity.internal.testutils.nativeauth

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.internal.testutils.BuildConfig
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import java.io.File

object NativeAuthCredentialHelper {
    val nativeAuthTestConfig:  NativeAuthTestConfig
        get() {
            val filePath = BuildConfig.NATIVE_AUTH_TEST_CONFIG
            return if (StringUtil.isNullOrEmpty(filePath)) {
                throw IllegalStateException("env var NATIVE_AUTH_CONFIG value not set")
            } else {
                val configs = readJsonFile(filePath)
                val type = object : TypeToken<Map<String, NativeAuthTestConfig.Config>>() {}.type
                Gson().fromJson(configs, type)
            }
        }

    private fun readJsonFile(filePath: String): String {
        return File(filePath).readText(Charsets.UTF_8)
    }
}
