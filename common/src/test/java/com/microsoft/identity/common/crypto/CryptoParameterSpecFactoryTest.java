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
package com.microsoft.identity.common.crypto;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyProperties;

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Unit tests for {@link CryptoParameterSpecFactory}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P}) // Targeting Android 9.0 (API 28) for the tests
public class CryptoParameterSpecFactoryTest {

    private static final String TEST_KEY_ALIAS = "test_key_alias";

    @Mock
    private Context mockContext;

    private IFlightsProvider mockFlightsProvider;

    private CryptoParameterSpecFactory cryptoParameterSpecFactory;


    @Before
    public void setUp() {
        // Setup mock flights provider
        mockFlightsProvider = Mockito.mock(IFlightsProvider.class);
        mockContext = Mockito.mock(Context.class);
    }


    @Test
    public void testGetPrioritizedCipherParameterSpec() {
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        final List<CipherSpec> specs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs();

        // Verify we have 2 specs (OAEP and PKCS1) in that order
        assertEquals(2, specs.size());
        assertEquals("RSA/NONE/OAEPwithSHA-256andMGF1Padding", specs.get(0).getTransformation());
        assertEquals("RSA/ECB/PKCS1Padding", specs.get(1).getTransformation());
    }

    @Test
    public void testGetPrioritizedKeyGenParameterSpecs_AllFlagsEnabled() {
        // When all flags are enabled
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))
                .thenReturn(true);
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        final List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have 3 specs in the right order
        assertEquals(3, specs.size());
        assertEquals("modern_spec_with_wrap_key", specs.get(0).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP, specs.get(0).getEncryptionPadding());

        assertEquals("modern_spec_without_wrap_key", specs.get(1).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP, specs.get(1).getEncryptionPadding());

        assertEquals("legacy_key_gen_spec", specs.get(2).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, specs.get(2).getEncryptionPadding());

    }

    @Test
    public void testGetPrioritizedKeyGenParameterSpecs_WithoutPurposeWrapKeyOnly() {
        // When only WITHOUT_PURPOSE_WRAP_KEY flag is enabled
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        final List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have 2 specs in the right order
        assertEquals(2, specs.size());
        assertEquals("modern_spec_without_wrap_key", specs.get(0).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, specs.get(0).getEncryptionPadding());

        assertEquals("legacy_key_gen_spec", specs.get(1).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, specs.get(1).getEncryptionPadding());
    }

    @Test
    public void testGetPrioritizedKeyGenParameterSpecs_WithPurposeWrapKeyOnly() {
        // When only WITHOUT_PURPOSE_WRAP_KEY flag is enabled
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        final List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have 2 specs in the right order
        assertEquals(2, specs.size());
        assertEquals("modern_spec_with_wrap_key", specs.get(0).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, specs.get(0).getEncryptionPadding());

        assertEquals("legacy_key_gen_spec", specs.get(1).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, specs.get(1).getEncryptionPadding());
    }

    @Test
    public void testGetPrioritizedKeyGenParameterSpecs_NoFlagsEnabled() {
        // When no flags are enabled
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        final List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have only the legacy spec
        assertEquals(1, specs.size());
        assertEquals("legacy_key_gen_spec", specs.get(0).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, specs.get(0).getEncryptionPadding());
    }


    @Test
    @Config(sdk = Build.VERSION_CODES.M) // API 23
    public void testGetPrioritizedKeyGenParameterSpecs_API23_WithFlags() {
        // Test on API 23 (M) with flags enabled
        // Should include modern spec without wrap key but not the one with wrap key (requires API 28)
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))
                .thenReturn(true);
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have 2 specs in the right order (no PURPOSE_WRAP_KEY since it needs API 28)
        assertEquals(2, specs.size());
        assertEquals("modern_spec_without_wrap_key", specs.get(0).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP, specs.get(0).getEncryptionPadding());

        assertEquals("legacy_key_gen_spec", specs.get(1).getDescription());
        assertEquals(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, specs.get(1).getEncryptionPadding());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1) // API 22, before M
    public void testGetPrioritizedKeyGenParameterSpecs_LegacyAPI() {
        // Test on pre-M API where only legacy spec should be available
        // Should include modern spec without wrap key but not the one with wrap key (requires API 28)
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))
                .thenReturn(true);
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have only the legacy spec regardless of flags
        assertEquals(1, specs.size());
        assertEquals("legacy_key_gen_spec", specs.get(0).getDescription());
    }
}
