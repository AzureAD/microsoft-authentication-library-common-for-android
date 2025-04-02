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

    val defaultPackageName: String
    /**
     * Gets the [key] value from the [appPackageName] restrictions manager.
     * Returns null if the key is not found or failed to read the value.
     */
    fun getString(key: String, appPackageName: String = defaultPackageName, defaultValue: String? = null): String?

    /**
     * Gets the [key] value from the [appPackageName] restrictions manager.
     * Returns null if the key is not found or failed to read the value.
     */
    fun getBoolean(key: String, appPackageName: String = defaultPackageName, defaultValue: Boolean = false): Boolean

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
    fun getFilteredBundleFromLocalRestrictionManager(bundleOfKeys: Bundle): Bundle
}
