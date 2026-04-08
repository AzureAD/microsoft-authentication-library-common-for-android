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
package com.microsoft.identity.deviceregistration;

import static com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation.DEVICE_REGISTRATION_OPERATIONS;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.BOUND_SERVICE;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.CONTENT_PROVIDER;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_BROKER_BUNDLE;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClient;
import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdater;
import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationClientController;
import com.microsoft.identity.deviceregistration.java.exception.DeviceRegistrationException;
import com.microsoft.identity.deviceregistration.java.protocol.parameters.IDeviceRegistrationProtocolParameters;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.logging.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;

/**
 * Android specific device registration controller to communicate protocols to the broker.
 * Moved from AADAuthenticator to common module to allow OneAuth consumers to use it.
 */
public class AndroidDeviceRegistrationClientController implements IDeviceRegistrationClientController {
    private static final String TAG = AndroidDeviceRegistrationClientController.class.getSimpleName();

    @NonNull
    private final String mActiveBrokerPackageName;
    @NonNull
    private final List<IIpcStrategy> mIpcStrategies;
    @NonNull
    private static final AndroidDeviceRegistrationProtocolPacker mProtocolPacker
            = new AndroidDeviceRegistrationProtocolPacker();

    private final boolean mSupportsBoundService;

    @NonNull
    private final ActiveBrokerCacheUpdater mCacheUpdater;

    @NonNull
    private final String mCallerPackageName;

    /**
     * Creates a new controller.
     *
     * @param context              application context.
     * @param components           platform components.
     * @param discoveryClient      broker discovery client for resolving active broker.
     * @param strategiesProvider   provides the IPC strategies and supportsBoundService flag.
     * @param cacheUpdater         cache updater for active broker cache.
     */
    public AndroidDeviceRegistrationClientController(
            @NonNull final Context context,
            @NonNull final IPlatformComponents components,
            @NonNull final IBrokerDiscoveryClient discoveryClient,
            @NonNull final DeviceRegistrationIpcStrategiesProvider strategiesProvider,
            @NonNull final ActiveBrokerCacheUpdater cacheUpdater
    ) throws ClientException {
        mCallerPackageName = context.getPackageName();
        mActiveBrokerPackageName = getActiveBrokerPackageName(discoveryClient);
        mIpcStrategies = strategiesProvider.getStrategies(context, components, mActiveBrokerPackageName);
        mCacheUpdater = cacheUpdater;
        mSupportsBoundService = strategiesProvider.getSupportsBoundService();
    }

    private static String getActiveBrokerPackageName(
            @NonNull final IBrokerDiscoveryClient discoveryClient) throws ClientException {
        final BrokerData activeBroker = discoveryClient.getActiveBroker(false);
        if (activeBroker == null) {
            throw new ClientException(ClientException.NOT_VALID_BROKER_FOUND,
                    "Broker should not be null when invoked from Broker API.");
        }
        return activeBroker.getPackageName();
    }

    /**
     * Communicates the protocol associated with the given parameters using the available strategies.
     *
     * @param protocolParameters protocol parameters to execute.
     * @return a serialized protocol response.
     * @throws BaseException if all strategies to execute the protocol fail.
     */
    @Override
    @NonNull
    public final byte[] execute(@NonNull final IDeviceRegistrationProtocolParameters protocolParameters)
            throws BaseException {
        final String methodTag = TAG + ":execute";

        if (mSupportsBoundService && (Thread.currentThread() == Looper.getMainLooper().getThread())) {
            throw new ClientException(ClientException.CALLED_ON_MAIN_THREAD,
                    protocolParameters.getProtocolName() + " must not be called from the main thread.");
        }

        final Queue<BrokerCommunicationException> communicationExceptionQueue = new LinkedList<>();
        final Span span = OTelUtility.createSpan(SpanName.DeviceRegistrationIpc.name());
        try (final Scope ignored = SpanExtension.makeCurrentSpan(span)) {
            span.setAttribute(AttributeName.device_registration_protocol_name.name(), protocolParameters.getProtocolName());
            span.setAttribute(AttributeName.calling_package_name.name(), mCallerPackageName);
            span.setAttribute(AttributeName.active_broker_package_name.name(), mActiveBrokerPackageName);
            for (final IIpcStrategy strategy : mIpcStrategies) {
                try {
                    byte[] protocolResult = communicateProtocolWithStrategy(protocolParameters, strategy);
                    setIpcStrategyTelemetryAttributes(strategy.getType(), "OK");
                    span.setStatus(StatusCode.OK);
                    return protocolResult;
                } catch (final BrokerCommunicationException communicationException) {
                    setIpcStrategyTelemetryAttributes(strategy.getType(), communicationException.getMessage());
                    // Fails to communicate to the broker. Try next strategy in list.
                    communicationExceptionQueue.add(communicationException);
                }
            }
            // If we reach this section We've tried all the strategies...
            Logger.error(methodTag, "All IPC strategies to communicate with the broker have failed.", null);
            for (final BrokerCommunicationException e : communicationExceptionQueue) {
                Logger.error(methodTag, e.getMessage(), e);
            }
            throw new DeviceRegistrationException(
                    DeviceRegistrationException.FAILED_TO_COMMUNICATE_WITH_BROKER_ERROR_CODE,
                    DeviceRegistrationException.FAILED_TO_COMMUNICATE_WITH_BROKER_ERROR_MESSAGE
            );
        } catch (final Throwable throwable) {
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR);
            throw throwable;
        } finally {
            span.end();
        }
    }

    /**
     * Communicates the protocol associated with the given parameters using the provided strategy.
     *
     * @param protocolParameters protocol parameters to execute.
     * @param ipcStrategy        strategy to be invoked.
     * @return a serialized protocol response.
     * @throws BrokerCommunicationException if the strategy fails.
     */
    @NonNull
    private byte[] communicateProtocolWithStrategy(
            @NonNull final IDeviceRegistrationProtocolParameters protocolParameters,
            @NonNull final IIpcStrategy ipcStrategy) throws BaseException {
        final String methodTag = TAG + ":executeProtocolWithStrategy";
        Logger.info(methodTag, "Executing " + protocolParameters.getProtocolName()
                + " with strategy: " + ipcStrategy.getType());
        final Bundle protocolParametersBundle;
        try {
            protocolParametersBundle = mProtocolPacker.pack(protocolParameters);
        } catch (final Throwable throwable) {
            Logger.error(methodTag, "Serialization error while packing the protocol", throwable);
            throw new DeviceRegistrationException(
                    DeviceRegistrationException.INTERNAL_ERROR_CODE,
                    DeviceRegistrationException.SERIALIZATION_ERROR_MESSAGE,
                    throwable
            );
        }
        final Bundle protocolResultBundle = ipcStrategy.communicateToBroker(
                new BrokerOperationBundle(
                        DEVICE_REGISTRATION_OPERATIONS,
                        mActiveBrokerPackageName,
                        protocolParametersBundle
                )
        );

        if (protocolResultBundle == null) {
            throw new ClientException(INVALID_BROKER_BUNDLE, "Broker Result not returned from Broker.");
        }

        mCacheUpdater.updateCachedActiveBrokerFromResultBundle(protocolResultBundle);

        return mProtocolPacker.unpackData(protocolResultBundle);
    }

    /**
     * Records IPC strategy telemetry attributes on the current span for device registration.
     * <p>
     * Maps the strategy type to its corresponding status attribute and sets a human-readable
     * status message. If no attribute is mapped for the strategy type, a warning is logged and no
     * attribute is set. Callers should avoid passing sensitive data in the status message.
     * </p>
     *
     * @param strategyType  {@link IIpcStrategy.Type} being evaluated.
     * @param statusMessage Status text describing the outcome; a fallback message is used when null.
     */
    private void setIpcStrategyTelemetryAttributes(
            @NonNull final IIpcStrategy.Type strategyType,
            @Nullable final String statusMessage) {
        final String methodTag = TAG + ":setIpcStrategyTelemetryAttributes";
        final String attributeName;
        if (CONTENT_PROVIDER.equals(strategyType)) {
            attributeName = AttributeName.content_provider_status.name();
        } else if (BOUND_SERVICE.equals(strategyType)) {
            attributeName = AttributeName.bound_service_status.name();
        } else {
            attributeName = null;
        }
        if (attributeName == null) {
            Logger.warn(methodTag, "No attribute name mapped for strategy: " + strategyType.name());
        } else {
            final String message = statusMessage == null ? "No status message" : statusMessage;
            SpanExtension.current().setAttribute(attributeName, message);
        }
    }
}
