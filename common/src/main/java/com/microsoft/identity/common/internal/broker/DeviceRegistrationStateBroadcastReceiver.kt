//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.broker

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.microsoft.identity.common.java.constants.DeviceRegistrationBroadcastConstants
import com.microsoft.identity.common.logging.Logger

/**
 * Broadcast receiver listening for device registration state change broadcasts from Broker.
 */
class DeviceRegistrationStateBroadcastReceiver {

    /**
     * Callback interface for device registration state change broadcasts.
     */
    interface DeviceRegistrationStateCallback {
        /**
         * Called when the device has been registered (WPJ).
         */
        fun onDeviceRegistered()

        /**
         * Called when the device has been unregistered (WPJ leave).
         */
        fun onDeviceUnregistered()

        /**
         * Called when the device registration has been upgraded.
         */
        fun onDeviceRegistrationUpgraded()

        /**
         * Called when a general device registration state change occurs.
         */
        fun onDeviceStateChanged()
    }

    companion object {
        private val TAG = DeviceRegistrationStateBroadcastReceiver::class.java.simpleName
        private var sReceiver: BroadcastReceiver? = null

        /**
         * Initializes the device registration state broadcast receiver to start listening
         * for device registration broadcasts from Broker.
         *
         * @param context  application context.
         * @param callback a callback to be invoked when a device registration broadcast is received.
         */
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        @Synchronized
        fun initialize(context: Context, callback: DeviceRegistrationStateCallback) {
            if (sReceiver == null) {
                sReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        handleBroadcast(intent, callback)
                    }
                }

                val filter = IntentFilter(DeviceRegistrationBroadcastConstants.DEVICE_REGISTRATION_STATE_CHANGED_BROADCAST_IDENTIFIER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(sReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(sReceiver, filter)
                }
            }
        }

        /**
         * Unregisters the device registration state broadcast receiver.
         *
         * @param context application context.
         */
        @Synchronized
        fun unregister(context: Context) {
            sReceiver?.let {
                context.unregisterReceiver(it)
                sReceiver = null
            }
        }

        private fun handleBroadcast(intent: Intent, callback: DeviceRegistrationStateCallback) {
            val methodTag = "$TAG:handleBroadcast"
            val broadcastType = intent.getStringExtra(DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_KEY)
            Logger.info(methodTag, "Received device registration broadcast with type: $broadcastType")
            when (broadcastType) {
                DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTERED ->
                    callback.onDeviceRegistered()
                DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_UNREGISTERED ->
                    callback.onDeviceUnregistered()
                DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTRATION_UPGRADED ->
                    callback.onDeviceRegistrationUpgraded()
                DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_STATE_CHANGED ->
                    callback.onDeviceStateChanged()
                else ->
                    Logger.warn(methodTag, "Ignoring unknown broadcast type: $broadcastType")
            }
        }
    }
}
