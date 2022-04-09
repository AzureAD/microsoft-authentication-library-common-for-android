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

import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.exception.ClientException;

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
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.identity.common.java.crypto.IDevicePopManager.Cipher.RSA_ECB_OAEPWithSHA_1AndMGF1Padding;
import static com.microsoft.identity.common.java.crypto.IDevicePopManager.Cipher.RSA_ECB_OAEPWithSHA_256AndMGF1Padding;
import static com.microsoft.identity.common.java.crypto.IDevicePopManager.Cipher.RSA_ECB_PKCS1_PADDING;
import static com.microsoft.identity.common.java.crypto.IDevicePopManager.Cipher.RSA_NONE_OAEPWithSHA_1AndMGF1Padding;

// Note: Test cannot use robolectric due to the following open issue
// https://github.com/robolectric/robolectric/issues/1518
//todo: Investigate if these tests can be migrated to common4j
@RunWith(Parameterized.class)
public class AndroidDevicePoPManagerEncryptionTests {

    private static final String DATA_TO_ENCRYPT = "The quick brown fox jumped over the lazy dog.";

    private final IDevicePopManager devicePopManager;
    private final IDevicePopManager.Cipher cipher;

    @Parameterized.Parameters
    public static Iterable<IDevicePopManager.Cipher> testParams() {
        final List<IDevicePopManager.Cipher> ciphers = new ArrayList<>();

        // Only execute these tests at appropriate API levels...
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ciphers.add(RSA_ECB_PKCS1_PADDING);
        }

        //https://stackoverflow.com/questions/36015194/android-keystoreexception-unknown-error -
        //https://issuetracker.google.com/issues/37075898
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ciphers.add(RSA_NONE_OAEPWithSHA_1AndMGF1Padding);
            ciphers.add(RSA_ECB_OAEPWithSHA_1AndMGF1Padding);
            ciphers.add(RSA_ECB_OAEPWithSHA_256AndMGF1Padding);
        }

        return ciphers;
    }

    public AndroidDevicePoPManagerEncryptionTests(final IDevicePopManager.Cipher cipher)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        devicePopManager = new AndroidDevicePopManager(ApplicationProvider.getApplicationContext());
        this.cipher = cipher;
    }

    @Before
    public void setUp() throws ClientException {
        devicePopManager.generateAsymmetricKey();
    }

    @After
    public void tearDown() {
        devicePopManager.clearAsymmetricKey();
    }

    @Test
    public void testEncryption() throws ClientException {
        final String cipherText = devicePopManager.encrypt(cipher, DATA_TO_ENCRYPT);
        final String decryptedValue = devicePopManager.decrypt(cipher, cipherText);

        Assert.assertEquals(DATA_TO_ENCRYPT, decryptedValue);
        Assert.assertNotEquals(DATA_TO_ENCRYPT, cipherText);
    }
}
