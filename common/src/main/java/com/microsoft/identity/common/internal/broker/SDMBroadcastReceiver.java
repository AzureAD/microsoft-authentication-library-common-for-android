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
package com.microsoft.identity.common.internal.broker;

import static com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.activebrokerdiscovery.BrokerDiscoveryClientFactory;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.java.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.java.cache.IAccountCredentialCache;
import com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.constants.SharedDeviceModeConstants;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.logging.Logger;

import java.util.UUID;

/**
 * Broadcast receiver listening for Shared device mode broadcasts from broker.
 */
public class SDMBroadcastReceiver {
    private static final String TAG = SDMBroadcastReceiver.class.getSimpleName();
    private static BroadcastReceiver sSDMBroadcastReceiver;

    /**
     * Initializes the SDM broadcast receiver to start listening for SDM broadcasts from broker
     * @param context application context.
     * @param sharedDeviceModeCallback a callback to be called when SDM broadcast is received.
     */
    synchronized public static void initialize( @NonNull final Context context,
                                   @NonNull final SharedDeviceModeCallback sharedDeviceModeCallback) {
        if (sSDMBroadcastReceiver == null) {
            sSDMBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    handleSharedDeviceModeBroadCast(context, intent, sharedDeviceModeCallback);
                }
            };

            final IntentFilter filter = new IntentFilter(SharedDeviceModeConstants.CURRENT_ACCOUNT_CHANGED_BROADCAST_IDENTIFIER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(sSDMBroadcastReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(sSDMBroadcastReceiver, filter);
            }
        }
    }

    /**
     * Handles the SDM broadcast and calls the callback method based on the broadcast type
     * @param context application context.
     * @param intent The receive intent for SDM broadcast.
     * @param sharedDeviceModeCallback Callback to be called.
     */
    private static void handleSharedDeviceModeBroadCast(@NonNull final Context context,
                                                        @NonNull final Intent intent,
                                                        @NonNull SharedDeviceModeCallback sharedDeviceModeCallback) {
        final String methodTag = TAG + ":handleSharedDeviceModeBroadCast";
        final String broadcastType = intent.getStringExtra(SharedDeviceModeConstants.BROADCAST_TYPE_KEY);
        Logger.info(methodTag, "Received SDM broadcast with type: " + broadcastType);
        try {
            final IPlatformComponents platformComponents = AndroidPlatformComponentsFactory.createFromContext(context);
            if (broadcastType == null) {
                Logger.warn(methodTag, "ignoring null broadcast type ");
            } else {
                switch (broadcastType) {
                    case SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTRATION_START:
                        sharedDeviceModeCallback.onSharedDeviceModeRegistrationStarted();
                        break;
                    case SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTERED:
                        if (isDeviceInSharedMode(context, platformComponents)) {
                            Logger.info(methodTag, "Device is registered in SDM, clearing default account cache.");
                            final IAccountCredentialCache accountCredentialCache = new SharedPreferencesAccountCredentialCache(
                                    new CacheKeyValueDelegate(),
                                    platformComponents.getStorageSupplier().getEncryptedNameValueStore(
                                            DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES, String.class)
                            );
                            accountCredentialCache.clearAll();
                            sharedDeviceModeCallback.onSharedDeviceModeRegistered();
                        } else {
                            Logger.warn(methodTag, "Device not in shared device mode, ignore broadcast.");
                        }
                        break;
                    case SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT:
                        sharedDeviceModeCallback.onGlobalSignOut();
                        break;
                    default:
                        Logger.warn(methodTag, "ignoring unknown broadcast type " + broadcastType);
                        break;
                }
            }
        } catch (final BaseException e) {
            Logger.error(methodTag, "Failed to handle SDM broadcast", e);
        }
    }

    private static boolean isDeviceInSharedMode(@NonNull final Context context,
                                                @NonNull IPlatformComponents platformComponents) throws BaseException {
        final BrokerData activeBroker = BrokerDiscoveryClientFactory.getInstanceForBrokerSdk(context, platformComponents)
                .getActiveBroker(false);
        if (activeBroker == null) {
            return  false;
        }
        final BrokerMsalController brokerMsalController = new BrokerMsalController(context, platformComponents, activeBroker.getPackageName());
        final CommandParameters commandParameters;
        commandParameters = CommandParameters.builder()
                .platformComponents(platformComponents)
                .correlationId(UUID.randomUUID().toString())
                .build();
        return brokerMsalController.getDeviceMode(commandParameters);
    }

    /**
     * Callback for SDM broadcasts
     */
    public interface SharedDeviceModeCallback {
        /**
         * Called when shared device mode registration is initiated.
         */
        void onSharedDeviceModeRegistrationStarted();

        /**
         * Called when device is registered in shared device mode.
         */
        void onSharedDeviceModeRegistered();

        /**
         * Called when global sign out occurs.
         */
        void onGlobalSignOut();
    }
}
