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
 * Factory object for creating instances of [ISecretKeyProvider] implementations.
 *
 * This factory determines which type of secret key provider to create based on a feature flag.
 * The factory supports:
 * - **KeyStoreBackedSecretKeyProvider**: Modern implementation that supports enhanced security encryption paddings.
 * - **AndroidWrappedKeyProvider**: Legacy fallback implementation for backward compatibility
 *
 */
object KeyStoreBackedSecretKeyProviderFactory {

    /**
     * Controls whether key invalidation checks should be skipped during key loading operations.
     *
     * When set to `true`, the key providers will skip validation checks for KeyStore and key file
     * integrity before every key load operation. This can improve performance but may reduce
     * security guarantees.
     *
     * Default: `false` (key invalidation checks are performed)
     */
    @JvmField
    var skipKeyInvalidationCheck: Boolean = false

    /**
     * Creates an appropriate [ISecretKeyProvider] instance based on current feature flag settings.
     *
     * This method selects between different key provider implementations based on the
     * [CommonFlight.ENABLE_KEYSTORE_BACKED_SECRET_KEY_PROVIDER] feature flag:
     *
     * @param keyIdentifier A unique identifier for the key, typically used as the Android KeyStore alias.
     *                     This should be unique within the application to avoid key collisions.
     * @param fileName The filename where the wrapped key data will be stored. This file will be
     *                created in the application's private directory and should be unique per key.
     * @param context The Android application context, used for accessing the KeyStore and
     *               creating private storage directories.
     * @return An [ISecretKeyProvider] instance configured with the specified parameters.
     *
     * @throws IllegalArgumentException if any of the parameters are invalid
     * @throws SecurityException if the application lacks necessary permissions for KeyStore access
     *
     * @see ISecretKeyProvider.key
     * @see KeyStoreBackedSecretKeyProvider
     * @see AndroidWrappedKeyProvider
     */
    fun create(
        keyIdentifier: String,
        fileName: String,
        context: android.content.Context
    ): ISecretKeyProvider {
        val enableKeyStoreBackedSecretKeyProvider =
            CommonFlightsManager
                .getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_KEYSTORE_BACKED_SECRET_KEY_PROVIDER)

        return if (enableKeyStoreBackedSecretKeyProvider) {
            KeyStoreBackedSecretKeyProvider(
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
