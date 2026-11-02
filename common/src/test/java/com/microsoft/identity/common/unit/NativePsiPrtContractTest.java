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

package com.microsoft.identity.common.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Build;
import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.API;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle.Operation;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.BrokerProtocolVersionUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.N)
public class NativePsiPrtContractTest {
    private static final String[] LEGACY_APIS = {
            "MSAL_HELLO|/hello||3.0",
            "ACQUIRE_TOKEN_INTERACTIVE|/acquireTokenInteractive||3.0",
            "ACQUIRE_TOKEN_SILENT|/acquireTokenSilent||3.0",
            "GET_ACCOUNTS|/getAccounts||3.0",
            "REMOVE_ACCOUNT|/removeAccounts||3.0",
            "GET_CURRENT_ACCOUNT_SHARED_DEVICE|/getCurrentAccountSharedDevice||3.0",
            "GET_DEVICE_MODE|/getDeviceMode||3.0",
            "SIGN_OUT_FROM_SHARED_DEVICE|/signOutFromSharedDevice||3.0",
            "GENERATE_SHR|/generateShr||6.0",
            "BROKER_HELLO|/brokerApi/hello|1.0|",
            "BROKER_GET_ACCOUNTS|/brokerApi/getBrokerAccounts|1.0|",
            "BROKER_REMOVE_ACCOUNT|/brokerApi/removeBrokerAccount|1.0|",
            "BROKER_UPDATE_BRT|/brokerApi/updateBrt|1.0|",
            "BROKER_SET_FLIGHTS|/brokerApi/setFlights|3.0|",
            "BROKER_GET_FLIGHTS|/brokerApi/getFlights|3.0|",
            "GET_SSO_TOKEN|/ssoToken||7.0",
            "GET_ALL_SSO_TOKENS|/allSsoTokens||",
            "UNKNOWN|||",
            "DEVICE_REGISTRATION_PROTOCOLS|/multipledeviceRegistration/protocols||",
            "BROKER_UPLOAD_LOGS|/brokerApi/uploadBrokerLogs|4.0|",
            "FETCH_DCF_AUTH_RESULT|/fetchDCFAuthResult||",
            "ACQUIRE_TOKEN_DCF|/acquireTokenDCF||",
            "BROKER_DISCOVERY_METADATA_RETRIEVAL|/brokerElection/brokerDiscoveryMetadataRetrieval||",
            "BROKER_DISCOVERY_FROM_SDK|/brokerElection/brokerDiscoveryFromSdk||",
            "BROKER_DISCOVERY_SET_ACTIVE_BROKER|/brokerElection/setActiveBroker||",
            "PASSTHROUGH|/passthrough||",
            "READ_RESTRICTIONS_MANAGER|/readRestrictionsManager||",
            "GET_PREFERRED_AUTH_METHOD|/getPreferredAuthMethod||",
            "BROKER_INDIVIDUAL_LOGS_UPLOAD|/brokerIndividualLogsUpload||",
            "BROKER_RESTORE_MSA_ACCOUNTS_WITH_TRANSFER_TOKENS|/brokerApi/restoreMsaAccountsWithTransferTokens|5.0|",
            "WEBAPPS_GET_SUPPORTED_WEB_APPS_CONTRACTS|/webapp/getSupportedWebAppsContracts||",
            "WEBAPPS_EXECUTE_WEB_APPS_REQUEST|/webapp/executeWebAppsRequest||",
            "PROVISION_RESOURCE_ACCOUNT|/provisionResourceAccount||",
            "GET_AAD_DEVICE_ID|/getAadDeviceId||",
            "BROKER_GET_NUMBER_MATCH|/brokerApi/getNumberMatch|3.0|"
    };

    private static final String[] LEGACY_OPERATIONS = {
            "MSAL_HELLO|MSAL_HELLO|HELLO",
            "MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST|ACQUIRE_TOKEN_INTERACTIVE|GET_INTENT_FOR_INTERACTIVE_REQUEST",
            "MSAL_ACQUIRE_TOKEN_SILENT|ACQUIRE_TOKEN_SILENT|ACQUIRE_TOKEN_SILENT",
            "MSAL_GET_ACCOUNTS|GET_ACCOUNTS|GET_ACCOUNTS",
            "MSAL_REMOVE_ACCOUNT|REMOVE_ACCOUNT|REMOVE_ACCOUNT",
            "MSAL_GET_DEVICE_MODE|GET_DEVICE_MODE|GET_DEVICE_MODE",
            "MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE|GET_CURRENT_ACCOUNT_SHARED_DEVICE|GET_CURRENT_ACCOUNT",
            "MSAL_SIGN_OUT_FROM_SHARED_DEVICE|SIGN_OUT_FROM_SHARED_DEVICE|REMOVE_ACCOUNT_FROM_SHARED_DEVICE",
            "MSAL_GENERATE_SHR|GENERATE_SHR|GENERATE_SHR",
            "BROKER_GET_KEY_FROM_INACTIVE_BROKER||",
            "BROKER_API_HELLO|BROKER_HELLO|",
            "BROKER_API_GET_BROKER_ACCOUNTS|BROKER_GET_ACCOUNTS|",
            "BROKER_API_REMOVE_BROKER_ACCOUNT|BROKER_REMOVE_ACCOUNT|",
            "BROKER_API_UPDATE_BRT|BROKER_UPDATE_BRT|",
            "BROKER_GET_FLIGHTS|BROKER_GET_FLIGHTS|",
            "BROKER_SET_FLIGHTS|BROKER_SET_FLIGHTS|",
            "MSAL_SSO_TOKEN|GET_SSO_TOKEN|",
            "MSAL_ALL_SSO_TOKENS|GET_ALL_SSO_TOKENS|",
            "DEVICE_REGISTRATION_OPERATIONS|DEVICE_REGISTRATION_PROTOCOLS|",
            "BROKER_API_UPLOAD_LOGS|BROKER_UPLOAD_LOGS|",
            "MSAL_FETCH_DCF_AUTH_RESULT|FETCH_DCF_AUTH_RESULT|",
            "MSAL_ACQUIRE_TOKEN_DCF|ACQUIRE_TOKEN_DCF|",
            "BROKER_DISCOVERY_METADATA_RETRIEVAL|BROKER_DISCOVERY_METADATA_RETRIEVAL|",
            "BROKER_DISCOVERY_FROM_SDK|BROKER_DISCOVERY_FROM_SDK|",
            "BROKER_DISCOVERY_SET_ACTIVE_BROKER|BROKER_DISCOVERY_SET_ACTIVE_BROKER|",
            "PASSTHROUGH|PASSTHROUGH|",
            "BROKER_READ_RESTRICTIONS_MANAGER|READ_RESTRICTIONS_MANAGER|",
            "MSAL_GET_PREFERRED_AUTH_METHOD|GET_PREFERRED_AUTH_METHOD|",
            "BROKER_INDIVIDUAL_LOGS_UPLOAD|BROKER_INDIVIDUAL_LOGS_UPLOAD|",
            "BROKER_API_RESTORE_MSA_ACCOUNTS_WITH_TRANSFER_TOKENS|BROKER_RESTORE_MSA_ACCOUNTS_WITH_TRANSFER_TOKENS|",
            "BROKER_WEBAPPS_API_GET_SUPPORTED_WEB_APPS_CONTRACTS|WEBAPPS_GET_SUPPORTED_WEB_APPS_CONTRACTS|",
            "BROKER_WEBAPPS_API_EXECUTE_WEB_APPS_REQUEST|WEBAPPS_EXECUTE_WEB_APPS_REQUEST|",
            "PROVISION_RESOURCE_ACCOUNT|PROVISION_RESOURCE_ACCOUNT|",
            "GET_AAD_DEVICE_ID|GET_AAD_DEVICE_ID|",
            "BROKER_GET_NUMBER_MATCH|BROKER_GET_NUMBER_MATCH|"
    };

    @Test
    public void nativeApi_preservesEveryLegacyOrdinalPathAndVersion() {
        assertEquals(36, API.values().length);
        for (int i = 0; i < LEGACY_APIS.length; i++) {
            final String[] expected = LEGACY_APIS[i].split("\\|", -1);
            final API actual = API.values()[i];
            assertEquals(expected[0], actual.name());
            assertEquals(i, actual.ordinal());
            assertEquals(orNull(expected[1]), actual.getPath());
            assertEquals(orNull(expected[2]), actual.getBrokerVersion());
            assertEquals(orNull(expected[3]), actual.getMsalVersion());
        }
        assertEquals(35, API.BROKER_RECOVER_PRT_WITH_NATIVE_PSI.ordinal());
        assertEquals("/brokerApi/recoverPrtWithNativePsi",
                API.BROKER_RECOVER_PRT_WITH_NATIVE_PSI.getPath());
        assertEquals("6.0", API.BROKER_RECOVER_PRT_WITH_NATIVE_PSI.getBrokerVersion());
        assertNull(API.BROKER_RECOVER_PRT_WITH_NATIVE_PSI.getMsalVersion());
    }

    @Test
    public void nativeHostMinimum_preservesLegacyHostAndMsalNamespaces() {
        final String nativeMinimum = API.BROKER_RECOVER_PRT_WITH_NATIVE_PSI.getBrokerVersion();
        assertFalse(BrokerProtocolVersionUtil.isProvidedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                "5.0", nativeMinimum));
        assertTrue(BrokerProtocolVersionUtil.isProvidedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                "6.0", nativeMinimum));
        assertTrue(BrokerProtocolVersionUtil.isProvidedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                "5.0", API.BROKER_HELLO.getBrokerVersion()));
        assertTrue(BrokerProtocolVersionUtil.isProvidedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                "5.0", API.BROKER_RESTORE_MSA_ACCOUNTS_WITH_TRANSFER_TOKENS.getBrokerVersion()));
        assertNull(API.GENERATE_SHR.getBrokerVersion());
        assertEquals("6.0", API.GENERATE_SHR.getMsalVersion());
    }

    @Test
    public void nativeOperation_preservesEveryLegacyOrdinalAndRouting() {
        assertEquals(36, Operation.values().length);
        for (int i = 0; i < LEGACY_OPERATIONS.length; i++) {
            final String[] expected = LEGACY_OPERATIONS[i].split("\\|", -1);
            final Operation actual = Operation.values()[i];
            assertEquals(expected[0], actual.name());
            assertEquals(i, actual.ordinal());
            assertEquals(expected[1].isEmpty() ? null : API.valueOf(expected[1]), actual.getContentApi());
            assertEquals(orNull(expected[2]), actual.getAccountManagerOperation());
        }
        assertEquals(35, Operation.BROKER_API_RECOVER_PRT_WITH_NATIVE_PSI.ordinal());
        assertEquals(API.BROKER_RECOVER_PRT_WITH_NATIVE_PSI,
                Operation.BROKER_API_RECOVER_PRT_WITH_NATIVE_PSI.getContentApi());
    }

    @Test
    public void nativeOperation_routesOnlyToNormalContentProvider() throws Exception {
        final BrokerOperationBundle operation = new BrokerOperationBundle(
                Operation.BROKER_API_RECOVER_PRT_WITH_NATIVE_PSI, "test.broker", new Bundle());
        assertEquals("/brokerApi/recoverPrtWithNativePsi", operation.getContentProviderPath());
        assertNull(operation.getOperation().getAccountManagerOperation());
        try {
            operation.getAccountManagerBundle();
            fail("Native proof must not be sent through AccountManager fallback.");
        } catch (final BrokerCommunicationException e) {
            assertEquals(BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                    e.getCategory());
        }
    }

    @Test
    public void nativeContract_hasExactCallerResultAndHelloKeys() {
        assertEquals("account.name", Broker.ACCOUNT_NAME);
        assertEquals("account.authority", Broker.ACCOUNT_AUTHORITY);
        assertEquals("signed.native.assertion", Broker.SIGNED_NATIVE_ASSERTION);
        assertEquals("original.registered.credential.id", Broker.ORIGINAL_REGISTERED_CREDENTIAL_ID);
        assertEquals("nonce", Broker.PRT_NONCE);
        assertEquals("native_psi_prt_recovery_succeeded", Broker.NATIVE_PSI_PRT_RECOVERY_SUCCEEDED);
        assertEquals("native_psi_prt_recovery_handler_available",
                Broker.NATIVE_PSI_PRT_RECOVERY_HANDLER_AVAILABLE);
        assertEquals("native_psi_prt_recovery_outcome_unknown",
                ClientException.NATIVE_PSI_PRT_RECOVERY_OUTCOME_UNKNOWN);
        assertEquals("6.0", Broker.computeMaxHostBrokerProtocol());
        assertEquals("20.0", Broker.CLIENT_MAX_PROTOCOL_VERSION);
        assertEquals("13.0", Broker.DEFAULT_MAX_BROKER_PROTOCOL_VERSION);
        assertEquals(AuthenticationConstants.BrokerContentProvider.BROKER_API_RECOVER_PRT_WITH_NATIVE_PSI_PATH,
                API.BROKER_RECOVER_PRT_WITH_NATIVE_PSI.getPath());
    }

    private static String orNull(final String value) {
        return value.isEmpty() ? null : value;
    }
}
