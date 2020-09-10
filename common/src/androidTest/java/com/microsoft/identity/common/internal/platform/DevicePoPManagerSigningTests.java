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
package com.microsoft.identity.common.internal.platform;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.exception.ClientException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;

import static com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithms.MD5_WITH_RSA;
import static com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithms.NONE_WITH_RSA;
import static com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithms.SHA_256_WITH_RSA;
import static com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithms.SHA_256_WITH_RSA_PSS;
import static com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithms.SHA_384_WITH_RSA;
import static com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithms.SHA_384_WITH_RSA_PSS;
import static com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithms.SHA_512_WITH_RSA;
import static com.microsoft.identity.common.internal.platform.DevicePopManager.SigningAlgorithms.SHA_512_WITH_RSA_PSS;

@RunWith(Parameterized.class)
public class DevicePoPManagerSigningTests {

    private static final String DATA_TO_SIGN = "The quick brown fox jumped over the lazy dog.";

    private final IDevicePopManager devicePopManager;
    private final String signingAlg;

    @Parameterized.Parameters
    public static Iterable<Object[]> testParams() {
        return Arrays.asList(
                new Object[]{
                        MD5_WITH_RSA
                },
                new Object[]{
                        NONE_WITH_RSA
                },
                new Object[]{
                        SHA_256_WITH_RSA
                },
                new Object[]{
                        SHA_256_WITH_RSA_PSS
                },
                new Object[]{
                        SHA_384_WITH_RSA
                },
                new Object[]{
                        SHA_384_WITH_RSA_PSS
                },
                new Object[]{
                        SHA_512_WITH_RSA
                },
                new Object[]{
                        SHA_512_WITH_RSA_PSS
                }
        );
    }

    @SuppressWarnings("unused")
    public DevicePoPManagerSigningTests(final String signingAlg)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        devicePopManager = new DevicePopManager();
        this.signingAlg = signingAlg;
    }

    @Before
    public void setUp() throws ClientException {
        devicePopManager.generateAsymmetricKey(ApplicationProvider.getApplicationContext());
    }

    @After
    public void tearDown() {
        devicePopManager.clearAsymmetricKey();
    }

    @Test
    public void testSigning() throws ClientException {
        final String sampleSignature = devicePopManager.sign(signingAlg, DATA_TO_SIGN);
        Assert.assertTrue(devicePopManager.verify(signingAlg, DATA_TO_SIGN, sampleSignature));
    }
}
