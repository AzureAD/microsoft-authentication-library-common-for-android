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
 * This object manages the SDM QR PIN mode for the device.
 * It contains flags to indicate if the device is on SDM QR PIN mode and if the camera consent should be suppressed.
 * This object is only initialized once in AndroidBrokerPlatformComponentsFactory
 * if this is not initialized, the default values are false, as the device is not on SDM QR PIN mode.
 */
object SdmQrPinManager {

    const val TAG = "SdmQrPinManager"

    /**
     * This is a flag to indicate if the device is on SDM QR PIN mode.
     * If this is true, the device should prompt the user for camera consent.
     */
    var isDeviceOnSdmQrPinAuth = false

    /**
     * This is a flag to indicate if the camera consent should be suppressed.
     * If this is true, the device should not prompt the user for camera consent.
     */
    var isCameraConsentSuppressed = false

    /**
     * This method initializes the SDM QR PIN manager by checking the broker restrictions manager.
     * It sets the [isDeviceOnSdmQrPinAuth] and [isCameraConsentSuppressed] flags.
     * This method should be called only in AndroidBrokerPlatformComponentsFactory.create
     * because the restrictions rarely change, only when a new policy is pushed to the device.
     *
     * @param brokerRestrictionsManager The broker restrictions manager to check the restrictions.
     */
    fun initializeSdmQrPinManager(brokerRestrictionsManager: IBrokerRestrictionsManager) {
        Logger.info(TAG, "Initializing SDM QR PIN manager.")
        val multiValueRequest  = buildMultiValueRequest(
            booleanKeys = setOf(SUPPRESS_CAMERA_CONSENT),
            stringKeys = setOf(PREFERRED_AUTH_CONFIG)
        )
        val multiValues = brokerRestrictionsManager.getMultiValues(
            brokerAppPackageName =  BrokerData.prodMicrosoftAuthenticator.packageName,
            bundleOfKeys = multiValueRequest
        )
        val preferredAuthConfig  = multiValues.getString(PREFERRED_AUTH_CONFIG)
        if (preferredAuthConfig  != null && preferredAuthConfig  == PreferredAuthMethod.QR.value) {
            isDeviceOnSdmQrPinAuth = true
        }
        isCameraConsentSuppressed = multiValues.getBoolean(SUPPRESS_CAMERA_CONSENT)
        Logger.info(TAG, "isDeviceOnSdmQrPinAuth: $isDeviceOnSdmQrPinAuth, isCameraConsentSuppressed: $isCameraConsentSuppressed")
    }
}
