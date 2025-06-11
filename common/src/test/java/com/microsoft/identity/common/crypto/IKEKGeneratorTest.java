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

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
public class IKEKGeneratorTest {

    private static final String TEST_KEY_ALIAS = "test-kek-alias";

    @Mock
    private Context mMockContext;

    @Mock
    private IFlightsProvider mMockFlightsProvider;

    private IFlightsProvider mOriginalFlightsProvider;
    private IKekManager mKekManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Store original flights provider
        mOriginalFlightsProvider = CommonFlightsManager.INSTANCE.getFlightsProvider();

        // Set mock flights provider
        //CommonFlightsManager.INSTANCE.setFlightsProvider(mMockFlightsProvider);

        // Create KEKGenerator
        mKekManager = new AndroidKeyStoreRsaKekManager(TEST_KEY_ALIAS, mMockContext);
    }

    @After
    public void tearDown() {
        // Restore original flights provider
        //CommonFlightsManager.INSTANCE.setFlightsProvider(mOriginalFlightsProvider);
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

    // Note: The following test is commented out because it would require a real Android KeyStore
    // which is not available in unit tests. This would be better as an instrumented test.
    /*
    @Test
    public void testGenerateNewKeyPair() throws ClientException {
        // Enable OAEP with SHA and MGF1 padding
        when(mMockFlightsProvider.isFlightEnabled(eq(CommonFlight.ENABLE_OAEP_WITH_SHA_AND_MGF1_PADDING))).thenReturn(true);

        // Generate key pair
        KeyPair keyPair = mKekGenerator.generateNewKeyPair();

        // Verify key pair is not null
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
    }
    */
}
