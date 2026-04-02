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

import android.os.Bundle;

import com.microsoft.identity.deviceregistration.java.exception.ApiUpdateRequiredException;
import com.microsoft.identity.deviceregistration.java.exception.BrokerUpdateRequiredException;
import com.microsoft.identity.deviceregistration.java.exception.DeviceRegistrationException;
import com.microsoft.identity.deviceregistration.java.exception.DrsErrorResponseException;
import com.microsoft.identity.deviceregistration.java.protocol.IDeviceRegistrationProtocol;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolPacker;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.IErrorInformation;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ThrowableUtil;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import lombok.NonNull;

/**
 * Android-specific packer for the device registration protocol.
 * Packs/unpacks {@link IDeviceRegistrationProtocol} objects into/from {@link Bundle}.
 */
public class AndroidDeviceRegistrationProtocolPacker implements IDeviceRegistrationProtocolPacker<Bundle> {

    private static final String TAG = AndroidDeviceRegistrationProtocolPacker.class.getSimpleName();

    // Bundle keys — must match the broker-side packer for wire compatibility.
    public static final String PROTOCOL_DATA = "protocol.data";
    public static final String PROTOCOL_NAME = "protocol.name";
    public static final String CORRELATION_ID = "correlation.id";
    public static final String PROTOCOL_EXCEPTION_ERROR_CODE = "protocol.exception.error.code";
    public static final String PROTOCOL_EXCEPTION_ERROR_MESSAGE = "protocol.exception.error.message";
    public static final String PROTOCOL_EXCEPTION_ERROR_CAUSE_MESSAGE = "protocol.exception.error.cause";
    public static final String PROTOCOL_EXCEPTION_STACK_TRACE_STRING = "protocol.exception.stack.trace.string";
    private static final String PROTOCOL_EXCEPTION_CALLING_PACKAGE_NAME = "protocol.exception.api.to.update";
    private static final String PROTOCOL_EXCEPTION_BROKER_PACKAGE_NAME = "protocol.exception.broker.to.update";

    @NonNull
    @Override
    public final Bundle pack(@NonNull final IDeviceRegistrationProtocol protocol) {
        final Bundle bundle = new Bundle();
        bundle.putString(PROTOCOL_NAME, protocol.getProtocolName());
        bundle.putByteArray(PROTOCOL_DATA, protocol.serialize());
        if (protocol.getCorrelationId() != null) {
            bundle.putString(CORRELATION_ID, protocol.getCorrelationId().toString());
        }
        return bundle;
    }

    @NonNull
    @Override
    public final String unpackName(@NonNull final Bundle packedData) throws DeviceRegistrationException {
        final String methodTag = TAG + ":unpackName";
        final String name = packedData.getString(PROTOCOL_NAME, null);
        if (StringUtil.isNullOrEmpty(name)) {
            final String errorMessage = "Bundle does not contain " + PROTOCOL_NAME + " key or is empty.";
            Logger.error(methodTag, errorMessage, null);
            throw new DeviceRegistrationException(
                    DeviceRegistrationException.INVALID_PACKAGE_ERROR_CODE, errorMessage);
        }
        return name;
    }

    @NonNull
    @Override
    public final String unpackCorrelationId(@NonNull final Bundle packedData) {
        final String methodTag = TAG + ":unpackCorrelationId";
        final String correlationId = packedData.getString(CORRELATION_ID, null);
        if (StringUtil.isNullOrEmpty(correlationId)) {
            final String warningMessage = "Bundle does not contain " + CORRELATION_ID +
                    " key or is empty. Generating a new one";
            Logger.warn(methodTag, warningMessage);
            return UUID.randomUUID().toString();
        }
        return correlationId;
    }

    @Override
    public final byte[] unpackData(@NonNull final Bundle packedData) throws BaseException {
        final String methodTag = TAG + ":unpackData";
        throwIfBundleContainsException(packedData);
        final byte[] data = packedData.getByteArray(PROTOCOL_DATA);
        if (data == null || data.length == 0) {
            final String errorMessage = "Bundle does not contain " + PROTOCOL_DATA + " key or is empty.";
            Logger.error(methodTag, errorMessage, null);
            throw new DeviceRegistrationException(
                    DeviceRegistrationException.INVALID_PACKAGE_ERROR_CODE, errorMessage);
        }
        Logger.info(methodTag, "Data unpacked successfully");
        return data;
    }

    @NonNull
    @Override
    public final Bundle packThrowable(@NonNull final Throwable throwable) {
        final Bundle bundle = new Bundle();
        final Throwable rootCause;
        if (throwable instanceof ExecutionException && throwable.getCause() != null) {
            rootCause = throwable.getCause();
        } else {
            rootCause = throwable;
        }
        final String errorCode;
        if (rootCause instanceof IErrorInformation) {
            errorCode = ((IErrorInformation) rootCause).getErrorCode();
        } else {
            errorCode = ClientException.UNKNOWN_ERROR;
        }
        bundle.putString(PROTOCOL_EXCEPTION_ERROR_CODE, errorCode);
        bundle.putString(PROTOCOL_EXCEPTION_ERROR_MESSAGE, rootCause.getMessage());
        if (rootCause.getCause() != null) {
            bundle.putString(PROTOCOL_EXCEPTION_ERROR_CAUSE_MESSAGE, rootCause.getCause().getMessage());
        }
        final String stackTraceString = ThrowableUtil.getStackTraceAsString(rootCause);
        bundle.putString(PROTOCOL_EXCEPTION_STACK_TRACE_STRING, stackTraceString);
        if (rootCause instanceof ApiUpdateRequiredException) {
            bundle.putString(
                    PROTOCOL_EXCEPTION_CALLING_PACKAGE_NAME,
                    ((ApiUpdateRequiredException) rootCause).getCallingPackageName());
        }
        if (rootCause instanceof BrokerUpdateRequiredException) {
            bundle.putString(
                    PROTOCOL_EXCEPTION_BROKER_PACKAGE_NAME,
                    ((BrokerUpdateRequiredException) rootCause).getBrokerPackageName());
        }
        return bundle;
    }

    /**
     * Throws a {@link DeviceRegistrationException} if the bundle contains a packed exception.
     */
    private void throwIfBundleContainsException(@NonNull final Bundle protocolBundle)
            throws DeviceRegistrationException, DrsErrorResponseException {
        final String methodTag = TAG + ":throwIfBundleContainsException";
        final String errorCode = protocolBundle.getString(PROTOCOL_EXCEPTION_ERROR_CODE, null);
        if (StringUtil.isNullOrEmpty(errorCode)) {
            return;
        }
        final String errorMessage = protocolBundle.getString(PROTOCOL_EXCEPTION_ERROR_MESSAGE, errorCode);
        final String causeMessage = protocolBundle.getString(PROTOCOL_EXCEPTION_ERROR_CAUSE_MESSAGE);
        final String stackTraceString = protocolBundle.getString(PROTOCOL_EXCEPTION_STACK_TRACE_STRING);
        final Throwable cause;
        if (StringUtil.isNullOrEmpty(causeMessage)) {
            cause = null;
        } else {
            cause = new Exception(causeMessage);
        }
        Logger.info(methodTag, "Bundle contains exception " + errorCode);
        if (DeviceRegistrationException.UPDATE_API_ERROR_CODE.equalsIgnoreCase(errorCode)) {
            final String callingPackageName = protocolBundle.getString(PROTOCOL_EXCEPTION_CALLING_PACKAGE_NAME);
            throw new ApiUpdateRequiredException(callingPackageName, cause);
        }
        if (DeviceRegistrationException.UPDATE_BROKER_ERROR_CODE.equalsIgnoreCase(errorCode)) {
            final String brokerPackageName = protocolBundle.getString(PROTOCOL_EXCEPTION_BROKER_PACKAGE_NAME);
            throw new BrokerUpdateRequiredException(brokerPackageName, cause);
        }
        final DrsErrorResponseException drsException =
                DrsErrorResponseException.getFromErrorResponse(errorMessage);
        if (!DrsErrorResponseException.DEFAULT_ERROR_CODE.equalsIgnoreCase(drsException.getErrorCode())) {
            throw drsException;
        }
        throw new DeviceRegistrationException(errorCode, errorMessage, cause, stackTraceString);
    }
}
