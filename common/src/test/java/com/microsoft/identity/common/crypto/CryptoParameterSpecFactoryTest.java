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

import android.content.Context;
import android.os.Build;

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.IFlightConfig;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.spec.MGF1ParameterSpec;
import java.util.List;

import javax.crypto.spec.OAEPParameterSpec;

import lombok.NonNull;

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

    private Boolean oeapEnabled = true; // Simulate OAEP enabled by default
    private Boolean newKeyGenSpecWithWrapKeyEnabled = true; // Simulate new key gen spec with wrap key enabled
    private Boolean newKeyGenSpecWithoutWrapKeyEnabled = true; // Simulate new key gen spec without wrap key enabled

    @Before
    public void setUp() {

        // Setup mock flights provider
        mockFlightsProvider = new IFlightsProvider() {

            @Override
            public boolean isFlightEnabled(@NonNull IFlightConfig flightConfig) {
                if (flightConfig.getKey().equals(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING.getKey())) {
                    // Simulate OAEP enabled for testing
                    return oeapEnabled; // Change to false to test PKCS1 path
                } else if (flightConfig.getKey().equals(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY.getKey())) {
                    return newKeyGenSpecWithWrapKeyEnabled; // Simulate new key gen spec enabled
                } else if (flightConfig.getKey().equals(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY.getKey())) {
                    return newKeyGenSpecWithoutWrapKeyEnabled; // Simulate modern spec without wrap key enabled
                }
                return false;
            }

            @Override
            public boolean getBooleanValue(@NonNull IFlightConfig flightConfig) {
                return false;
            }

            @Override
            public int getIntValue(@NonNull IFlightConfig flightConfig) {
                return 0;
            }

            @Override
            public double getDoubleValue(@NonNull IFlightConfig flightConfig) {
                return 0;
            }

            @Override
            public String getStringValue(@NonNull IFlightConfig flightConfig) {
                return "";
            }

            @Override
            public JSONObject getJsonValue(@NonNull IFlightConfig flightConfig) {
                return null;
            }
        };


        mockContext = org.mockito.Mockito.mock(Context.class);
        // Create the instance to test
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);
    }



    @Test
    public void testGetPrioritizedCipherParameterSpec_WithOAEPEnabled() {
        // When OAEP is enabled
        //mockFlightsProvider.addFlight(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING.getKey(), "true");

        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS,mockFlightsProvider);

        // Get the prioritized specs
        List<CipherSpec> specs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs();

        // Verify we have 2 specs (OAEP and PKCS1) in that order
        assertEquals(2, specs.size());
        assertEquals("RSA/NONE/OAEPwithSHA-256andMGF1Padding", specs.get(0).getTransformation());
        assertEquals("RSA/ECB/PKCS1Padding", specs.get(1).getTransformation());
    }

    @Test
    public void testGetPrioritizedCipherParameterSpec_WithOAEPDisabled() {
        // When OAEP is disabled
        oeapEnabled = false;
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<CipherSpec> specs = cryptoParameterSpecFactory.getPrioritizedCipherParameterSpecs();

        // Verify we have only 1 spec (PKCS1)
        assertEquals(1, specs.size());
        assertEquals("RSA/ECB/PKCS1Padding", specs.get(0).getTransformation());
    }

    @Test
    public void testGetPrioritizedKeyGenParameterSpecs_AllFlagsEnabled() {
        // When all flags are enabled
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have 3 specs in the right order
        assertEquals(3, specs.size());
        assertEquals("modern_spec_with_wrap_key", specs.get(0).getDescription());
        assertEquals("modern_spec_without_wrap_key", specs.get(1).getDescription());
        assertEquals("legacy_key_gen_spec", specs.get(2).getDescription());
    }

    @Test
    public void testGetPrioritizedKeyGenParameterSpecs_WithoutPurposeWrapKeyOnly() {
        // When only WITHOUT_PURPOSE_WRAP_KEY flag is enabled
        newKeyGenSpecWithWrapKeyEnabled = false; // Simulate wrap key disabled
        newKeyGenSpecWithoutWrapKeyEnabled = true; // Simulate modern spec without wrap key enabled
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have 2 specs in the right order
        assertEquals(2, specs.size());
        assertEquals("modern_spec_without_wrap_key", specs.get(0).getDescription());
        assertEquals("legacy_key_gen_spec", specs.get(1).getDescription());
    }

    @Test
    public void testGetPrioritizedKeyGenParameterSpecs_NoFlagsEnabled() {
        // When no flags are enabled
        newKeyGenSpecWithWrapKeyEnabled = false; // Simulate wrap key disabled
        newKeyGenSpecWithoutWrapKeyEnabled = false; // Simulate modern spec without wrap key disabled
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have only the legacy spec
        assertEquals(1, specs.size());
        assertEquals("legacy_key_gen_spec", specs.get(0).getDescription());
    }


    @Test
    @Config(sdk = Build.VERSION_CODES.M) // API 23
    public void testGetPrioritizedKeyGenParameterSpecs_API23_WithFlags() {
        // Test on API 23 (M) with flags enabled
        // Should include modern spec without wrap key but not the one with wrap key (requires API 28)
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have 2 specs in the right order (no PURPOSE_WRAP_KEY since it needs API 28)
        assertEquals(2, specs.size());
        assertEquals("modern_spec_without_wrap_key", specs.get(0).getDescription());
        assertEquals("legacy_key_gen_spec", specs.get(1).getDescription());
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP) // API 21, before M
    public void testGetPrioritizedKeyGenParameterSpecs_LegacyAPI() {
        // Test on pre-M API where only legacy spec should be available
        // Re-create the factory with the updated flags
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have only the legacy spec regardless of flags
        assertEquals(1, specs.size());
        assertEquals("legacy_key_gen_spec", specs.get(0).getDescription());
    }

    @Test
    public void testGetAlgorithmParameterSpec_WithOAEPEnabled() {
        // When OAEP is enabled
        oeapEnabled = true;

        // Re-create the factory
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Call the method via reflection since it's private
        try {
            java.lang.reflect.Method method = CryptoParameterSpecFactory.class.getDeclaredMethod(
                    "getAlgorithmParameterSpec", int.class);
            method.setAccessible(true);

            Object spec = method.invoke(cryptoParameterSpecFactory, android.security.keystore.KeyProperties.PURPOSE_ENCRYPT);

            assertNotNull("Algorithm parameter spec should not be null", spec);
            assertTrue("Should return a KeyGenParameterSpec",
                    spec instanceof android.security.keystore.KeyGenParameterSpec);

            // For OAEP, it should include DIGEST_SHA256 and DIGEST_SHA1
            android.security.keystore.KeyGenParameterSpec keySpec =
                    (android.security.keystore.KeyGenParameterSpec) spec;

            // Unfortunately we can't directly check the digests, but we can verify
            // the padding is set correctly if OAEP is enabled
            java.lang.reflect.Field builderField = keySpec.getClass().getDeclaredField("mEncryptionPaddings");
            builderField.setAccessible(true);
            String[] paddings = (String[]) builderField.get(keySpec);

            assertEquals(1, paddings.length);
            assertEquals(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_RSA_OAEP, paddings[0]);

        } catch (Exception e) {
            fail("Failed to test getAlgorithmParameterSpec: " + e.getMessage());
        }
    }

    @Test
    public void testGetAlgorithmParameterSpec_WithOAEPDisabled() {
        // When OAEP is disabled
        oeapEnabled = false;

        // Re-create the factory
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Call the method via reflection since it's private
        try {
            java.lang.reflect.Method method = CryptoParameterSpecFactory.class.getDeclaredMethod(
                    "getAlgorithmParameterSpec", int.class);
            method.setAccessible(true);

            Object spec = method.invoke(cryptoParameterSpecFactory, android.security.keystore.KeyProperties.PURPOSE_ENCRYPT);

            assertNotNull("Algorithm parameter spec should not be null", spec);
            assertTrue("Should return a KeyGenParameterSpec",
                    spec instanceof android.security.keystore.KeyGenParameterSpec);

            // For PKCS1, it should include DIGEST_SHA256 and DIGEST_SHA512
            android.security.keystore.KeyGenParameterSpec keySpec =
                    (android.security.keystore.KeyGenParameterSpec) spec;

            // Unfortunately we can't directly check the digests, but we can verify
            // the padding is set correctly if OAEP is disabled
            java.lang.reflect.Field builderField = keySpec.getClass().getDeclaredField("mEncryptionPaddings");
            builderField.setAccessible(true);
            String[] paddings = (String[]) builderField.get(keySpec);

            assertEquals(1, paddings.length);
            assertEquals(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, paddings[0]);

        } catch (Exception e) {
            fail("Failed to test getAlgorithmParameterSpec: " + e.getMessage());
        }
    }

    @Test
    public void testGetLegacyKeyGenParamSpec() {
        // Test the legacy key generation parameter spec
        try {
            java.lang.reflect.Method method = CryptoParameterSpecFactory.class.getDeclaredMethod(
                    "getLegacyKeyGenParamSpec");
            method.setAccessible(true);

            Object spec = method.invoke(cryptoParameterSpecFactory);

            assertNotNull("Legacy key gen parameter spec should not be null", spec);
            assertTrue("Should return a KeyPairGeneratorSpec",
                    spec instanceof android.security.KeyPairGeneratorSpec);

            android.security.KeyPairGeneratorSpec legacySpec =
                    (android.security.KeyPairGeneratorSpec) spec;

            // Check key alias is correctly set
            java.lang.reflect.Field aliasField = legacySpec.getClass().getDeclaredField("mKeystoreAlias");
            aliasField.setAccessible(true);
            String alias = (String) aliasField.get(legacySpec);

            assertEquals(TEST_KEY_ALIAS, alias);

        } catch (Exception e) {
            fail("Failed to test getLegacyKeyGenParamSpec: " + e.getMessage());
        }
    }

    @Test
    public void testKeySize() {
        // Test that the default key size is set correctly (2048)
        try {
            java.lang.reflect.Method method = CryptoParameterSpecFactory.class.getDeclaredMethod(
                    "getAlgorithmParameterSpec", int.class);
            method.setAccessible(true);

            Object spec = method.invoke(cryptoParameterSpecFactory, android.security.keystore.KeyProperties.PURPOSE_ENCRYPT);

            assertNotNull("Algorithm parameter spec should not be null", spec);
            assertTrue("Should return a KeyGenParameterSpec",
                    spec instanceof android.security.keystore.KeyGenParameterSpec);

            android.security.keystore.KeyGenParameterSpec keySpec =
                    (android.security.keystore.KeyGenParameterSpec) spec;

            // Check key size field
            java.lang.reflect.Field keySizeField = keySpec.getClass().getDeclaredField("mKeySize");
            keySizeField.setAccessible(true);
            int keySize = (int) keySizeField.get(keySpec);

            assertEquals(2048, keySize);

        } catch (Exception e) {
            fail("Failed to test key size: " + e.getMessage());
        }
    }

    @Test
    public void testOAEPParameterSpec() {
        // Specifically test the OAEP spec configuration
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        try {
            // Access the oaepSpec field by reflection
            java.lang.reflect.Field oaepSpecField = CryptoParameterSpecFactory.class.getDeclaredField("oaepSpec");
            oaepSpecField.setAccessible(true);

            Object oaepSpec = oaepSpecField.get(null); // it's a static field

            assertNotNull("OAEP spec should not be null", oaepSpec);
            assertTrue("Should be an OAEPParameterSpec", oaepSpec instanceof OAEPParameterSpec);

            OAEPParameterSpec spec = (OAEPParameterSpec) oaepSpec;

            assertEquals("SHA-256", spec.getDigestAlgorithm());
            assertEquals("MGF1", spec.getMGFAlgorithm());
            assertTrue(spec.getMGFParameters() instanceof MGF1ParameterSpec);
            assertEquals(MGF1ParameterSpec.SHA1, spec.getMGFParameters());

        } catch (Exception e) {
            fail("Failed to test OAEP parameter spec: " + e.getMessage());
        }
    }

    @Test
    public void testWithPurposeWrapKeyOnly() {
        // Test with only PURPOSE_WRAP_KEY enabled
        newKeyGenSpecWithWrapKeyEnabled = true;
        newKeyGenSpecWithoutWrapKeyEnabled = false;

        // Re-create the factory
        cryptoParameterSpecFactory = new CryptoParameterSpecFactory(mockContext, TEST_KEY_ALIAS, mockFlightsProvider);

        // Get the prioritized specs
        List<KeyGenSpec> specs = cryptoParameterSpecFactory.getPrioritizedKeyGenParameterSpecs();

        // Verify we have 2 specs in the right order
        assertEquals(2, specs.size());
        assertEquals("modern_spec_with_wrap_key", specs.get(0).getDescription());
        assertEquals("legacy_key_gen_spec", specs.get(1).getDescription());
    }
}
