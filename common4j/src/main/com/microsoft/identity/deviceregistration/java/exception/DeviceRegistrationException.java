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
package com.microsoft.identity.deviceregistration.java.exception;

import com.microsoft.identity.common.java.exception.BaseException;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Base class for all device registration exceptions.
 */
@Accessors(prefix = "m")
@Getter
public class DeviceRegistrationException extends BaseException {
    private static final String TAG = DeviceRegistrationException.class.getSimpleName();

    @Nullable
    private final String mStackTraceString;

    /**
     * Device registration error codes
     **/
    // Thrown to indicate that the active broker needs to be updated.
    public static final String UPDATE_BROKER_ERROR_CODE = "update_broker";

    // Thrown to indicate the device registration API needs to be updated.
    public static final String UPDATE_API_ERROR_CODE = "update_device_registration_api";

    // Thrown when an unexpected error occurs during the execution of a device registration protocol.
    public static final String INTERNAL_ERROR_CODE = "internal_error";

    // Thrown when invalid package is provided to the protocol packer
    public static final String INVALID_PACKAGE_ERROR_CODE = "invalid_package";

    // Thrown when the API failed to communicate with the broker.
    public static final String FAILED_TO_COMMUNICATE_WITH_BROKER_ERROR_CODE = "failed_to_communicate_with_broker";

    // Thrown when the expected device registration entry is null.
    public static final String NO_SUCH_DEVICE_REGISTRATION_ERROR_CODE = "no_such_device_registration";

    // Thrown to indicate that the proportionate device registration record
    // does not match with the returned record in the fallback strategy.
    public static final String NOT_MATCHING_RECORD_FOUND_ERROR_CODE = "no_matching_record_found";

    /**
     * Device registration messages
     **/
    public static final String FAILED_TO_COMMUNICATE_WITH_BROKER_ERROR_MESSAGE
            = "All the strategies to communicate with the broker have failed.";

    public static final String SERIALIZATION_ERROR_MESSAGE
        = "The protocol can not be serialized ";

    public static final String NO_SUCH_DEVICE_REGISTRATION_ERROR_MESSAGE
            = "The device registration record requested does not exist";

    public static final String NOT_MATCHING_RECORD_FOUND_ERROR_MESSAGE
            = "A record matching the proportionate device id was not found";

    public DeviceRegistrationException(@NonNull final String deviceRegistrationErrorCode,
                                       @NonNull final String errorMessage) {
        super(deviceRegistrationErrorCode, errorMessage);
        mStackTraceString = null;
    }

    public DeviceRegistrationException(@NonNull final String deviceRegistrationErrorCode,
                                       @NonNull final String errorMessage,
                                       @Nullable final Throwable cause) {
        super(deviceRegistrationErrorCode, errorMessage, cause);
        mStackTraceString = null;
    }

    public DeviceRegistrationException(@NonNull final String deviceRegistrationErrorCode,
                                       @NonNull final String errorMessage,
                                       @Nullable final Throwable cause,
                                       @Nullable final String stackTraceString) {
        super(deviceRegistrationErrorCode, errorMessage, cause);
        mStackTraceString = stackTraceString;
    }

    /**
     * Return a BrokerUpdateRequiredException.
     *
     * @param brokerPackageName The package name of the active broker.
     * @param cause             The {@link Throwable} contains the cause for the exception.
     * @return BrokerUpdateRequiredException
     */
    public static BrokerUpdateRequiredException getBrokerUpdateNeededException(
            @NonNull final String brokerPackageName,
            @Nullable final Throwable cause) {
        return new BrokerUpdateRequiredException(brokerPackageName, cause);
    }

    /**
     * Return a ApiUpdateRequiredException.
     *
     * @param callingPackageName The package name of app calling the device registration API.
     * @param cause              The {@link Throwable} contains the cause for the exception.
     * @return ApiUpdateRequiredException
     */
    public static ApiUpdateRequiredException getApiUpdateNeededException(
            @NonNull final String callingPackageName,
            @Nullable final Throwable cause) {
        return new ApiUpdateRequiredException(callingPackageName, cause);
    }
}
