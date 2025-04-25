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

import com.microsoft.identity.common.internal.broker.IBrokerRestrictionsManager.BrokerRestrictionsManagerKeys.PREFERRED_AUTH_CONFIG
import com.microsoft.identity.common.internal.broker.IBrokerRestrictionsManager.BrokerRestrictionsManagerKeys.SUPPRESS_CAMERA_CONSENT
import com.microsoft.identity.common.internal.broker.IBrokerRestrictionsManager.BrokerRestrictionsManagerKeys.buildMultiValueRequest
import com.microsoft.identity.common.java.ui.PreferredAuthMethod
import com.microsoft.identity.common.logging.Logger

/**
 * Manages the SDM QR PIN mode settings for the device.
 *
 * This object contains flags that indicate:
 * - Whether the device is currently in SDM QR PIN mode.
 * - Whether camera consent should be suppressed.
 * - The preferred authentication method for the device.
 *
 * It is initialized each time `GetPreferredAuthMethodMsalBrokerOperation` is called,
 * since apps always contact the broker to get the preferred authentication method
 * before starting the authentication flow.
 *
 * Note: If this object is not initialized, both flags default to `false`,
 * meaning the device is not in SDM QR PIN mode.
 */
object SdmQrPinManager {

    private const val TAG = "SdmQrPinManager"

    /**
     * This is the preferred authentication method for the device on SDM mode.
     * This is set in the Authenticator app, using app configuration policies for managed Android Enterprise devices.
     */
    var preferredAuthMethod: String? = null

    /**
     * This is a flag to indicate if the device is on SDM QR PIN mode.
     * If this is true, the device should prompt the user for camera consent.
     */
    var isDeviceOnSdmQrPinAuth = false
        private set

    /**
     * This is a flag to indicate if the camera consent should be suppressed.
     * If this is true, the device should not prompt the user for camera consent.
     */
    var isCameraConsentSuppressed = false
        private set

    /**
     * This variable is used to store the last update time of the SDM QR PIN manager.
     */
    private var lastUpdateTime: Long = 0L

    /**
     * This method initializes the SDM QR PIN manager by checking the broker restrictions manager.
     * It sets the [isDeviceOnSdmQrPinAuth], [preferredAuthMethod] and [isCameraConsentSuppressed] flags.
     * This method is called each time `GetPreferredAuthMethodMsalBrokerOperation` is called,
     * because the restrictions rarely change, only when a new policy is pushed to the device.
     *
     * @param brokerRestrictionsManager The broker restrictions manager to check the restrictions.
     */
    fun initializeSdmQrPinManager(brokerRestrictionsManager: IBrokerRestrictionsManager) {
        Logger.info(TAG, "Initializing SDM QR PIN manager.")
        val multiValueRequest = buildMultiValueRequest(
            booleanKeys = setOf(SUPPRESS_CAMERA_CONSENT),
            stringKeys = setOf(PREFERRED_AUTH_CONFIG)
        )
        val multiValues = brokerRestrictionsManager.getMultiValues(
            brokerAppPackageName = BrokerData.prodMicrosoftAuthenticator.packageName,
            bundleOfKeys = multiValueRequest
        )
        preferredAuthMethod = multiValues.getString(PREFERRED_AUTH_CONFIG)
        if (preferredAuthMethod == PreferredAuthMethod.QR.value) {
            isDeviceOnSdmQrPinAuth = true
        }
        isCameraConsentSuppressed = multiValues.getBoolean(SUPPRESS_CAMERA_CONSENT)
        Logger.info(
            TAG,
            "preferredAuthMethod: $preferredAuthMethod," +
                    " isCameraConsentSuppressed: $isCameraConsentSuppressed"
        )
    }
}
