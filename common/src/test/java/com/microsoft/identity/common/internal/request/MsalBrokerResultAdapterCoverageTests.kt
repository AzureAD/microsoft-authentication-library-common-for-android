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
package com.microsoft.identity.common.internal.request

import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.broker.BrokerResult
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter
import com.microsoft.identity.common.java.cache.CacheRecord
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.exception.ArgumentException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.exception.UnsupportedBrokerException
import com.microsoft.identity.common.java.exception.UserCancelException
import com.microsoft.identity.internal.testutils.MockRecords
import lombok.SneakyThrows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Deterministic coverage tests for the pure-logic bundle marshalling helpers of
 * [MsalBrokerResultAdapter] that are not already exercised by
 * [MsalBrokerResultAdapterTests]. All tests are fully synchronous (no threads,
 * async, real crypto, keystore or network).
 */
@RunWith(RobolectricTestRunner::class)
class MsalBrokerResultAdapterCoverageTests {

    private val adapter = MsalBrokerResultAdapter()

    // region device mode

    @Test
    @SneakyThrows
    fun deviceMode_roundTripsThroughBundle() {
        assertTrue(adapter.getDeviceModeFromResultBundle(adapter.bundleFromDeviceMode(true)))
        assertEquals(false, adapter.getDeviceModeFromResultBundle(adapter.bundleFromDeviceMode(false)))
    }

    @Test(expected = ClientException::class)
    fun getDeviceModeFromResultBundle_emptyBundle_throws() {
        adapter.getDeviceModeFromResultBundle(Bundle())
    }

    // endregion

    // region simple error/result helpers

    @Test
    fun getExceptionForEmptyResultBundle_returnsInvalidBrokerBundle() {
        val exception = adapter.getExceptionForEmptyResultBundle()
        assertEquals(ClientException.INVALID_BROKER_BUNDLE, exception.errorCode)
    }

    @Test
    fun getGenerateShrResultFromResultBundle_missing_returnsNull() {
        assertNull(adapter.getGenerateShrResultFromResultBundle(Bundle()))
    }

    @Test(expected = ClientException::class)
    fun getSupportedWebAppsContractFromBundle_missing_throws() {
        adapter.getSupportedWebAppsContractFromBundle(Bundle())
    }

    @Test(expected = ClientException::class)
    fun getExecuteWebAppRequestResultFromBundle_missing_throws() {
        adapter.getExecuteWebAppRequestResultFromBundle(Bundle())
    }

    @Test(expected = ClientException::class)
    fun getPreferredAuthMethodFromResultBundle_nullBundle_throws() {
        adapter.getPreferredAuthMethodFromResultBundle(null)
    }

    // endregion

    // region remove account verification

    @Test
    @SneakyThrows
    fun verifyRemoveAccountResultFromBundle_nullBundle_returns() {
        // Backward compatibility: null bundle is treated as success.
        adapter.verifyRemoveAccountResultFromBundle(null)
    }

    @Test
    @SneakyThrows
    fun verifyRemoveAccountResultFromBundle_successResult_returns() {
        val brokerResult = BrokerResult.Builder().success(true).build()
        val bundle = adapter.bundleFromBrokerResult(brokerResult, null)
        adapter.verifyRemoveAccountResultFromBundle(bundle)
    }

    @Test(expected = ClientException::class)
    fun verifyRemoveAccountResultFromBundle_failureResult_throws() {
        val brokerResult = BrokerResult.Builder()
            .success(false)
            .errorCode("remove_account_failed")
            .errorMessage("could not remove account")
            .build()
        val bundle = adapter.bundleFromBrokerResult(brokerResult, null)
        adapter.verifyRemoveAccountResultFromBundle(bundle)
    }

    // endregion

    // region hello / handshake

    @Test
    fun verifyHelloFromResultBundle_nullBundle_throwsUnsupported() {
        try {
            adapter.verifyHelloFromResultBundle("com.microsoft.test.broker", null)
            fail("Expected UnsupportedBrokerException")
        } catch (e: UnsupportedBrokerException) {
            // expected
        }
    }

    @Test
    fun verifyHelloFromResultBundle_emptyBundle_throwsUnsupported() {
        try {
            adapter.verifyHelloFromResultBundle("com.microsoft.test.broker", Bundle())
            fail("Expected UnsupportedBrokerException")
        } catch (e: UnsupportedBrokerException) {
            // expected
        }
    }

    @Test
    @SneakyThrows
    fun verifyHelloFromResultBundle_withNegotiatedVersion_returnsVersion() {
        val bundle = Bundle().apply {
            putString(AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY, "13.0")
        }
        assertEquals("13.0", adapter.verifyHelloFromResultBundle("com.microsoft.test.broker", bundle))
    }

    // endregion

    // region broker result / accounts round trips

    @Test
    @SneakyThrows
    fun bundleFromBrokerResult_roundTripsUncompressed() {
        val brokerResult = BrokerResult.Builder()
            .success(true)
            .userName("user@contoso.com")
            .localAccountId("local-account-id")
            .tenantId("tenant-id")
            .authority("https://login.microsoftonline.com/tenant-id")
            .build()

        val bundle = adapter.bundleFromBrokerResult(brokerResult, null)
        val deserialized = adapter.brokerResultFromBundle(bundle)

        assertTrue(deserialized.isSuccess)
        assertEquals("user@contoso.com", deserialized.userName)
        assertEquals("local-account-id", deserialized.localAccountId)
        assertEquals("tenant-id", deserialized.tenantId)
        assertEquals("https://login.microsoftonline.com/tenant-id", deserialized.authority)
    }

    @Test
    @SneakyThrows
    fun bundleFromAccounts_roundTripsThroughBundle() {
        val cacheRecord: ICacheRecord = CacheRecord.builder()
            .account(MockRecords.getMockAccountRecord_AAD())
            .idToken(MockRecords.getMockIdTokenRecord_AAD())
            .accessToken(MockRecords.getMockAccessTokenRecord_AAD())
            .refreshToken(MockRecords.getMockRefreshTokenRecord_AAD())
            .build()

        val bundle = adapter.bundleFromAccounts(listOf(cacheRecord), null)
        val accounts = adapter.getAccountsFromResultBundle(bundle)

        assertEquals(1, accounts.size)
        assertEquals(cacheRecord.account.homeAccountId, accounts[0].account.homeAccountId)
    }

    // endregion

    // region exception-type mapping through the bundle boundary

    @Test
    @SneakyThrows
    fun getBaseExceptionFromBundle_serviceException_mapsToServiceException() {
        val serviceException = ServiceException("service_error", "service failed", 400, null).apply {
            setHttpResponseBody(hashMapOf("error_description" to "bad request"))
        }
        val bundle = adapter.bundleFromBaseException(serviceException, null)

        val received = adapter.getBaseExceptionFromBundle(bundle)

        assertTrue(received is ServiceException)
        assertEquals("service_error", received.errorCode)
        assertEquals("service failed", received.message)
    }

    @Test
    @SneakyThrows
    fun getBaseExceptionFromBundle_userCancel_mapsToUserCancelException() {
        val bundle = adapter.bundleFromBaseException(UserCancelException(), null)

        val received = adapter.getBaseExceptionFromBundle(bundle)

        assertTrue(received is UserCancelException)
    }

    @Test
    @SneakyThrows
    fun getBaseExceptionFromBundle_argumentException_mapsToArgumentException() {
        val argumentException = ArgumentException(
            ArgumentException.BROKER_TOKEN_REQUEST_OPERATION_NAME,
            "scope",
            "bad argument"
        )
        val bundle = adapter.bundleFromBaseException(argumentException, null)

        val received = adapter.getBaseExceptionFromBundle(bundle)

        assertTrue(received is ArgumentException)
    }

    // endregion
}
