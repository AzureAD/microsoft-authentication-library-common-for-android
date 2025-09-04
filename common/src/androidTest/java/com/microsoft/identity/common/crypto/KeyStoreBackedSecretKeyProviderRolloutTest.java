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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.util.FileUtil;
import com.microsoft.identity.common.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;

import javax.crypto.SecretKey;

/**
 * Instrumented tests for KeyStoreBackedSecretKeyProvider.
 * These tests run on real Android devices/emulators with access to the Android KeyStore.
 */
@RunWith(AndroidJUnit4.class)
public class KeyStoreBackedSecretKeyProviderRolloutTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final String MOCK_KEY_ALIAS = "MOCK_KEY_ALIAS_INSTRUMENTED";
    private final String MOCK_KEY_FILE_PATH = "MOCK_KEY_FILE_PATH_INSTRUMENTED";

    @BeforeClass
    public static void classSetUp() {
        Logger.setAndroidLogger();
        Logger.setAllowLogcat(true);
    }

    @Before
    public void setUp() throws Exception {
        // Clean slate for each test
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);
        FileUtil.deleteFile(getKeyFile());
    }

    /**
     * Creates a Phase 1 provider configuration for backward compatibility testing.
     * <p>
     * Phase 1 represents the baseline configuration with both new format flights disabled:
     * - ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING = false (uses PKCS1 padding)
     * - WRAPPED_SECRET_KEY_SERIALIZER_VERSION = 0 (uses legacy format without metadata)
     * <p>
     * This configuration is used to test scenarios where applications are running
     * with the original crypto implementation before any flight rollouts.
     *
     */
    private void enablePhase1Flights() {
        configureFlights(false, 0);
    }

    /**
     * Creates a Phase 2 provider configuration for intermediate rollout testing.
     * <p>
     * Phase 2 represents the intermediate configuration where only the new wrapped
     * secret key format flight is enabled:
     * - ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING = false (still uses PKCS1 padding)
     * - WRAPPED_SECRET_KEY_SERIALIZER_VERSION = 1 (uses new format with metadata)
     * <p>
     * This configuration is used to test scenarios where the new wrapped secret key
     * format has been rolled out but OAEP padding has not yet been enabled.
     * This allows testing backward compatibility with Phase 1 and forward compatibility
     * with Phase 3.
     *
     */
    private void enablePhase2Flights() {
        configureFlights(false, 1);
    }

    /**
     * Creates a Phase 3 provider configuration for full feature rollout testing.
     * <p>
     * Phase 3 represents the final configuration with both flights enabled:
     * - ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING = true (uses OAEP with SHA and MGF1 padding)
     * - WRAPPED_SECRET_KEY_SERIALIZER_VERSION = 1 (uses new format with metadata)
     * <p>
     * This configuration is used to test scenarios where all crypto enhancements
     * have been fully rolled out. It represents the target state for maximum
     * security and functionality.
     *
     */
    private void enablePhase3Flights() {
        configureFlights(true, 1);
    }

    /**
     * Helper method to configure flights with specified settings.
     * This method reduces code duplication across the enablePhase methods.
     *
     * @param enableOAEPWithSHAAndMGF1Padding whether to enable OAEP with SHA and MGF1 padding
     * @param wrappedSecretKeySerializerVersion the version of the wrapped secret key serializer (0=legacy, 1=JSON format)
     */
    private void configureFlights(final boolean enableOAEPWithSHAAndMGF1Padding,
                                  final int wrappedSecretKeySerializerVersion) {
        final IFlightsProvider mockFlightsProvider = createFlightsProvider(
                enableOAEPWithSHAAndMGF1Padding,
                wrappedSecretKeySerializerVersion);
        final IFlightsManager flightsManager = createFlightsManager(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(flightsManager);
    }

    /**
     * Creates a mock flights provider with the specified flight configurations.
     *
     * @param enableOAEPWithSHAAndMGF1Padding whether to enable OAEP with SHA and MGF1 padding
     * @param wrappedSecretKeySerializerVersion the version of the wrapped secret key serializer (0=legacy, 1=JSON format)
     * @return configured mock IFlightsProvider
     */
    private IFlightsProvider createFlightsProvider(final boolean enableOAEPWithSHAAndMGF1Padding,
                                                  final int wrappedSecretKeySerializerVersion) {
        final IFlightsProvider mockFlightsProvider = Mockito.mock(IFlightsProvider.class);

        // These flights are always enabled for key generation spec improvements
        Mockito.when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITH_PURPOSE_WRAP_KEY))
                .thenReturn(true);
        Mockito.when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_NEW_KEY_GEN_SPEC_FOR_WRAP_WITHOUT_PURPOSE_WRAP_KEY))
                .thenReturn(true);

        // Configure the phase-specific flights
        Mockito.when(mockFlightsProvider.isFlightEnabled(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))
                .thenReturn(enableOAEPWithSHAAndMGF1Padding);
        Mockito.when(mockFlightsProvider.getIntValue(CommonFlight.WRAPPED_SECRET_KEY_SERIALIZER_VERSION))
                .thenReturn(wrappedSecretKeySerializerVersion);

        return mockFlightsProvider;
    }

    /**
     * Creates a mock flights manager with the given flights provider.
     *
     * @param flightsProvider the flights provider to use
     * @return configured mock IFlightsManager
     */
    private IFlightsManager createFlightsManager(final IFlightsProvider flightsProvider) {
        return new IFlightsManager() {
            @Override
            public @NotNull IFlightsProvider getFlightsProvider(long waitForConfigsWithTimeoutInMs) {
                return flightsProvider;
            }

            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId, long waitForConfigsWithTimeoutInMs) {
                return flightsProvider;
            }

            @Override
            public @NotNull IFlightsProvider getFlightsProviderForTenant(@NotNull String tenantId) {
                return flightsProvider;
            }

            @NonNull
            @Override
            public IFlightsProvider getFlightsProvider() {
                return flightsProvider;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        AndroidKeyStoreUtil.deleteKey(MOCK_KEY_ALIAS);
        FileUtil.deleteFile(getKeyFile());
    }

    /**
     * Gets the file path for the secret key storage file.
     * This file is used to persist wrapped secret keys to disk.
     *
     * @return File object representing the key storage file location
     */
    private File getKeyFile() {
        return new File(
                context.getDir(context.getPackageName(), Context.MODE_PRIVATE),
                MOCK_KEY_FILE_PATH
        );
    }

    /**
     * Creates a KeyStoreBackedSecretKeyProvider instance with the standard test configuration.
     * This helper method eliminates code duplication across test methods.
     *
     * @return a new KeyStoreBackedSecretKeyProvider instance configured for testing
     */
    private KeyStoreBackedSecretKeyProvider createKeyProvider() {
        return new KeyStoreBackedSecretKeyProvider(
                context,
                MOCK_KEY_ALIAS,
                MOCK_KEY_FILE_PATH
        );
    }

    private void validateFileIsLegacyFormat() {
        final File keyFile = getKeyFile();
        Assert.assertTrue("Key file should exist after generation", keyFile.exists());
        final long fileSize = keyFile.length();
        Assert.assertEquals( 256, fileSize);
    }

    private void validateFileINewFormat() {
        final File keyFile = getKeyFile();
        Assert.assertTrue("Key file should exist after generation", keyFile.exists());
        final long fileSize = keyFile.length();
        Assert.assertTrue(fileSize > (long) 256);
    }

    /**
     * Validates that two keys are equivalent by comparing their properties.
     *
     * @param originalKey the original key
     * @param readKey the key read from storage
     * @param testDescription description for assertion messages
     */
    private void validateKeyEquivalence(
            final SecretKey originalKey,
            final SecretKey readKey,
            final String testDescription) {
        Assert.assertNotNull(testDescription + " key should not be null", readKey);

        final String originalThumbprint = KeyUtil.getKeyThumbPrint(originalKey);
        final String readThumbprint = KeyUtil.getKeyThumbPrint(readKey);

        Assert.assertEquals("Thumbprints should match for " + testDescription,
                originalThumbprint, readThumbprint);
        Assert.assertEquals("Keys should be same instance for " + testDescription,
                originalKey, readKey);

    }

    /**
     * Validates key file metadata contains or doesn't contain specific algorithm indicators.
     *
     * @param shouldContain algorithm string that should be present
     * @param shouldNotContain algorithm string that should not be present
     * @param testDescription description for assertion messages
     */
    private void validateKeyMetadata(String shouldContain, String shouldNotContain, String testDescription) {
        final File keyFile = getKeyFile();
        try {
            byte[] keyFileContent = java.nio.file.Files.readAllBytes(keyFile.toPath());
            String keyFileString = new String(keyFileContent, java.nio.charset.StandardCharsets.UTF_8);

            if (shouldContain != null) {
                Assert.assertTrue(testDescription + " metadata should contain " + shouldContain,
                        keyFileString.contains(shouldContain));
            }

            if (shouldNotContain != null) {
                Assert.assertFalse(testDescription + " metadata should not contain " + shouldNotContain, keyFileString.contains(shouldNotContain));
            }
        } catch (java.io.IOException e) {
            Assert.fail("Failed to read key file for metadata validation in " + testDescription + ": " + e.getMessage());
        }
    }

    @Test
    public void tesForwardCompatibility_Phase1to2() throws ClientException {
        // Test that a key created with phase 1 provider can be read by phase 2 provider
        enablePhase1Flights();
        final KeyStoreBackedSecretKeyProvider phase1KeyProvider = createKeyProvider();

        // Step 1: Create key with phase 1 provider
        final SecretKey phase1Key = phase1KeyProvider.generateNewSecretKey();
        // Validate legacy format file properties
        validateFileIsLegacyFormat();

        // Step 2: Try to read the same key with phase 2
        enablePhase2Flights();
        final KeyStoreBackedSecretKeyProvider phase2Provider = createKeyProvider();

        final SecretKey phase2Key = phase2Provider.readSecretKeyFromStorage();

        // Validate key equivalence
        validateKeyEquivalence(phase1Key, phase2Key, "phase 1 to phase 2 compatibility");
    }

    @Test
    public void testBackwardCompatibility_Phase2to1() throws ClientException {
        // Test that a key created with phase 2 provider can be read by phase 1 provider
        enablePhase2Flights();
        final KeyStoreBackedSecretKeyProvider phase2KeyProvider = createKeyProvider();

        // Step 1: Create key with phase 2 provider
        final SecretKey phase2Key = phase2KeyProvider.generateNewSecretKey();
        // Validate new format file properties
        validateFileINewFormat();

        // Step 2: Try to read the same key with phase 1
        enablePhase1Flights();
        final KeyStoreBackedSecretKeyProvider phase1Provider = createKeyProvider();
        final SecretKey phase1Key = phase1Provider.readSecretKeyFromStorage();

        // Validate key equivalence
        validateKeyEquivalence(phase2Key, phase1Key, "phase 2 to phase 1 backward compatibility");
    }

    @Test
    public void testForwardCompatibility_Phase2to3() throws ClientException {
        // Test that a key created with phase 2 provider can be read by phase 3 provider
        enablePhase2Flights();
        final KeyStoreBackedSecretKeyProvider phase2KeyProvider = createKeyProvider();

        // Step 1: Create key with phase 2 provider
        final SecretKey phase2Key = phase2KeyProvider.generateNewSecretKey();

        // Validate Phase 2 format file properties and algorithm metadata
        validateFileINewFormat();
        validateKeyMetadata("PKCS1", "OAEP", "Phase 2 key");

        // Step 2: Try to read the same key with phase 3
        enablePhase3Flights();
        final KeyStoreBackedSecretKeyProvider phase3Provider = createKeyProvider();

        final SecretKey phase3Key = phase3Provider.readSecretKeyFromStorage();

        // Validate key equivalence
        validateKeyEquivalence(phase2Key, phase3Key,"phase 2 to phase 3 forward compatibility");
    }

    @Test
    public void testBackwardCompatibility_Phase3to2() throws ClientException {
        // Test that a key created with phase 3 provider can be read by phase 2 provider
        enablePhase3Flights();
        final KeyStoreBackedSecretKeyProvider phase3KeyProvider = createKeyProvider();

        // Step 1: Create key with phase 3 provider
        final SecretKey phase3Key = phase3KeyProvider.generateNewSecretKey();
        Assert.assertNotNull("Provider key should not be null", phase3Key);

        // Validate Phase 3 format file properties
        validateFileINewFormat();
        validateKeyMetadata("OAEP", "PKCS1", "New Phase 3 key");


        // Step 2: Try to read the same key with phase 2
        enablePhase2Flights();
        final KeyStoreBackedSecretKeyProvider phase2Provider = createKeyProvider();
        final SecretKey phase2Key = phase2Provider.readSecretKeyFromStorage();

        // Validate key equivalence
        validateKeyEquivalence(phase3Key, phase2Key, "phase 3 to phase 2 backward compatibility");
    }
}
