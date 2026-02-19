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

import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.broker.BrokerResult
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter.REMOVE_RT_FROM_AAD_RESULT_MSAL_PROTOCOL_VERSION
import com.microsoft.identity.common.java.cache.CacheRecord
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.webapps.WebAppsAccountItem
import com.microsoft.identity.common.java.commands.webapps.WebAppsGetTokenSubOperationResponse
import com.microsoft.identity.common.java.dto.AadDeviceIdRecord
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.UiRequiredException
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.result.LocalAuthenticationResult
import com.microsoft.identity.common.java.util.SchemaUtil
import com.microsoft.identity.internal.testutils.MockRecords
import lombok.SneakyThrows
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MsalBrokerResultAdapterTests {

    fun getInstance(): MsalBrokerResultAdapter {
        return MsalBrokerResultAdapter(true)
    }

    @Test
    fun testShouldRemoveRefreshToken_MSAResponse() {
        val mockCacheRecord = CacheRecord.builder()
                .account(MockRecords.getMockAccountRecord_MSA())
                .idToken(MockRecords.getMockIdTokenRecord_MSA())
                .accessToken(MockRecords.getMockAccessTokenRecord_MSA())
                .refreshToken(MockRecords.getMockRefreshTokenRecord_MSA())
                .build()

        val cacheRecords: MutableList<ICacheRecord> = ArrayList()
        cacheRecords.add(mockCacheRecord)

        val mockResult = LocalAuthenticationResult(
                mockCacheRecord,
                cacheRecords,
                SdkType.MSAL,
                true
        )

        // We'll return RT to older SDK
        val resultWithOlderSdk = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                "15.0"
        )

        Assert.assertNotNull(resultWithOlderSdk.refreshToken)
        for(tenantProfile in resultWithOlderSdk.tenantProfileData) {
            Assert.assertNotNull(tenantProfile.refreshToken)
        }

        // With SDK >= 16, we would still return RT.
        val resultWithProtocolVer16 = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                REMOVE_RT_FROM_AAD_RESULT_MSAL_PROTOCOL_VERSION
        )

        Assert.assertNotNull(resultWithProtocolVer16.refreshToken)
        for(tenantProfile in resultWithProtocolVer16.tenantProfileData) {
            Assert.assertNotNull(tenantProfile.refreshToken)
        }

        val resultWithNewerSdk = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                "17.1234"
        )

        Assert.assertNotNull(resultWithNewerSdk.refreshToken)
        for(tenantProfile in resultWithNewerSdk.tenantProfileData) {
            Assert.assertNotNull(tenantProfile.refreshToken)
        }
    }

    @Test
    fun testShouldRemoveRefreshToken_AADResponse() {
        val mockCacheRecord = CacheRecord.builder()
                .account(MockRecords.getMockAccountRecord_AAD())
                .idToken(MockRecords.getMockIdTokenRecord_AAD())
                .accessToken(MockRecords.getMockAccessTokenRecord_AAD())
                .refreshToken(MockRecords.getMockRefreshTokenRecord_AAD())
                .build()

        val cacheRecords: MutableList<ICacheRecord> = ArrayList()
        cacheRecords.add(mockCacheRecord)

        val mockResult = LocalAuthenticationResult(
                mockCacheRecord,
                cacheRecords,
                SdkType.MSAL,
                true
        )

        // We'll still return RT to older SDK
        val resultWithOlderSdk = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                "15.0"
        )

        Assert.assertNotNull(resultWithOlderSdk.refreshToken)
        for(tenantProfile in resultWithOlderSdk.tenantProfileData) {
            Assert.assertNotNull(tenantProfile.refreshToken)
        }

        // With SDK >= 16, we would NOT return RT.
        val resultWithProtocolVer16 = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                REMOVE_RT_FROM_AAD_RESULT_MSAL_PROTOCOL_VERSION
        )

        Assert.assertNull(resultWithProtocolVer16.refreshToken)
        for(tenantProfile in resultWithProtocolVer16.tenantProfileData) {
            Assert.assertNull(tenantProfile.refreshToken)
        }

        val resultWithNewerSdk = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                "17.1234"
        )

        Assert.assertNull(resultWithNewerSdk.refreshToken)
        for(tenantProfile in resultWithNewerSdk.tenantProfileData) {
            Assert.assertNull(tenantProfile.refreshToken)
        }
    }

    @Test
    fun testShouldRemoveRefreshToken_MSAPassthroughResponse() {
        val mockCacheRecord = CacheRecord.builder()
                .account(MockRecords.getMockAccountRecord_MSAPassthrough())
                .idToken(MockRecords.getMockIdTokenRecord_MSAPassthrough())
                .accessToken(MockRecords.getMockAccessTokenRecord_MSAPassthrough())
                .refreshToken(MockRecords.getMockRefreshTokenRecord_MSAPassthrough())
                .build()

        val cacheRecords: MutableList<ICacheRecord> = ArrayList()
        cacheRecords.add(mockCacheRecord)

        val mockResult = LocalAuthenticationResult(
                mockCacheRecord,
                cacheRecords,
                SdkType.MSAL,
                true
        )

        // We'll still return RT to older SDK
        val resultWithOlderSdk = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                "15.0"
        )

        Assert.assertNotNull(resultWithOlderSdk.refreshToken)
        for(tenantProfile in resultWithOlderSdk.tenantProfileData) {
            Assert.assertNotNull(tenantProfile.refreshToken)
        }

        // With SDK >= 16, we would NOT return RT.
        // (Because MSA passthrough = AAD guest scenario)
        val resultWithProtocolVer16 = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                REMOVE_RT_FROM_AAD_RESULT_MSAL_PROTOCOL_VERSION
        )

        Assert.assertNull(resultWithProtocolVer16.refreshToken)
        for(tenantProfile in resultWithProtocolVer16.tenantProfileData) {
            Assert.assertNull(tenantProfile.refreshToken)
        }

        val resultWithNewerSdk = getInstance().buildBrokerResultFromAuthenticationResult(
                mockResult,
                "17.1234"
        )

        Assert.assertNull(resultWithNewerSdk.refreshToken)
        for(tenantProfile in resultWithNewerSdk.tenantProfileData) {
            Assert.assertNull(tenantProfile.refreshToken)
        }
    }

    /**
     * Tests result for [MsalBrokerResultAdapter.resourceAccountRecordFromBundle]
     */
    fun testResourceAccountRecordFromBundle() {
        val mockHomeAccountId = "mockHomeAccountId"
        val mockNegotiatedBrokerVersion = "18.0"
        val mockAccountName = "mockAccountName"
        val mockAccountRecord = AccountRecord()
        mockAccountRecord.homeAccountId = mockHomeAccountId
        mockAccountRecord.username = mockAccountName
        val mockCacheRecord = CacheRecord.builder()
            .account(mockAccountRecord)
            .build()
        val resultAdapter = MsalBrokerResultAdapter()

        val resultBundle = resultAdapter.bundleFromAccounts(
            listOf<ICacheRecord>(mockCacheRecord),
            mockNegotiatedBrokerVersion
        )

        val resultCacheRecord = resultAdapter.resourceAccountRecordFromBundle(resultBundle)

        assertEquals(mockHomeAccountId, resultCacheRecord.account.homeAccountId)
        assertEquals(mockAccountName, resultCacheRecord.account.username)
    }

    fun testResourceAccountRecordFromBundle_NoAccountReturned() {
        val mockHomeAccountId = "mockHomeAccountId"
        val mockNegotiatedBrokerVersion = "18.0"
        val mockAccountName = "mockAccountName"
        val mockAccountRecord = AccountRecord()
        mockAccountRecord.homeAccountId = mockHomeAccountId
        mockAccountRecord.username = mockAccountName
        val resultAdapter = MsalBrokerResultAdapter()

        val resultBundle = resultAdapter.bundleFromAccounts(
            emptyList(),
            mockNegotiatedBrokerVersion
        )

        try {
            resultAdapter.resourceAccountRecordFromBundle(resultBundle)
            Assert.fail("Expected exception not thrown")
        } catch (e: ClientException) {
            // Expected exception
            assertEquals(ClientException.INVALID_BROKER_BUNDLE, e.errorCode)
        }
    }

    /**
     * Tests result for [MsalBrokerResultAdapter.resourceAccountRecordFromBundle]
     */
    @Test
    @SneakyThrows
    fun testResourceAccountRecordFromBundle_MoreThanOneAccount() {
        val mockHomeAccountId = "mockHomeAccountId"
        val mockNegotiatedBrokerVersion = "18.0"
        val mockAccountName = "mockAccountName"
        val mockAccountRecord = AccountRecord()
        mockAccountRecord.homeAccountId = mockHomeAccountId
        mockAccountRecord.username = mockAccountName
        val mockCacheRecord = CacheRecord.builder()
            .account(mockAccountRecord)
            .build()
        val resultAdapter = MsalBrokerResultAdapter()

        val resultBundle = resultAdapter.bundleFromAccounts(
            listOf<ICacheRecord>(mockCacheRecord, mockCacheRecord),
            mockNegotiatedBrokerVersion
        )

        try {
            resultAdapter.resourceAccountRecordFromBundle(resultBundle)
            Assert.fail("Expected exception not thrown")
        } catch (e: ClientException) {
            // Expected exception
            assertEquals(ClientException.INVALID_BROKER_BUNDLE, e.errorCode)
        }
    }

    @Test
    @SneakyThrows
    fun testAadDeviceIdRecordFromBundle() {
        val mockTenantId = "mockTenantId"
        val mockDeviceId = "mockDeviceId"
        val mockAadDeviceIdRecord = AadDeviceIdRecord(mockTenantId, mockDeviceId)
        val brokerResult = BrokerResult.Builder()
            .aadDeviceIdRecord(mockAadDeviceIdRecord)
            .success(true)
            .build()
        val resultAdapter = MsalBrokerResultAdapter()
        val resultBundle = resultAdapter.bundleFromBrokerResult(brokerResult, "18.0")

        val aadDeviceRecord = resultAdapter.aadDeviceIdRecordFromBundle(resultBundle)

        assertEquals(mockTenantId, aadDeviceRecord.tenantId)
        assertEquals(mockDeviceId, aadDeviceRecord.deviceId)
    }

    @Test
    @SneakyThrows
    fun testAadDeviceIdRecordFromBundle_NoRegistration() {
        val mockException = ClientException(
            "no_such_device_registration",
            "The device registration record requested does not exist"
        )
        val resultAdapter = MsalBrokerResultAdapter()
        val resultBundle = resultAdapter.bundleFromBaseException(mockException, null)

        try {
            resultAdapter.aadDeviceIdRecordFromBundle(resultBundle)
            Assert.fail("Expected exception not thrown")
        } catch (e: ClientException) {
            // Expected exception
            assertEquals(mockException.errorCode, e.errorCode)
        }
    }

    @Test
    @SneakyThrows
    fun testBundleFromBaseException_UiRequiredException() {
        val mockErrorCode = "invalid_grant"
        val mockErrorMessage = "invalid_grant"
        val mockUsername = "mock_username"

        val uiRequiredException = UiRequiredException(mockErrorCode, mockErrorMessage).apply {
            username = mockUsername
        }

        val resultAdapter = MsalBrokerResultAdapter()
        val resultBundle = resultAdapter.bundleFromBaseException(uiRequiredException, null)

        assertEquals(false, resultBundle.getBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS))
        val brokerResult = resultAdapter.brokerResultFromBundle(resultBundle)

        assertEquals(mockErrorCode, brokerResult.errorCode)
        assertEquals(mockErrorMessage, brokerResult.errorMessage)
        assertEquals(mockUsername, brokerResult.userName)
        assertEquals(UiRequiredException.sName, brokerResult.exceptionType)

        val receivedException = resultAdapter.getBaseExceptionFromBundle(resultBundle) as UiRequiredException
        assertNotNull(receivedException)
        assertEquals(mockUsername, receivedException.username)
        assertEquals(mockErrorCode, receivedException.errorCode)
        assertEquals(mockErrorMessage, receivedException.message)
    }

    @Test
    @SneakyThrows
    fun testBundleFromBaseException_UiRequiredException_NoUsername() {
        val mockErrorCode = "invalid_grant"
        val mockErrorMessage = "invalid_grant"

        val uiRequiredException = UiRequiredException(mockErrorCode, mockErrorMessage)

        val resultAdapter = MsalBrokerResultAdapter()
        val resultBundle = resultAdapter.bundleFromBaseException(uiRequiredException, null)

        assertEquals(false, resultBundle.getBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS))
        val brokerResult = resultAdapter.brokerResultFromBundle(resultBundle)

        assertEquals(mockErrorCode, brokerResult.errorCode)
        assertEquals(mockErrorMessage, brokerResult.errorMessage)
        assertNull(brokerResult.userName)
        assertEquals(UiRequiredException.sName, brokerResult.exceptionType)

        val receivedException = resultAdapter.getBaseExceptionFromBundle(resultBundle) as UiRequiredException
        assertNotNull(receivedException)
        assertNull(receivedException.username)
        assertEquals(mockErrorCode, receivedException.errorCode)
        assertEquals(mockErrorMessage, receivedException.message)
    }

    @Test
    fun testGetBrokerPerformanceMetricsFromBundle_WithValidTimestamps() {
        val mockRequestReceivedTimestamp = 123456789L
        val mockResponseGenerationTimestamp = 987654321L
        val resultAdapter = MsalBrokerResultAdapter()

        val resultBundle = android.os.Bundle().apply {
            putLong(
                com.microsoft.identity.common.java.AuthenticationConstants.Broker.BROKER_REQUEST_RECEIVED_TIMESTAMP,
                mockRequestReceivedTimestamp
            )
            putLong(
                com.microsoft.identity.common.java.AuthenticationConstants.Broker.BROKER_RESPONSE_GENERATION_TIMESTAMP,
                mockResponseGenerationTimestamp
            )
        }

        val metrics = resultAdapter.getBrokerPerformanceMetricsFromBundle(resultBundle)

        assertNotNull(metrics)

        // Validate timestamp values
        assertEquals(mockRequestReceivedTimestamp, metrics!!.brokerRequestReceivedTimestamp)
        assertEquals(mockResponseGenerationTimestamp, metrics.brokerResponseGenerationTimestamp)

        // Validate calculated broker handling time
        val expectedBrokerHandlingTime = mockResponseGenerationTimestamp - mockRequestReceivedTimestamp
        assertEquals(expectedBrokerHandlingTime, metrics.brokerHandlingTime)

        // Validate response latency (should be >= 0)
        assertNotNull(metrics.responseLatency)
        assertTrue(metrics.responseLatency >= 0)
    }

    @Test
    fun testGetBrokerPerformanceMetricsFromBundle_WithoutTimestamps() {
        val resultAdapter = MsalBrokerResultAdapter()
        val resultBundle = android.os.Bundle() // Empty bundle, no timestamps
        val metrics = resultAdapter.getBrokerPerformanceMetricsFromBundle(resultBundle)
        assertNull(metrics)
    }

    @Test
    @SneakyThrows
    fun testGetBaseExceptionFromBundle_WithBrokerAppInfo() {
        val mockErrorCode = "test_error"
        val mockErrorMessage = "Test error message"
        val mockBrokerVersion = "1.2.3"
        val mockBrokerPackageName = "com.microsoft.broker"

        val clientException = ClientException(mockErrorCode, mockErrorMessage)
        val resultAdapter = MsalBrokerResultAdapter()

        val resultBundle = resultAdapter.bundleFromBaseException(clientException, null).apply {
            putString(AuthenticationConstants.Broker.BROKER_VERSION, mockBrokerVersion)
            putString(AuthenticationConstants.Broker.BROKER_PACKAGE_NAME, mockBrokerPackageName)
        }

        val receivedException = resultAdapter.getBaseExceptionFromBundle(resultBundle)

        assertNotNull(receivedException)
        assertEquals(mockErrorCode, receivedException.errorCode)
        assertEquals(mockErrorMessage, receivedException.message)
        assertEquals(mockBrokerVersion, receivedException.brokerAppVersion)
        assertEquals(mockBrokerPackageName, receivedException.brokerAppPackageName)
    }

    @Test
    @SneakyThrows
    fun testGetBaseExceptionFromBundle_WithoutBrokerAppInfo() {
        val mockErrorCode = "test_error"
        val mockErrorMessage = "Test error message"

        val clientException = ClientException(mockErrorCode, mockErrorMessage)
        val resultAdapter = MsalBrokerResultAdapter()

        val resultBundle = resultAdapter.bundleFromBaseException(clientException, null)

        val receivedException = resultAdapter.getBaseExceptionFromBundle(resultBundle)

        assertNotNull(receivedException)
        assertEquals(mockErrorCode, receivedException.errorCode)
        assertEquals(mockErrorMessage, receivedException.message)
        assertNull(receivedException.brokerAppVersion)
        assertNull(receivedException.brokerAppPackageName)
    }

    // ==================== bundleFromAuthenticationResultForWebApps Tests ====================

    /**
     * Test that bundleFromAuthenticationResultForWebApps includes all required token fields
     */
    @Test
    @SneakyThrows
    fun testBundleFromAuthenticationResultForWebApps_ContainsAllRequiredFields() {
        val mockCacheRecord = CacheRecord.builder()
            .account(MockRecords.getMockAccountRecord_AAD())
            .idToken(MockRecords.getMockIdTokenRecord_AAD())
            .accessToken(MockRecords.getMockAccessTokenRecord_AAD())
            .refreshToken(MockRecords.getMockRefreshTokenRecord_AAD())
            .build()

        val cacheRecords: MutableList<ICacheRecord> = ArrayList()
        cacheRecords.add(mockCacheRecord)

        val mockResult = LocalAuthenticationResult(
            mockCacheRecord,
            cacheRecords,
            SdkType.MSAL,
            false
        )

        val resultAdapter = getInstance()
        val mockState = "field_validation_state"
        val protocolVersion = null // Use null to ensure no compression

        val resultBundle = resultAdapter.bundleFromAuthenticationResultForWebApps(
            mockResult,
            protocolVersion,
            mockState
        )

        assertNotNull(resultBundle)
        val resultString = resultBundle.getString(AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT)
        assertNotNull(resultString)

        // Verify the JSON contains expected fields
        assertTrue(resultString!!.contains(WebAppsGetTokenSubOperationResponse.FIELD_STATE))
        assertTrue(resultString.contains(WebAppsGetTokenSubOperationResponse.FIELD_EXPIRES_IN))
        assertTrue(resultString.contains(WebAppsGetTokenSubOperationResponse.FIELD_CLIENT_INFO))
        assertTrue(resultString.contains(WebAppsGetTokenSubOperationResponse.FIELD_ACCOUNT))
        assertTrue(resultString.contains(WebAppsGetTokenSubOperationResponse.FIELD_ID_TOKEN))
        assertTrue(resultString.contains(WebAppsGetTokenSubOperationResponse.FIELD_ACCESS_TOKEN))
        assertTrue(resultString.contains(WebAppsGetTokenSubOperationResponse.FIELD_SCOPES))
    }

    /**
     * Test that bundleFromAuthenticationResultForWebApps handles empty state string
     */
    @Test
    @SneakyThrows
    fun testBundleFromAuthenticationResultForWebApps_WithEmptyState_Success() {
        val mockCacheRecord = CacheRecord.builder()
            .account(MockRecords.getMockAccountRecord_AAD())
            .idToken(MockRecords.getMockIdTokenRecord_AAD())
            .accessToken(MockRecords.getMockAccessTokenRecord_AAD())
            .refreshToken(MockRecords.getMockRefreshTokenRecord_AAD())
            .build()

        val cacheRecords: MutableList<ICacheRecord> = ArrayList()
        cacheRecords.add(mockCacheRecord)

        val mockResult = LocalAuthenticationResult(
            mockCacheRecord,
            cacheRecords,
            SdkType.MSAL,
            false
        )

        val resultAdapter = getInstance()
        val protocolVersion = "10.0"

        val resultBundle = resultAdapter.bundleFromAuthenticationResultForWebApps(
            mockResult,
            protocolVersion,
            "" // empty state
        )

        assertNotNull(resultBundle)
        assertTrue(resultBundle.containsKey(AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT_COMPRESSED))
    }

    /**
     * Test that bundleFromAuthenticationResultForWebApps handles special characters in state
     */
    @Test
    @SneakyThrows
    fun testBundleFromAuthenticationResultForWebApps_WithSpecialCharactersInState_Success() {
        val mockCacheRecord = CacheRecord.builder()
            .account(MockRecords.getMockAccountRecord_AAD())
            .idToken(MockRecords.getMockIdTokenRecord_AAD())
            .accessToken(MockRecords.getMockAccessTokenRecord_AAD())
            .refreshToken(MockRecords.getMockRefreshTokenRecord_AAD())
            .build()

        val cacheRecords: MutableList<ICacheRecord> = ArrayList()
        cacheRecords.add(mockCacheRecord)

        val mockResult = LocalAuthenticationResult(
            mockCacheRecord,
            cacheRecords,
            SdkType.MSAL,
            false
        )

        val resultAdapter = getInstance()
        val mockState = "state_with_special_chars_!@#$%^&*()_+-={}[]|:;<>?,./"
        val protocolVersion = "10.0"

        val resultBundle = resultAdapter.bundleFromAuthenticationResultForWebApps(
            mockResult,
            protocolVersion,
            mockState
        )

        assertNotNull(resultBundle)
        assertTrue(resultBundle.containsKey(AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT_COMPRESSED))
    }

    /**
     * Test that bundleFromAuthenticationResultForWebApps handles username being ".MISSING_FROM_THE_TOKEN_RESPONSE"
     * In this case, username should be set to null in the WebAppsAccountItem
     * This scenario occurs during lookup mode requests where ESTS sets id token to "none"
     * and the username is not found in the cache
     */
    @Test
    @SneakyThrows
    fun testBundleFromAuthenticationResultForWebApps_WithMissingUsernameToken_SetsUsernameToNull() {
        // Create a custom account record with username set to the special "missing" value
        val accountRecordWithMissingUsername = AccountRecord()
        accountRecordWithMissingUsername.username = SchemaUtil.MISSING_FROM_THE_TOKEN_RESPONSE
        accountRecordWithMissingUsername.homeAccountId = "mock-home-account-id"
        accountRecordWithMissingUsername.localAccountId = "mock-local-account-id"
        accountRecordWithMissingUsername.clientInfo = "mock-client-info"
        accountRecordWithMissingUsername.environment = "login.microsoftonline.com"
        accountRecordWithMissingUsername.realm = "mock-tenant-id"

        val mockCacheRecord = CacheRecord.builder()
            .account(accountRecordWithMissingUsername)
            .idToken(MockRecords.getMockIdTokenRecord_AAD())
            .accessToken(MockRecords.getMockAccessTokenRecord_AAD())
            .refreshToken(MockRecords.getMockRefreshTokenRecord_AAD())
            .build()

        val cacheRecords: MutableList<ICacheRecord> = ArrayList()
        cacheRecords.add(mockCacheRecord)

        val mockResult = LocalAuthenticationResult(
            mockCacheRecord,
            cacheRecords,
            SdkType.MSAL,
            false
        )

        val resultAdapter = getInstance()
        val mockState = "lookup_mode_state"
        val protocolVersion = null // Use null to ensure no compression for easier inspection

        val resultBundle = resultAdapter.bundleFromAuthenticationResultForWebApps(
            mockResult,
            protocolVersion,
            mockState
        )

        assertNotNull(resultBundle)
        assertTrue(resultBundle.containsKey(AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT))

        val resultString = resultBundle.getString(AuthenticationConstants.Broker.BROKER_WEB_APPS_SUCCESSFUL_RESULT)
        assertNotNull(resultString)

        // Verify the JSON structure is valid and contains expected fields
        assertTrue(resultString!!.contains(WebAppsGetTokenSubOperationResponse.FIELD_STATE))
        assertTrue(resultString.contains(mockState))
        assertTrue(resultString.contains(WebAppsGetTokenSubOperationResponse.FIELD_ACCOUNT))
        assertTrue(resultString.contains(WebAppsAccountItem.FIELD_HOME_ACCOUNT_ID))

        // The username field in the account object should be null (represented as "username":null in JSON)
        // We verify that the special MISSING_FROM_THE_TOKEN_RESPONSE value is NOT in the result
        Assert.assertFalse(
            "Username should be null/absent, not the MISSING_FROM_THE_TOKEN_RESPONSE value",
            resultString.contains(SchemaUtil.MISSING_FROM_THE_TOKEN_RESPONSE)
        )
    }
}