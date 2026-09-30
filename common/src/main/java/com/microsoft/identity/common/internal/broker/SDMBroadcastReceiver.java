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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.activebrokerdiscovery.BrokerDiscoveryClientFactory;
import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClient;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.java.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.java.cache.IAccountCredentialCache;
import com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.constants.SharedDeviceModeConstants;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.logging.Logger;

import java.util.UUID;

/**
 * Broadcast receiver listening for Shared device mode broadcasts from broker.
 */
public class SDMBroadcastReceiver {
    private static final String TAG = SDMBroadcastReceiver.class.getSimpleName();
    private static BroadcastReceiver sSDMBroadcastReceiver;
    private static Context sRegisteredContext;
    private static volatile SharedDeviceModeCallback sSharedDeviceModeCallback;
    private static volatile DeviceModeProvider sDeviceModeProvider;
    private static RegistrationState sRegistrationState;
    private static long sInitializationGeneration;

    /**
     * Initializes the SDM broadcast receiver to start listening for SDM broadcasts from broker.
     * Broker discovery and capability checks run synchronously, so this method must be called from
     * a worker thread. If initialization fails, callers may invoke this method again.
     *
     * @param context application context.
     * @param sharedDeviceModeCallback a callback to be called when SDM broadcast is received.
     */
    @WorkerThread
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void initialize(@NonNull final Context context,
                                  @NonNull final SharedDeviceModeCallback sharedDeviceModeCallback) {
        final Context applicationContextCandidate = context.getApplicationContext();
        final Context applicationContext = applicationContextCandidate == null
                ? context
                : applicationContextCandidate;
        final IPlatformComponents platformComponents =
                AndroidPlatformComponentsFactory.createFromContext(applicationContext);
        final IBrokerDiscoveryClient brokerDiscoveryClient =
                BrokerDiscoveryClientFactory.getInstanceForClientSdk(
                        applicationContext,
                        platformComponents
                );
        initialize(
                applicationContext,
                sharedDeviceModeCallback,
                brokerDiscoveryClient,
                activeBroker -> BrokerDiscoveryClientFactory.isSdmBroadcastProtectionEnabled(
                        applicationContext,
                        platformComponents,
                        activeBroker
                ),
                new PackageHelper(applicationContext),
                SDMBroadcastReceiver::isDeviceInSharedMode
        );
    }

    @VisibleForTesting
    @WorkerThread
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    static void initialize(@NonNull final Context context,
                           @NonNull final SharedDeviceModeCallback sharedDeviceModeCallback,
                           @NonNull final IBrokerDiscoveryClient brokerDiscoveryClient,
                           @NonNull final BrokerCapabilityProvider brokerCapabilityProvider,
                           @NonNull final PackageHelper packageHelper,
                           @NonNull final DeviceModeProvider deviceModeProvider) {
        final String methodTag = TAG + ":initialize";
        final long initializationGeneration = beginInitialization();
        final RegistrationState registrationState;
        try {
            registrationState = resolveRegistrationState(
                    brokerDiscoveryClient,
                    brokerCapabilityProvider,
                    packageHelper
            );
        } catch (final ClientException e) {
            Logger.error(methodTag, "Unable to resolve SDM broadcast registration mode.", e);
            handleResolutionFailure(
                    initializationGeneration,
                    sharedDeviceModeCallback,
                    deviceModeProvider
            );
            return;
        }

        if (registrationState == null) {
            Logger.warn(methodTag, "No valid Broker is available for SDM broadcast registration.");
            handleResolutionFailure(
                    initializationGeneration,
                    sharedDeviceModeCallback,
                    deviceModeProvider
            );
            return;
        }

        synchronized (SDMBroadcastReceiver.class) {
            if (initializationGeneration != sInitializationGeneration) {
                return;
            }

            applyRegistrationState(
                    context,
                    sharedDeviceModeCallback,
                    deviceModeProvider,
                    registrationState
            );
        }
    }

    private static synchronized long beginInitialization() {
        return ++sInitializationGeneration;
    }

    private static synchronized void handleResolutionFailure(
            final long initializationGeneration,
            @NonNull final SharedDeviceModeCallback sharedDeviceModeCallback,
            @NonNull final DeviceModeProvider deviceModeProvider) {
        if (initializationGeneration != sInitializationGeneration) {
            return;
        }

        if (isCurrentRegistrationProtected()) {
            sSharedDeviceModeCallback = sharedDeviceModeCallback;
            sDeviceModeProvider = deviceModeProvider;
            Logger.warn(
                    TAG + ":initialize",
                    "Keeping the existing protected SDM broadcast receiver."
            );
            return;
        }

        clearRegistration();
    }

    private static void applyRegistrationState(
            @NonNull final Context context,
            @NonNull final SharedDeviceModeCallback sharedDeviceModeCallback,
            @NonNull final DeviceModeProvider deviceModeProvider,
            @NonNull final RegistrationState registrationState) {
        final String methodTag = TAG + ":initialize";
        if (registrationState.equals(sRegistrationState) && sSDMBroadcastReceiver != null) {
            sSharedDeviceModeCallback = sharedDeviceModeCallback;
            sDeviceModeProvider = deviceModeProvider;
            return;
        }

        final boolean preserveCurrentRegistration = isCurrentRegistrationProtected();
        final Context previousContext = preserveCurrentRegistration ? sRegisteredContext : null;
        final BroadcastReceiver previousReceiver =
                preserveCurrentRegistration ? sSDMBroadcastReceiver : null;
        if (preserveCurrentRegistration) {
            sSharedDeviceModeCallback = sharedDeviceModeCallback;
            sDeviceModeProvider = deviceModeProvider;
        } else {
            clearRegistration();
        }

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final SharedDeviceModeCallback callback = sSharedDeviceModeCallback;
                final DeviceModeProvider provider = sDeviceModeProvider;
                if (callback != null && provider != null) {
                    handleSharedDeviceModeBroadCast(context, intent, callback, provider);
                }
            }
        };
        final IntentFilter filter = new IntentFilter(
                SharedDeviceModeConstants.CURRENT_ACCOUNT_CHANGED_BROADCAST_IDENTIFIER
        );

        try {
            registerReceiver(
                    new ContextReceiverRegistrar(context),
                    receiver,
                    filter,
                    registrationState.mPermissionName,
                    Build.VERSION.SDK_INT
            );

            sRegisteredContext = context;
            sSharedDeviceModeCallback = sharedDeviceModeCallback;
            sDeviceModeProvider = deviceModeProvider;
            sRegistrationState = registrationState;
            sSDMBroadcastReceiver = receiver;
            unregisterReceiver(previousContext, previousReceiver);
            Logger.info(
                    methodTag,
                    registrationState.mPermissionName == null
                            ? "Registered SDM broadcast receiver in compatibility mode."
                            : "Registered protected SDM broadcast receiver."
            );
        } catch (final SecurityException | IllegalArgumentException e) {
            Logger.error(methodTag, "Failed to register SDM broadcast receiver.", e);
            if (!preserveCurrentRegistration) {
                clearRegistration();
            }
        }
    }

    @VisibleForTesting
    static void registerReceiver(@NonNull final ReceiverRegistrar receiverRegistrar,
                                 @NonNull final BroadcastReceiver receiver,
                                 @NonNull final IntentFilter filter,
                                 final String permissionName,
                                 final int sdkInt) {
        if (permissionName == null) {
            receiverRegistrar.registerLegacy(
                    receiver,
                    filter,
                    sdkInt >= Build.VERSION_CODES.TIRAMISU
            );
        } else {
            receiverRegistrar.registerProtected(
                    receiver,
                    filter,
                    permissionName,
                    sdkInt >= Build.VERSION_CODES.TIRAMISU
            );
        }
    }

    private static RegistrationState resolveRegistrationState(
            @NonNull final IBrokerDiscoveryClient brokerDiscoveryClient,
            @NonNull final BrokerCapabilityProvider brokerCapabilityProvider,
            @NonNull final PackageHelper packageHelper) throws ClientException {
        final BrokerData activeBroker = brokerDiscoveryClient.getActiveBroker(false);
        if (activeBroker == null) {
            return null;
        }

        if (!brokerCapabilityProvider.isSdmBroadcastProtectionEnabled(activeBroker)) {
            return new RegistrationState(activeBroker.getPackageName(), null);
        }

        final String permissionName = activeBroker.getPackageName()
                + SharedDeviceModeConstants.BROADCAST_PERMISSION_SUFFIX;
        if (!packageHelper.isSignaturePermissionGrantedToPackage(
                permissionName,
                activeBroker.getPackageName()
        )) {
            throw new ClientException(
                    ClientException.INVALID_BROKER_BUNDLE,
                    "The active Broker permission contract is invalid."
            );
        }

        return new RegistrationState(activeBroker.getPackageName(), permissionName);
    }

    private static void clearRegistration() {
        unregisterReceiver(sRegisteredContext, sSDMBroadcastReceiver);

        sSDMBroadcastReceiver = null;
        sRegisteredContext = null;
        sSharedDeviceModeCallback = null;
        sDeviceModeProvider = null;
        sRegistrationState = null;
    }

    private static void unregisterReceiver(final Context context,
                                           final BroadcastReceiver receiver) {
        if (context != null && receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (final IllegalArgumentException e) {
                Logger.warn(TAG + ":clearRegistration", "SDM broadcast receiver was not registered.");
            }
        }
    }

    private static boolean isCurrentRegistrationProtected() {
        return sSDMBroadcastReceiver != null
                && sRegistrationState != null
                && sRegistrationState.mPermissionName != null;
    }

    @VisibleForTesting
    synchronized static void resetForTest() {
        sInitializationGeneration++;
        clearRegistration();
    }

    @VisibleForTesting
    interface BrokerCapabilityProvider {
        boolean isSdmBroadcastProtectionEnabled(@NonNull BrokerData activeBroker)
                throws ClientException;
    }

    @VisibleForTesting
    interface ReceiverRegistrar {
        void registerLegacy(@NonNull BroadcastReceiver receiver,
                            @NonNull IntentFilter filter,
                            boolean exported);

        void registerProtected(@NonNull BroadcastReceiver receiver,
                               @NonNull IntentFilter filter,
                               @NonNull String permissionName,
                               boolean exported);
    }

    @VisibleForTesting
    interface DeviceModeProvider {
        boolean isDeviceInSharedMode(@NonNull Context context,
                                     @NonNull IPlatformComponents platformComponents)
                throws BaseException;
    }

    private static final class ContextReceiverRegistrar implements ReceiverRegistrar {
        private final Context mContext;

        private ContextReceiverRegistrar(@NonNull final Context context) {
            mContext = context;
        }

        @Override
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        public void registerLegacy(@NonNull final BroadcastReceiver receiver,
                                   @NonNull final IntentFilter filter,
                                   final boolean exported) {
            if (exported) {
                mContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                mContext.registerReceiver(receiver, filter);
            }
        }

        @Override
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        public void registerProtected(@NonNull final BroadcastReceiver receiver,
                                      @NonNull final IntentFilter filter,
                                      @NonNull final String permissionName,
                                      final boolean exported) {
            if (exported) {
                mContext.registerReceiver(
                        receiver,
                        filter,
                        permissionName,
                        null,
                        Context.RECEIVER_EXPORTED
                );
            } else {
                mContext.registerReceiver(receiver, filter, permissionName, null);
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
                                                        @NonNull SharedDeviceModeCallback sharedDeviceModeCallback,
                                                        @NonNull DeviceModeProvider deviceModeProvider) {
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
                        if (deviceModeProvider.isDeviceInSharedMode(
                                context,
                                platformComponents
                        )) {
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

    private static final class RegistrationState {
        private final String mBrokerPackageName;
        private final String mPermissionName;

        private RegistrationState(@NonNull final String brokerPackageName,
                                  final String permissionName) {
            mBrokerPackageName = brokerPackageName;
            mPermissionName = permissionName;
        }

        @Override
        public boolean equals(final Object object) {
            if (this == object) {
                return true;
            }

            if (!(object instanceof RegistrationState)) {
                return false;
            }

            final RegistrationState other = (RegistrationState) object;
            return mBrokerPackageName.equals(other.mBrokerPackageName)
                    && (mPermissionName == null
                    ? other.mPermissionName == null
                    : mPermissionName.equals(other.mPermissionName));
        }

        @Override
        public int hashCode() {
            int result = mBrokerPackageName.hashCode();
            result = 31 * result + (mPermissionName == null ? 0 : mPermissionName.hashCode());
            return result;
        }
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
