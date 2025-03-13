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
package com.microsoft.identity.common.internal.util

import android.content.Context
import android.content.RestrictionsManager
import android.os.Bundle
import com.microsoft.identity.common.internal.broker.ipc.BrokerProcessIpcUtils
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory

import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.PackageHelper
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.logging.Logger

/**
 * Helper class to read the app restrictions manager of the current broker app or another broker app.
 */
class RestrictionsManagerHelper(
    private val context: Context,
    private val components: IPlatformComponents,
    private val restrictionsManager: RestrictionsManager
) {
    constructor(context: Context, components: IPlatformComponents) : this(
        context,
        components,
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
    )

    constructor(context: Context) : this(
        context,
        AndroidPlatformComponentsFactory.createFromContext(context),
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
    )

    companion object {
        val TAG: String = RestrictionsManagerHelper::class.java.simpleName
        const val BOOLEAN_VALUES_KEY = "booleanValuesKey"
        const val STRING_VALUES_KEY = "stringValuesKey"
    }

    /**
     * Creates a request bundle with the keys to be requested from the app restrictions manager.
     * The keys are filtered based on the type of the keys.
     *
     * @param stringKeysToInclude Keys to be requested from the app restrictions manager.
     * @param booleanKeysToInclude Keys to be requested from the app restrictions manager.
     * @return Bundle with the keys to be requested from the app restrictions manager.
     */
    fun createRequestBundle(
        stringKeysToInclude: Set<String>? = null,
        booleanKeysToInclude: Set<String>? = null
    ): Bundle {
        return Bundle().apply {
            stringKeysToInclude?.forEach { key ->
                putStringKeyOnBundleRequest(this, key)
            }
            booleanKeysToInclude?.forEach { key ->
                putBooleanKeyOnBundleRequest(this, key)
            }

        }
    }

    /**
     * Gets the [key] value from the [appPackageName] restrictions manager.
     * Returns null if the key is not found or failed to read the value.
     * The default [appPackageName] is the current app.
     */
    fun getString(key: String, appPackageName: String = context.packageName, default: String? = null): String? {
        return fetchAndFilterRestrictions(
            dataRequired = createRequestBundle(stringKeysToInclude = setOf(key)),
            appPackageName = appPackageName
        )?.getString(key) ?: default
    }

    /**
     * Gets the [key] value from the [appPackageName] restrictions manager.
     * Returns null if the key is not found or failed to read the value.
     * The default [appPackageName] is the current app.
     */
    fun getBoolean(key: String, appPackageName: String = context.packageName, default: Boolean = false): Boolean {
        return fetchAndFilterRestrictions(
            dataRequired = createRequestBundle(booleanKeysToInclude = setOf(key)),
            appPackageName = appPackageName
        )?.getBoolean(key) ?: default
    }

    /**
     * Reads the keys from the bundle of keys provided
     * and returns a bundle with the values from the app restrictions manager.
     * <p>
     * The provided bundle should contain a key determining the type of the keys to be requested
     * and the value should be an array list of keys.
     * Example: if the bundle contains a key "stringValues" with an array list of keys ["key1s", "key2s"]
     * and a key "booleanValues" with an array list of keys ["key0b"].
     * Then the returned bundle will contain the values for the keys "key1s", "key2s" and "key0b".
     *
     *
     * @param bundleOfKeys Bundle of keys to be requested from the app restrictions manager.
     * @return Bundle with the values from the app restrictions manager.
     */
    fun getFilteredBundleFromLocalRestrictionManager(bundleOfKeys: Bundle): Bundle {
        val resultBundle = Bundle()
        bundleOfKeys.keySet().forEach { keyType ->
            Logger.verbose(TAG, "getValuesFromBundle: $keyType")
            when (keyType) {
                BOOLEAN_VALUES_KEY -> {
                    bundleOfKeys.getStringArrayList(keyType)?.forEach { key ->
                        Logger.verbose(TAG, "getValuesFromBundle: $key")
                        resultBundle.putBoolean(key, getBooleanValue(key))
                    }
                }

                STRING_VALUES_KEY -> {
                    bundleOfKeys.getStringArrayList(keyType)?.forEach { key ->
                        Logger.verbose(TAG, "getValuesFromBundle: $key")
                        resultBundle.putString(key, getStringValue(key))
                    }
                }
            }
        }
        return resultBundle
    }

    /**
     * Reads the boolean value for a key from the app restrictions manager.
     *
     * @param key Key to be requested from the app restrictions manager.
     * @return Value for the key from the app restrictions manager.
     */
    private fun getBooleanValue(key: String): Boolean {
        val methodTag = "$TAG:getBooleanValue"
        val appRestrictions = restrictionsManager.applicationRestrictions
        if (appRestrictions != null) {
            return appRestrictions.getBoolean(key)
        }
        Logger.warn(methodTag, "ApplicationRestrictions is null")
        return false
    }

    /**
     * Reads the String value for a key from the app restrictions manager.
     *
     * @param key Key to be requested from the app restrictions manager.
     * @return Value for the key from the app restrictions manager.
     * Or null if the key is not found.
     */
    private fun getStringValue(key: String): String? {
        val methodTag = "$TAG:getStringValue"
        val appRestrictions = restrictionsManager.applicationRestrictions
        if (appRestrictions != null) {
            return appRestrictions.getString(key)
        }
        Logger.warn(methodTag, "ApplicationRestrictions is null")
        return null
    }

    /**
     * Add a key for a boolean to the request bundle.
     */
     private fun putBooleanKeyOnBundleRequest(bundle: Bundle, key: String) {
        val arrayList = bundle.getStringArrayList(BOOLEAN_VALUES_KEY)
        if (arrayList == null) {
            bundle.putStringArrayList(BOOLEAN_VALUES_KEY, ArrayList<String>())
        }
        bundle.getStringArrayList(BOOLEAN_VALUES_KEY)?.add(key)
    }

    /**
     * Add a key for a String to the request bundle.
     */
     private fun putStringKeyOnBundleRequest(requestBundle: Bundle, key: String) {
        val arrayList = requestBundle.getStringArrayList(STRING_VALUES_KEY)
        if (arrayList == null) {
            requestBundle.putStringArrayList(STRING_VALUES_KEY, ArrayList<String>())
        }
        requestBundle.getStringArrayList(STRING_VALUES_KEY)?.add(key)
    }

    /**
     * Fetches the restriction manager bundle from the [appPackageName],
     * filtering the keys based on the [dataRequired] bundle.
     */
    private fun fetchRestrictionManagerBundleFromTargetApp(
        appPackageName: String,
        dataRequired: Bundle
    ): Bundle? {
        val methodTag = "$TAG:fetchRestrictionManagerBundleFromTargetApp"
        if (!PackageHelper(context).isPackageInstalledAndEnabled(appPackageName)) {
            Logger.warn(methodTag, "$appPackageName is not installed, return null.")
            return null
        }
        return try {
            Logger.info(methodTag, "Request to read $appPackageName restriction manager")
            BrokerProcessIpcUtils.getIpcStrategyForRequestBetweenBrokers(context, components)
                .communicateToBroker(
                    BrokerOperationBundle(
                        BrokerOperationBundle.Operation.BROKER_READ_RESTRICTIONS_MANAGER,
                        appPackageName,
                        dataRequired
                    )
                )
        } catch (throwable: Throwable) {
            Logger.error(methodTag, "Communication to $appPackageName failed.", throwable)
            null
        }
    }

    /**
     * Fetches the restriction manager bundle from the [appPackageName],
     * filtering the keys based on the [dataRequired] bundle.
     */
    private fun fetchAndFilterRestrictions(dataRequired: Bundle, appPackageName: String): Bundle? {
        val methodTag = "$TAG:fetchAndFilterRestrictions"
        Logger.info(methodTag, "appPackageName: $appPackageName, current app: ${context.packageName}")
        return if (appPackageName.equals(context.packageName, ignoreCase = true)) {
            Logger.info(methodTag, "Request to read local restriction manager")
            getFilteredBundleFromLocalRestrictionManager(dataRequired)
        } else {
            Logger.info(methodTag, "Request to read $appPackageName restriction manager")
            return fetchRestrictionManagerBundleFromTargetApp(appPackageName, dataRequired)
        }
    }
}
