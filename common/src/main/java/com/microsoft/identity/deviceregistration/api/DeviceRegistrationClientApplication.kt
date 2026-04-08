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

package com.microsoft.identity.deviceregistration.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.internal.activebrokerdiscovery.BrokerDiscoveryClientFactory
import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClient
import com.microsoft.identity.common.internal.broker.IInstallCertCallback
import com.microsoft.identity.common.internal.broker.InstallCertActivityLauncher
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdater
import com.microsoft.identity.common.internal.cache.ClientActiveBrokerCache
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.logging.Logger
import com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationClientController
import com.microsoft.identity.deviceregistration.DeviceRegistrationIpcStrategiesProvider
import com.microsoft.identity.deviceregistration.java.DeviceState
import com.microsoft.identity.deviceregistration.java.DrsDiscoveryEndpoint
import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationRecord
import com.microsoft.identity.deviceregistration.java.exception.DeviceRegistrationException
import com.microsoft.identity.deviceregistration.java.protocol.parameters.DeviceRegistrationPreAuthorizedV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.DeviceRegistrationWithTokensV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.GetDeviceRegistrationRecordV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.GetDeviceRegistrationRecordsV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.GetDeviceTokenV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.GetInstallWpjCertificateIntentRequestV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.GetRegistrationStateV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.InstallCertificateSilentlyV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.PreProvisionedBlobV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.UnregisterDeviceV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.response.DeviceRegistrationPreAuthorizedV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.DeviceRegistrationWithTokensV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.GetDeviceRegistrationRecordV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.GetDeviceRegistrationRecordsV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.GetDeviceTokenV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.GetInstallWpjCertificateIntentRequestV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.GetRegistrationStateV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.InstallCertificateSilentlyV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.PreProvisionedBlobV0Response
import java.util.UUID

/**
 * Performs device registration operations via IPC to the broker.
 *
 * Strategy construction is delegated to [com.microsoft.identity.deviceregistration.DeviceRegistrationIpcStrategiesProvider].
 * Default provider supplies ContentProvider + BoundService.
 * Broker supplies its own provider that adds WpjLegacyAccountAuthenticatorStrategy.
 *
 * All public methods require a [java.util.UUID] correlationId for IPC request tracing.
 */
class DeviceRegistrationClientApplication {

    private val mController: AndroidDeviceRegistrationClientController

    /**
     * Simple constructor — creates defaults from context.
     * Used by OneAuth consumers.
     *
     * @throws com.microsoft.identity.common.java.exception.ClientException if no valid broker is found.
     */
    @Throws(ClientException::class)
    constructor(context: Context) {
        val components = AndroidPlatformComponentsFactory.createFromContext(context)
        mController = buildController(
            context,
            components,
            BrokerDiscoveryClientFactory.Companion.getInstanceForClientSdk(context, components),
            DeviceRegistrationIpcStrategiesProvider()
        )
    }

    /**
     * Full constructor — accepts all dependencies explicitly.
     * Used by broker's DRCA and for testing.
     *
     * @throws ClientException if no valid broker is found.
     */
    @Throws(ClientException::class)
    constructor(
        context: Context,
        components: IPlatformComponents,
        discoveryClient: IBrokerDiscoveryClient,
        strategiesProvider: DeviceRegistrationIpcStrategiesProvider
    ) {
        mController = buildController(context, components, discoveryClient, strategiesProvider)
    }

    companion object {
        private val TAG = DeviceRegistrationClientApplication::class.java.simpleName

        private fun buildController(
            context: Context,
            components: IPlatformComponents,
            discoveryClient: IBrokerDiscoveryClient,
            strategiesProvider: DeviceRegistrationIpcStrategiesProvider
        ): AndroidDeviceRegistrationClientController {
            val cacheUpdater = ActiveBrokerCacheUpdater(
                context,
                ClientActiveBrokerCache.Companion.getBrokerSdkCache(components.storageSupplier)
            )
            return AndroidDeviceRegistrationClientController(
                context,
                components,
                discoveryClient,
                strategiesProvider,
                cacheUpdater
            )
        }
    }

    /**
     * Performs Device Registration with Access Token.
     *
     * @param idToken               (JWT) with an ID Token.
     * @param accessToken           accessToken obtained from OAuth.
     * @param refreshToken          token used to set up the PRT.
     * @param registerAsSharedDevice whether to register as shared device.
     * @param drsDiscoveryEndpoint     discovery endpoint name. Default is "PROD".
     * @param correlationId         correlation ID for request tracing.
     */
    @JvmOverloads
    @Throws(BaseException::class)
    fun deviceRegistrationWithTokens(
        idToken: String,
        accessToken: String,
        refreshToken: String,
        registerAsSharedDevice: Boolean,
        correlationId: UUID,
        drsDiscoveryEndpoint: DrsDiscoveryEndpoint = DrsDiscoveryEndpoint.PROD
    ): IDeviceRegistrationRecord {
        val methodTag = "$TAG:deviceRegistrationWithTokens"
        Logger.info(methodTag, "Registration started. CorrelationId: $correlationId")
        val responseSerialized = mController.execute(
            DeviceRegistrationWithTokensV0Parameters(
                correlationId,
                idToken,
                accessToken,
                refreshToken,
                registerAsSharedDevice,
                drsDiscoveryEndpoint.name
            )
        )
        val result = DeviceRegistrationWithTokensV0Response.create(responseSerialized)
            .deviceRegistrationRecord
        Logger.info(methodTag, "Registration ended successfully.")
        return result
    }

    /**
     * Unregisters/leaves the device registration.
     *
     * @param deviceRegistrationRecord record to unregister.
     * @param correlationId            correlation ID for request tracing.
     */
    @Throws(BaseException::class)
    fun leave(deviceRegistrationRecord: IDeviceRegistrationRecord, correlationId: UUID) {
        val methodTag = "$TAG:leave"
        Logger.info(methodTag, "Leave started. CorrelationId: $correlationId")
        mController.execute(UnregisterDeviceV0Parameters(correlationId, deviceRegistrationRecord))
        Logger.info(methodTag, "Leave ended successfully.")
    }

    /**
     * Returns a signed pre-provisioned blob (JWS) containing the nonce from ADRS.
     *
     * @param tenantId      tenant ID for device registration.
     * @param correlationId correlation ID for request tracing.
     */
    @Throws(BaseException::class)
    fun getPreProvisionedBlob(tenantId: String, correlationId: UUID): String {
        val methodTag = "$TAG:getPreProvisionedBlob"
        Logger.info(methodTag, "GetPreProvisionedBlob started. CorrelationId: $correlationId")
        val responseSerialized = mController.execute(
            PreProvisionedBlobV0Parameters(
                correlationId,
                tenantId
            )
        )
        val result = PreProvisionedBlobV0Response.create(responseSerialized).jws
        Logger.info(methodTag, "Blob returned successfully.")
        return result
    }

    /**
     * Gets the registration state from ADRS.
     *
     * @param deviceRegistrationRecord record to query state for.
     * @param correlationId            correlation ID for request tracing.
     * @return [com.microsoft.identity.deviceregistration.java.DeviceState] representing the device state.
     */
    @Throws(BaseException::class)
    fun getRegistrationState(
        deviceRegistrationRecord: IDeviceRegistrationRecord,
        correlationId: UUID
    ): DeviceState {
        val methodTag = "$TAG:getRegistrationState"
        Logger.info(methodTag, "GetRegistrationState started. CorrelationId: $correlationId")
        val responseSerialized = mController.execute(
            GetRegistrationStateV0Parameters(correlationId, deviceRegistrationRecord)
        )
        val result = DeviceState.Companion.fromString(
            GetRegistrationStateV0Response.create(responseSerialized).deviceState
        )
        Logger.info(methodTag, "Get state ended successfully.")
        return result
    }

    /**
     * Registers device with encrypted preAuthorizedJoinChallenge.
     *
     * @param tenantId                    tenant to register to.
     * @param encryptedPreAuthorizedToken encrypted token (JWE).
     * @param registerAsSharedDevice      whether to register as shared.
     * @param drsDiscoveryEndpoint           discovery endpoint name.
     * @param correlationId               correlation ID for request tracing.
     */
    @JvmOverloads
    @Throws(BaseException::class)
    fun registerWithEncryptedPreAuthorizedToken(
        tenantId: String,
        encryptedPreAuthorizedToken: String,
        registerAsSharedDevice: Boolean,
        correlationId: UUID,
        drsDiscoveryEndpoint: DrsDiscoveryEndpoint = DrsDiscoveryEndpoint.PROD
    ): IDeviceRegistrationRecord {
        return registerWithPreAuthorizedTokenInternal(
            tenantId, encryptedPreAuthorizedToken, registerAsSharedDevice, true, drsDiscoveryEndpoint, correlationId
        )
    }

    /**
     * Registers device with preAuthorizedJoinChallenge.
     *
     * @param tenantId               tenant to register to.
     * @param preAuthorizedToken     challenge blob.
     * @param registerAsSharedDevice whether to register as shared.
     * @param correlationId          correlation ID for request tracing.
     * @param drsDiscoveryEndpoint      discovery endpoint name.
     */
    @JvmOverloads
    @Throws(BaseException::class)
    fun registerWithPreAuthorizedToken(
        tenantId: String,
        preAuthorizedToken: String,
        registerAsSharedDevice: Boolean,
        correlationId: UUID,
        drsDiscoveryEndpoint: DrsDiscoveryEndpoint = DrsDiscoveryEndpoint.PROD
    ): IDeviceRegistrationRecord {
        return registerWithPreAuthorizedTokenInternal(
            tenantId, preAuthorizedToken, registerAsSharedDevice, false, drsDiscoveryEndpoint, correlationId
        )
    }

    @Throws(BaseException::class)
    private fun registerWithPreAuthorizedTokenInternal(
        tenantId: String,
        preAuthorizedToken: String,
        registerAsSharedDevice: Boolean,
        isEncrypted: Boolean,
        drsDiscoveryEndpoint: DrsDiscoveryEndpoint,
        correlationId: UUID
    ): IDeviceRegistrationRecord {
        val methodTag = "$TAG:registerWithPreAuthorizedTokenInternal"
        Logger.info(methodTag, "Registration started. CorrelationId: $correlationId")
        val responseSerialized = mController.execute(
            DeviceRegistrationPreAuthorizedV0Parameters(
                correlationId, tenantId, preAuthorizedToken,
                isEncrypted, registerAsSharedDevice, drsDiscoveryEndpoint.name
            )
        )
        val result = DeviceRegistrationPreAuthorizedV0Response.create(responseSerialized)
            .deviceRegistrationRecord
        Logger.info(methodTag, "Registration ended successfully.")
        return result
    }

    /**
     * Gets the device token for a device registration record.
     *
     * @param deviceRegistrationRecord record to get token for.
     * @param resources                resource requiring device token.
     * @param correlationId            correlation ID for request tracing.
     * @param scope                    optional scope.
     */
    @Throws(BaseException::class)
    @JvmOverloads
    fun getDeviceToken(
        deviceRegistrationRecord: IDeviceRegistrationRecord,
        resources: String,
        correlationId: UUID,
        scope: String? = null
    ): String {
        val methodTag = "$TAG:getDeviceToken"
        Logger.info(methodTag, "GetDeviceToken started. CorrelationId: $correlationId")
        val responseSerialized = mController.execute(
            GetDeviceTokenV0Parameters(correlationId, deviceRegistrationRecord, resources, scope)
        )
        Logger.info(methodTag, "Get device token ended successfully.")
        return GetDeviceTokenV0Response.create(responseSerialized).deviceToken
    }

    /**
     * Installs device registration certificate on device.
     *
     * @param deviceRegistrationRecord record to install cert for.
     * @param activity                 Activity for cert install UI.
     * @param callback                 callback for result.
     * @param correlationId            correlation ID for request tracing.
     */
    fun installCert(
        deviceRegistrationRecord: IDeviceRegistrationRecord,
        activity: Activity,
        callback: IInstallCertCallback,
        correlationId: UUID
    ) {
        val methodTag = "$TAG:installCert"
        Logger.info(methodTag, "InstallCert started. CorrelationId: $correlationId")
        try {
            val responseSerialized = mController.execute(
                GetInstallWpjCertificateIntentRequestV0Parameters(
                    correlationId,
                    deviceRegistrationRecord
                )
            )
            val response = GetInstallWpjCertificateIntentRequestV0Response.create(responseSerialized)
            Logger.info(methodTag, "Response from broker received")
            InstallCertActivityLauncher.installCertificate(
                activity,
                createInstallCertIntent(response),
                callback,
                response.installCertActivityResultKey,
                response.installCertActivityErrorKey
            )
        } catch (exception: BaseException) {
            callback.onError(exception)
        }
    }

    private fun createInstallCertIntent(response: GetInstallWpjCertificateIntentRequestV0Response): Intent {
        val intent = Intent()
        intent.setPackage(response.brokerPackageName)
        intent.setClassName(response.brokerPackageName, response.activityClassName)
        for (key in response.extras.keys) {
            intent.putExtra(key, response.extras[key])
        }
        return intent
    }

    /**
     * Installs certificate silently.
     *
     * @param deviceRegistrationRecord record to install cert for.
     * @param correlationId            correlation ID for request tracing.
     */
    @Throws(DeviceRegistrationException::class, ClientException::class)
    fun installCertSilently(
        deviceRegistrationRecord: IDeviceRegistrationRecord,
        correlationId: UUID
    ): Boolean {
        val methodTag = "$TAG:installCertSilently"
        Logger.info(methodTag, "InstallCertSilently started. CorrelationId: $correlationId")
        val responseSerialized = mController.execute(
            InstallCertificateSilentlyV0Parameters(correlationId, deviceRegistrationRecord)
        )
        Logger.info(methodTag, "Install cert silently ended successfully.")
        return InstallCertificateSilentlyV0Response.create(responseSerialized).isCertificateInstalled
    }

    /**
     * Gets all device registration records.
     *
     * @param correlationId correlation ID for request tracing.
     * @return list of device registration records.
     */
    @Throws(BaseException::class)
    fun getAllEntries(correlationId: UUID): List<IDeviceRegistrationRecord> {
        val methodTag = "$TAG:getAllEntries"
        Logger.info(methodTag, "GetAllEntries started. CorrelationId: $correlationId")
        val responseSerialized = mController.execute(
            GetDeviceRegistrationRecordsV0Parameters(
                correlationId
            )
        )
        Logger.info(methodTag, "Return all device registration records.")
        return GetDeviceRegistrationRecordsV0Response.create(responseSerialized)
            .deviceRegistrationRecords
    }

    /**
     * Gets the device registration record matching the supplied identifier.
     *
     * @param identifier    tenant ID or UPN.
     * @param correlationId correlation ID for request tracing.
     * @return matching record, or null.
     */
    @Throws(BaseException::class)
    fun getDeviceRegistrationRecord(
        identifier: String,
        correlationId: UUID
    ): IDeviceRegistrationRecord? {
        val methodTag = "$TAG:getDeviceRegistrationRecord"
        Logger.info(methodTag, "GetDeviceRegistrationRecord started. CorrelationId: $correlationId")
        val responseSerialized = mController.execute(
            GetDeviceRegistrationRecordV0Parameters(correlationId, identifier)
        )
        Logger.info(methodTag, "Return a device registration record.")
        return GetDeviceRegistrationRecordV0Response.create(responseSerialized)
            .deviceRegistrationRecord
    }
}