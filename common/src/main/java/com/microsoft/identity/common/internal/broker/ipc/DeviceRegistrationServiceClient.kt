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
package com.microsoft.identity.common.internal.broker.ipc

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import com.microsoft.identity.client.IDeviceRegistrationService
import com.microsoft.identity.common.internal.broker.BoundServiceClient

/**
 * A client for communicating with the DeviceRegistrationService via IPC.
 * This client binds to the service and allows executing device registration protocol operations with the broker.
 *
 * @param context the application context used to bind to the service.
 */
class DeviceRegistrationServiceClient(context: Context) :
    BoundServiceClient<IDeviceRegistrationService>(
        context,
        SERVICE_CLASS_NAME,
        SERVICE_INTENT_FILTER
    ) {
    companion object {
        /** The fully qualified class name of the DeviceRegistrationService to bind to. */
        private const val SERVICE_CLASS_NAME = "com.microsoft.identity.client.DeviceRegistrationService"

        /** The intent filter used to identify the DeviceRegistrationService. */
        private const val SERVICE_INTENT_FILTER = "com.microsoft.identity.client.DeviceRegistration"
    }

    /**
     * Extracts the [IDeviceRegistrationService] AIDL interface from the given [IBinder].
     *
     * @param binder the [IBinder] returned by the service connection.
     * @return the [IDeviceRegistrationService] interface for communicating with the service.
     */
    override fun getInterfaceFromIBinder(binder: IBinder): IDeviceRegistrationService =
        IDeviceRegistrationService.Stub.asInterface(binder)

    /**
     * Executes the device registration protocol operation by delegating to the AIDL interface.
     *
     * @param inputBundle the [BrokerOperationBundle] containing the operation parameters.
     * @param aidlInterface the [IDeviceRegistrationService] AIDL interface bound to the service.
     * @return a [Bundle] containing the result of the device registration protocol, or null if no result.
     */
    override fun performOperationInternal(
        inputBundle: BrokerOperationBundle,
        aidlInterface: IDeviceRegistrationService
    ): Bundle? = aidlInterface.executeDeviceRegistrationProtocol(inputBundle.bundle)
}
