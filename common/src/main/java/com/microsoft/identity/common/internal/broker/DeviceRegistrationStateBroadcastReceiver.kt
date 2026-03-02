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
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.StatusCode

/**
 * Broadcast receiver listening for device registration state change broadcasts from the broker.
 */
object DeviceRegistrationStateBroadcastReceiver {

    private val TAG = DeviceRegistrationStateBroadcastReceiver::class.java.simpleName
    private var broadcastReceiver: BroadcastReceiver? = null

    /**
     * Callback interface for device registration state change events.
     */
    interface DeviceRegistrationStateCallback {
        /**
         * Called when the device has been registered.
         */
        fun onDeviceRegistered()

        /**
         * Called when the device has been unregistered.
         */
        fun onDeviceUnregistered()

        /**
         * Called when the device registration has been upgraded.
         */
        fun onDeviceRegistrationUpgraded()

        /**
         * Called when a general device state change has occurred.
         */
        fun onDeviceStateChanged()
    }

    /**
     * Initializes the device registration state broadcast receiver.
     * No-op if the [CommonFlight.ENABLE_DEVICE_REGISTRATION_STATE_BROADCAST] feature flag is disabled.
     *
     * @param context  application context.
     * @param callback callback to be invoked when a device registration state broadcast is received.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Synchronized
    @JvmStatic
    fun initialize(context: Context, callback: DeviceRegistrationStateCallback) {
        val methodTag = "$TAG:initialize"
        if (!CommonFlightsManager.INSTANCE.flightsProvider
                .isFlightEnabled(CommonFlight.ENABLE_DEVICE_REGISTRATION_STATE_BROADCAST)
        ) {
            Logger.info(methodTag, "Device registration state broadcast is disabled by feature flag.")
            return
        }

        if (broadcastReceiver == null) {
            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    handleBroadcast(intent, callback)
                }
            }

            val filter = IntentFilter(
                DeviceRegistrationBroadcastConstants.DEVICE_REGISTRATION_STATE_CHANGED_BROADCAST_IDENTIFIER
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(broadcastReceiver, filter)
            }
            Logger.info(methodTag, "Device registration state broadcast receiver registered.")
        }
    }

    /**
     * Unregisters the device registration state broadcast receiver.
     *
     * @param context application context.
     */
    @Synchronized
    @JvmStatic
    fun unregister(context: Context) {
        val methodTag = "$TAG:unregister"
        if (broadcastReceiver != null) {
            context.unregisterReceiver(broadcastReceiver)
            broadcastReceiver = null
            Logger.info(methodTag, "Device registration state broadcast receiver unregistered.")
        }
    }

    private fun handleBroadcast(intent: Intent, callback: DeviceRegistrationStateCallback) {
        val methodTag = "$TAG:handleBroadcast"
        val span = OTelUtility.createSpan(SpanName.DeviceRegistrationStateNotificationReceived.name())
        try {
            SpanExtension.makeCurrentSpan(span).use {
                val broadcastType = intent.getStringExtra(
                    DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_KEY
                )
                Logger.info(
                    methodTag,
                    "Received device registration state broadcast with type: $broadcastType"
                )
                when (broadcastType) {
                    DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTERED ->
                        callback.onDeviceRegistered()
                    DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_UNREGISTERED ->
                        callback.onDeviceUnregistered()
                    DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_REGISTRATION_UPGRADED ->
                        callback.onDeviceRegistrationUpgraded()
                    DeviceRegistrationBroadcastConstants.BROADCAST_TYPE_DEVICE_STATE_CHANGED ->
                        callback.onDeviceStateChanged()
                    null -> Logger.warn(methodTag, "Ignoring null broadcast type.")
                    else -> Logger.warn(methodTag, "Ignoring unknown broadcast type: $broadcastType")
                }
                span.setStatus(StatusCode.OK)
            }
        } catch (t: Throwable) {
            span.setStatus(StatusCode.ERROR)
            span.recordException(t)
            throw t
        } finally {
            span.end()
        }
    }
}
