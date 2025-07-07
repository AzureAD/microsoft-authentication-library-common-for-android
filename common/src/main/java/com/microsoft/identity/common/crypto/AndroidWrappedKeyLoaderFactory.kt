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
package com.microsoft.identity.common.crypto

import com.microsoft.identity.common.java.crypto.key.ISecretKeyProvider
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager

/**
 * Factory class for creating wrapped key loaders specific to the Android platform.
 *
 * This object is responsible for creating the appropriate implementation of [ISecretKeyProvider]
 * based on feature flag. It abstracts away the details of which
 * specific loader implementation should be used, allowing for runtime switching between
 * different implementations without affecting client code.
 */
object AndroidWrappedKeyLoaderFactory {
    const val WRAPPED_KEY_KEY_IDENTIFIER: String = "A001"
    var skipKeyInvalidationCheck: Boolean = false
    /**
     * Creates an appropriate wrapped key loader instance based on current feature flags.
     *
     * This method checks the [CommonFlight.ENABLE_NEW_ANDROID_WRAPPED_KEY_LOADER] feature flag
     * to determine whether to use the new implementation or the legacy implementation of
     * the Android wrapped key loader.
     *
     * @param keyIdentifier A unique identifier for the key being loaded
     * @param fileName The name of the file where the wrapped key is stored
     * @param context The Android application context needed for file and security operations
     * @return An implementation of [ISecretKeyProvider] that can load the specified wrapped key
     */
    fun createWrappedKeyLoader(
        keyIdentifier: String,
        fileName: String,
        context: android.content.Context
    ): ISecretKeyProvider {
        val useNewAndroidWrappedKeyLoader =
            CommonFlightsManager
                .getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_NEW_ANDROID_WRAPPED_KEY_LOADER)

        return if (useNewAndroidWrappedKeyLoader) {
            NewAndroidWrappedKeyProvider(
                keyIdentifier,
                fileName,
                context
            )
        } else {
            AndroidWrappedKeyProvider(
                keyIdentifier,
                fileName,
                context
            )
        }
    }
}
