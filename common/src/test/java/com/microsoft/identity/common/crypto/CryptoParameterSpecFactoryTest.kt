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
package com.microsoft.identity.common.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [CryptoParameterSpecFactory]
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P]) // Targeting Android 9.0 (API 28) for the tests
class CryptoParameterSpecFactoryTest {
    @Mock
    private var mockContext: Context? = null

    private var mockFlightsProvider: IFlightsProvider? = null

    private var cryptoParameterSpecFactory: CryptoParameterSpecFactory? = null


    @Before
    fun setUp() {
        // Setup mock flights provider
        mockFlightsProvider = Mockito.mock(IFlightsProvider::class.java)
        mockContext = Mockito.mock(Context::class.java)
    }


    @Test
    fun testGetPrioritizedCipherParameterSpec() {
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!,
            TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        // Get the prioritized specs
        val specs = cryptoParameterSpecFactory!!.getPrioritizedCipherParameterSpecs()

        // Verify we have 2 specs (OAEP and PKCS1) in that order
        Assert.assertEquals(2, specs.size)
        Assert.assertEquals("RSA/NONE/OAEPwithSHA-256andMGF1Padding", specs[0].transformation)
        Assert.assertEquals("RSA/ECB/PKCS1Padding", specs[1].transformation)
    }

    @Test
    fun testGetPrioritizedKeyGenParameterSpecs_AllFlagsEnabled() {
        // When all flags are enabled
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))
            .thenReturn(true)
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!, TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        // Get the prioritized specs
        val specs = cryptoParameterSpecFactory!!.getPrioritizedKeyGenParameterSpecs()

        // Verify we have 3 specs in the right order
        Assert.assertEquals(3, specs.size.toLong())
        Assert.assertEquals("modern_spec_with_wrap_key", specs[0].description)
        Assert.assertEquals(
            listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP),
            specs[0].encryptionPaddings
        )

        Assert.assertEquals("modern_spec_without_wrap_key", specs[1].description)
        Assert.assertEquals(
            listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP),
            specs[1].encryptionPaddings
        )

        Assert.assertEquals("legacy_key_gen_spec", specs[2].description)
        Assert.assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1), specs[2].encryptionPaddings)
    }

    @Test
    fun testGetPrioritizedKeyGenParameterSpecs_WithoutPurposeWrapKeyOnly() {
        // When only WITHOUT_PURPOSE_WRAP_KEY flag is enabled
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!, TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        // Get the prioritized specs
        val specs = cryptoParameterSpecFactory!!.getPrioritizedKeyGenParameterSpecs()

        // Verify we have 2 specs in the right order
        Assert.assertEquals(2, specs.size.toLong())
        Assert.assertEquals("modern_spec_without_wrap_key", specs[0].description)
        Assert.assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1), specs[0].encryptionPaddings)

        Assert.assertEquals("legacy_key_gen_spec", specs[1].description)
        Assert.assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1), specs[1].encryptionPaddings)
    }

    @Test
    fun testGetPrioritizedKeyGenParameterSpecs_WithPurposeWrapKeyOnly() {
        // When only WITHOUT_PURPOSE_WRAP_KEY flag is enabled
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!, TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        // Get the prioritized specs
        val specs = cryptoParameterSpecFactory!!.getPrioritizedKeyGenParameterSpecs()

        // Verify we have 2 specs in the right order
        Assert.assertEquals(2, specs.size.toLong())
        Assert.assertEquals("modern_spec_with_wrap_key", specs[0].description)
        Assert.assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1), specs[0].encryptionPaddings)

        Assert.assertEquals("legacy_key_gen_spec", specs[1].description)
        Assert.assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1), specs[1].encryptionPaddings)
    }

    @Test
    fun testGetPrioritizedKeyGenParameterSpecs_NoFlagsEnabled() {
        // When no flags are enabled
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!, TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        // Get the prioritized specs
        val specs = cryptoParameterSpecFactory!!.getPrioritizedKeyGenParameterSpecs()

        // Verify we have only the legacy spec
        Assert.assertEquals(1, specs.size.toLong())
        Assert.assertEquals("legacy_key_gen_spec", specs[0].description)
        Assert.assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1), specs[0].encryptionPaddings)
    }


    @Test
    @Config(sdk = [Build.VERSION_CODES.M]) // API 23
    fun testGetPrioritizedKeyGenParameterSpecs_API23_WithFlags() {
        // Test on API 23 (M) with flags enabled
        // Should include modern spec without wrap key but not the one with wrap key (requires API 28)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))
            .thenReturn(true)
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!, TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        // Get the prioritized specs
        val specs = cryptoParameterSpecFactory!!.getPrioritizedKeyGenParameterSpecs()

        // Verify we have 2 specs in the right order (no PURPOSE_WRAP_KEY since it needs API 28)
        Assert.assertEquals(2, specs.size.toLong())
        Assert.assertEquals("modern_spec_without_wrap_key", specs[0].description)
        Assert.assertEquals(
            listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP),
            specs[0].encryptionPaddings
        )

        Assert.assertEquals("legacy_key_gen_spec", specs[1].description)
        Assert.assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1), specs[1].encryptionPaddings)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // API 28 (a legacy, <= 30, device)
    fun testGetPrioritizedKeyGenParameterSpecs_ConservativeFixEnabled_LegacyDevice_SkipsAdvancedSpecs() {
        // When the legacy-device fix is enabled on API <= 30, the advanced specs must be skipped
        // and the conservative spec elected instead (this is the regression fix).
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_CONSERVATIVE_KEY_GEN_SPEC_FOR_LEGACY_DEVICES))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!, TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        val specs = cryptoParameterSpecFactory!!.getPrioritizedKeyGenParameterSpecs()

        // Only the conservative spec and the legacy fallback should be present (no advanced specs).
        Assert.assertEquals(2, specs.size.toLong())
        Assert.assertEquals(CONSERVATIVE_SPEC, specs[0].description)
        Assert.assertEquals(listOf(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1), specs[0].encryptionPaddings)
        Assert.assertEquals("legacy_key_gen_spec", specs[1].description)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM]) // API 35 (>= 31)
    fun testGetPrioritizedKeyGenParameterSpecs_ConservativeFixEnabled_ModernDevice_IncludesAdvancedAndConservative() {
        // On API >= 31 the advanced specs are supported, so they are elected alongside the
        // conservative spec and the legacy fallback even when the fix is enabled.
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_CONSERVATIVE_KEY_GEN_SPEC_FOR_LEGACY_DEVICES))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!, TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        val specs = cryptoParameterSpecFactory!!.getPrioritizedKeyGenParameterSpecs()

        Assert.assertEquals(4, specs.size.toLong())
        Assert.assertEquals("modern_spec_with_wrap_key", specs[0].description)
        Assert.assertEquals("modern_spec_without_wrap_key", specs[1].description)
        Assert.assertEquals(CONSERVATIVE_SPEC, specs[2].description)
        Assert.assertEquals("legacy_key_gen_spec", specs[3].description)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // API 28 (a legacy, <= 30, device)
    fun testGetPrioritizedKeyGenParameterSpecs_ConservativeFixDisabled_LegacyDevice_PreservesAdvancedSpecs() {
        // When the fix flight is OFF (ECS kill-switch), the previous behaviour must be restored:
        // advanced specs are still offered on API <= 30 and no conservative spec is added.
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_CONSERVATIVE_KEY_GEN_SPEC_FOR_LEGACY_DEVICES))
            .thenReturn(false)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        Mockito.`when`(mockFlightsProvider!!.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
            .thenReturn(true)
        cryptoParameterSpecFactory = CryptoParameterSpecFactory(
            mockContext!!, TEST_KEY_ALIAS,
            mockFlightsProvider!!
        )

        val specs = cryptoParameterSpecFactory!!.getPrioritizedKeyGenParameterSpecs()

        // API 28 satisfies the wrap-key gate, so both advanced specs + legacy, no conservative.
        Assert.assertEquals(3, specs.size.toLong())
        Assert.assertEquals("modern_spec_with_wrap_key", specs[0].description)
        Assert.assertEquals("modern_spec_without_wrap_key", specs[1].description)
        Assert.assertEquals("legacy_key_gen_spec", specs[2].description)
        Assert.assertFalse(specs.any { it.description == CONSERVATIVE_SPEC })
    }

    companion object {
        private const val TEST_KEY_ALIAS = "test_key_alias"
        private const val CONSERVATIVE_SPEC = "conservative_spec_for_api_30_and_below"
    }
}
