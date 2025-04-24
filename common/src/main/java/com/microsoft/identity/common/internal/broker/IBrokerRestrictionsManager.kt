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
package com.microsoft.identity.common.internal.broker

import android.os.Bundle

/**
 * Helper class to read the app restrictions manager of the current broker app or another broker app.
 */
interface IBrokerRestrictionsManager {
    companion object BrokerRestrictionsManagerKeys {
        /** Keys to group the values in the bundle. */
        const val BOOLEAN_VALUES_KEY = "booleanValuesKey"
        const val STRING_VALUES_KEY = "stringValuesKey"

        /** Keys to be used in the app restrictions manager to read the values. */
        // String keys
        const val PREFERRED_AUTH_CONFIG = "preferred_auth_config"
        // Boolean keys
        const val SUPPRESS_CAMERA_CONSENT = "sdm_suppress_camera_consent"

        /**
         * Creates a request bundle with the keys to be requested from the app restrictions manager.
         * The keys are filtered based on the type of the keys.
         *
         * @param stringKeys Keys to be requested from the app restrictions manager.
         * @param booleanKeys Keys to be requested from the app restrictions manager.
         * @return Bundle with the keys to be requested from the app restrictions manager.
         */
        @JvmStatic
        @JvmOverloads
        fun buildMultiValueRequest(stringKeys: Set<String>? = null, booleanKeys: Set<String>? = null): Bundle {
            val stringList = stringKeys?.let { ArrayList(it) }
            val booleanList = booleanKeys?.let { ArrayList(it) }
            return Bundle().apply {
                this.putStringArrayList(STRING_VALUES_KEY, stringList)
                this.putStringArrayList(BOOLEAN_VALUES_KEY, booleanList)
            }
        }
    }
    /**
     * Gets the [key] value from the [brokerAppPackageName] restrictions manager.
     * Returns null if the key is not found or failed to read the value.
     */
    fun getString(key: String, brokerAppPackageName: String, defaultValue: String? = null): String?

    /**
     * Gets the [key] value from the [brokerAppPackageName] restrictions manager.
     * Returns null if the key is not found or failed to read the value.
     */
    fun getBoolean(key: String, brokerAppPackageName: String, defaultValue: Boolean = false): Boolean

    /**
     * Reads the keys from the [bundleOfKeys] provided
     * and returns a bundle with the values from the [brokerAppPackageName] restrictions manager.
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
    fun getMultiValues(brokerAppPackageName: String, bundleOfKeys: Bundle) : Bundle
}
