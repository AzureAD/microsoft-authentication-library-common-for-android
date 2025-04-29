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

import com.microsoft.identity.common.internal.broker.IRestrictionsManager.BrokerRestrictionsManagerKeys.PREFERRED_AUTH_CONFIG
import com.microsoft.identity.common.internal.broker.IRestrictionsManager.BrokerRestrictionsManagerKeys.SUPPRESS_CAMERA_CONSENT
import com.microsoft.identity.common.logging.Logger

/**
 * Manages the SDM QR PIN mode settings for the device.
 *
 * This object contains functions to check"
 * - Whether camera consent should be suppressed.
 * - The preferred authentication method for the device.
 *
 * It is initialized each time `brokerAndroidBrokerPlatformComponentsFactory.create` is called.
 *
 * Note: If this object is not initialized, values default to `false` and `null`,
 * meaning the device is not in SDM QR PIN mode.
 */
object SdmQrPinManager {

    private const val TAG = "SdmQrPinManager"

    private var restrictionsManager: IRestrictionsManager? = null

    /**
     * This method is used to initialize the SDM QR PIN manager with the broker restrictions manager.
     * This is called when brokerAndroidBrokerPlatformComponentsFactory.create is called
     * to ensure the manager is initialized with the correct broker restrictions manager.
     */
    fun initializeSdmQrPinManager(restrictionsManager: IRestrictionsManager) {
        if (this.restrictionsManager == null) {
            this.restrictionsManager = restrictionsManager
        }
    }

    /**
     * This method checks if the device preferred authentication config is set.
     */
    fun getPreferredAuthConfig(): String? {
        val methodTag = "$TAG:getPreferredAuthConfig"
        val defaultValue = null
        restrictionsManager?.let { manager ->
            return manager.getString(
                key = PREFERRED_AUTH_CONFIG,
                brokerAppPackageName = BrokerData.prodMicrosoftAuthenticator.packageName,
                defaultValue = defaultValue
            )
        } ?: run {
            Logger.warn(methodTag, "Broker restrictions manager is not initialized.")
            return defaultValue
        }
    }

    /**
     * This method checks if the camera consent is suppressed.
     */
    fun isCameraConsentSuppressed(): Boolean {
        val methodTag = "$TAG:isCameraConsentSuppressed"
        val defaultValue = false
        restrictionsManager?.let { manager ->
            return manager.getBoolean(
                key = SUPPRESS_CAMERA_CONSENT,
                brokerAppPackageName = BrokerData.prodMicrosoftAuthenticator.packageName,
                defaultValue = defaultValue
            )
        } ?: run {
            Logger.warn(methodTag, "Broker restrictions manager is not initialized.")
            return defaultValue
        }
    }
}
