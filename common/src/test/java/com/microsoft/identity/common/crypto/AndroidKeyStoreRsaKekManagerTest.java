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

import com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link AndroidKeyStoreRsaKekManager}
 * focusing on different padding modes and feature flag behavior.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AndroidKeyStoreRsaKekManagerTest {

    private static final String TEST_KEY_ALIAS = "test-kek-manager-key";

    @Mock
    private Context mMockContext;

    @Mock
    private IFlightsProvider mMockFlightsProvider;

    private IFlightsProvider mOriginalFlightsProvider;
    private AndroidKeyStoreRsaKekManager mKekManager;
    private KeyPair mMockKeyPair;
    private SecretKey mMockSecretKey;

    @Before
    public void setUp() throws NoSuchAlgorithmException {
        MockitoAnnotations.initMocks(this);

        // Store original flights provider
        mOriginalFlightsProvider = CommonFlightsManager.INSTANCE.getFlightsProvider();

        // Set mock flights provider
        //CommonFlightsManager.INSTANCE.setFlightsProvider(mMockFlightsProvider);

        // Create a real KeyPair for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        mMockKeyPair = keyPairGenerator.generateKeyPair();

        // Create a mock SecretKey
        mMockSecretKey = mock(SecretKey.class);
        when(mMockSecretKey.getAlgorithm()).thenReturn("AES");

        // Create AndroidKeyStoreRsaKekManager
        mKekManager = new AndroidKeyStoreRsaKekManager(TEST_KEY_ALIAS, mMockContext);
    }

    @After
    public void tearDown() {
        // Restore original flights provider
       // CommonFlightsManager.INSTANCE.setFlightsProvider(mOriginalFlightsProvider);
    }

    @Test
    public void testGetCipherTransformation_WithOAEPEnabled() {
        // Enable OAEP with SHA and MGF1 padding
        when(mMockFlightsProvider.isFlightEnabled(eq(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))).thenReturn(true);

        // Test transformation
        String transformation = mKekManager.getCipherTransformation();
        assertEquals("RSA/NONE/OAEPwithSHA-1andMGF1Padding", transformation);
    }

    @Test
    public void testGetCipherTransformation_WithOAEPDisabled() {
        // Disable OAEP with SHA and MGF1 padding
        when(mMockFlightsProvider.isFlightEnabled(eq(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))).thenReturn(false);

        // Test transformation
        String transformation = mKekManager.getCipherTransformation();
        assertEquals("RSA/ECB/PKCS1Padding", transformation);
    }

    @Test
    public void testKekExists() throws ClientException {
        try (MockedStatic<AndroidKeyStoreUtil> mockKeyStoreUtil = Mockito.mockStatic(AndroidKeyStoreUtil.class)) {
            // Set up mock to return keypair
            mockKeyStoreUtil.when(() -> AndroidKeyStoreUtil.readKey(anyString())).thenReturn(mMockKeyPair);

            // Test wrapper key exists
            boolean exists = mKekManager.kekExists();
            assertTrue(exists);
        }
    }

    @Test
    public void testKekDoesNotExist() throws ClientException {
        try (MockedStatic<AndroidKeyStoreUtil> mockKeyStoreUtil = Mockito.mockStatic(AndroidKeyStoreUtil.class)) {
            // Set up mock to return null
            mockKeyStoreUtil.when(() -> AndroidKeyStoreUtil.readKey(anyString())).thenReturn(null);

            // Test wrapper key does not exist
            boolean exists = mKekManager.kekExists();
            assertFalse(exists);
        }
    }

    @Test
    public void testUnwrapKey_FallbackChain() throws ClientException {
        try (MockedStatic<AndroidKeyStoreUtil> mockKeyStoreUtil = Mockito.mockStatic(AndroidKeyStoreUtil.class)) {
            // Enable OAEP padding
            when(mMockFlightsProvider.isFlightEnabled(eq(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))).thenReturn(true);

            // Set up mock to return keypair
            mockKeyStoreUtil.when(() -> AndroidKeyStoreUtil.readKey(anyString())).thenReturn(mMockKeyPair);

            // Create a wrapped key (simulated)
            byte[] wrappedKey = new byte[256];

            // First: primary transformation fails
            mockKeyStoreUtil.when(() -> AndroidKeyStoreUtil.unwrap(
                    eq(wrappedKey),
                    eq("AES"),
                    eq(mMockKeyPair),
                    eq("RSA/NONE/OAEPwithSHA-1andMGF1Padding")))
                    .thenThrow(new ClientException("Incompatible padding mode", "Test exception"));

            // Second: SHA-1 fallback fails
            mockKeyStoreUtil.when(() -> AndroidKeyStoreUtil.unwrap(
                    eq(wrappedKey),
                    eq("AES"),
                    eq(mMockKeyPair),
                    eq("RSA/NONE/OAEPwithSHA-1andMGF1Padding")))
                    .thenThrow(new ClientException("Incompatible padding mode", "Test exception"));

            // Third: PKCS1Padding succeeds
            mockKeyStoreUtil.when(() -> AndroidKeyStoreUtil.unwrap(
                    eq(wrappedKey),
                    eq("AES"),
                    eq(mMockKeyPair),
                    eq("RSA/ECB/PKCS1Padding")))
                    .thenReturn(mMockSecretKey);

            // Test unwrap key
            SecretKey unwrappedKey = mKekManager.unwrapKey(wrappedKey, "AES");

            // Verify the unwrapped key is the same as our mock
            assertSame(mMockSecretKey, unwrappedKey);
        }
    }

    @Test
    public void testWrapKey() throws ClientException {
        try (MockedStatic<AndroidKeyStoreUtil> mockKeyStoreUtil = Mockito.mockStatic(AndroidKeyStoreUtil.class)) {
            // Set up mock to return keypair
            mockKeyStoreUtil.when(() -> AndroidKeyStoreUtil.readKey(anyString())).thenReturn(mMockKeyPair);

            // Set up mock for wrap operation
            byte[] expectedWrappedKey = new byte[256];
            mockKeyStoreUtil.when(() -> AndroidKeyStoreUtil.wrap(eq(mMockSecretKey), eq(mMockKeyPair), anyString()))
                    .thenReturn(expectedWrappedKey);

            // Test wrap key
            byte[] wrappedKey = mKekManager.wrapKey(mMockSecretKey);

            // Verify the wrapped key
            assertSame(expectedWrappedKey, wrappedKey);
        }
    }
}
