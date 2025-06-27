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
package com.microsoft.identity.common.java.opentelemetry

import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager.getFlightsProvider
import com.microsoft.identity.common.java.interfaces.IStorageSupplier
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo
import com.microsoft.identity.common.java.util.StringUtil

/**
 * Utility class for storing telemetry region TDBR claims by tenant.
 */
class EUClaimStorageUtil {
    companion object {
        private val TAG = EUClaimStorageUtil::class.java.simpleName

        /**
         * Store telemetry region by tenant.
         *
         * @param clientInfo the client info containing tenant information and tdbr claim
         */
        fun storeTelemetryRegionByTenant(
            supplier: IStorageSupplier,
            clientInfo: ClientInfo
        ) {
            val methodTag = "$TAG:storeTelemetryRegionByTenant"

            if (!getFlightsProvider().isFlightEnabled(CommonFlight.ENABLE_USING_TDBR_CLAIM_FOR_EU_ROUTING)) {
                // If flight is not enabled, just return, don't store anything
                return;
            }

            val tenantId = clientInfo.utid
            val tdbrClaim = clientInfo.tdbrClaim

            if (StringUtil.isNullOrEmpty(tenantId)) {
                Logger.warn(
                    methodTag,
                    "tenantId is null or empty. Not storing telemetry region by tenant."
                )
                return
            }

            if (StringUtil.isNullOrEmpty(tdbrClaim)) {
                Logger.warn(
                    methodTag,
                    "Received no tdbr claim, not storing anything in shared preferences.."
                )
                return
            }

            // Store the tdbr claim for a specific tenant ID
            Logger.info(
                methodTag,
                "Storing telemetry region by tenant: $tenantId, TDBR Claim: $tdbrClaim"
            )
            val tdbrValueStore = supplier.getUnencryptedNameValueStore(
                ClientInfo.TDBR_CLAIM,
                String::class.java
            )
            tdbrValueStore.put(tenantId, tdbrClaim)

            // Attach tenant id to the current span
            SpanExtension.current().setAttribute(AttributeName.tenant_id.name, tenantId)
        }
    }
}