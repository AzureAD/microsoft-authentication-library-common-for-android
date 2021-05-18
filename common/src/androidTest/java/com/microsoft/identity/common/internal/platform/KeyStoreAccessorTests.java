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
package com.microsoft.identity.common.internal.platform;

import android.content.Context;
import android.os.Build;

import androidx.test.InstrumentationRegistry;

import com.microsoft.identity.common.exception.ClientException;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

public class KeyStoreAccessorTests {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    public void testSymmetricBasicFunctionalitySuccessfulRawKey() throws Exception {
        KeyAccessor accessor = KeyStoreAccessor.newInstance(SymmetricCipher.AES_GCM_NONE_HMACSHA256, true);
        byte[] in = new byte[1024];
        RANDOM.nextBytes(in);
        byte[] out = accessor.encrypt(in);
        byte[] around = accessor.decrypt(out);
        Assert.assertArrayEquals(in, around);
        Assert.assertNull(accessor.getCertificateChain());
        Assert.assertEquals(SecureHardwareState.FALSE, accessor.getSecureHardwareState());
    }

    @Test
    public void testSymmetricBasicFunctionalitySuccessful() throws Exception {
        KeyAccessor accessor = KeyStoreAccessor.newInstance(SymmetricCipher.AES_GCM_NONE_HMACSHA256, false);
        byte[] in = new byte[1024];
        RANDOM.nextBytes(in);
        byte[] out = accessor.encrypt(in);
        byte[] around = accessor.decrypt(out);
        Assert.assertArrayEquals(in, around);
        Assert.assertNull(accessor.getCertificateChain());
        Assert.assertTrue(accessor.getSecureHardwareState() instanceof SecureHardwareState);
    }

    @Test
    public void testAsymmetricBasicFunctionalitySuccessful() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        KeyAccessor accessor = KeyStoreAccessor.newInstance(context, IDevicePopManager.Cipher.RSA_ECB_PKCS1_PADDING, IDevicePopManager.SigningAlgorithm.SHA_256_WITH_RSA);
        byte[] in = new byte[245];
        RANDOM.nextBytes(in);
        byte[] out = accessor.encrypt(in);
        byte[] around = accessor.decrypt(out);
        Assert.assertArrayEquals(in, around);
        byte[] signed = accessor.sign(in);
        Assert.assertTrue(accessor.verify(in, signed));
    }

    @Test(expected = ClientException.class)
    public void testBasicFunctionalityDecryptDoesSomething() throws Exception {
        KeyAccessor accessor = KeyStoreAccessor.newInstance(SymmetricCipher.AES_GCM_NONE_HMACSHA256, false);
        byte[] in = new byte[1024];
        RANDOM.nextBytes(in);
        byte[] out = accessor.encrypt(in);
        out[700] ^= out[700]; //I don't care what value it is as long as it's changed.
        byte[] around = accessor.decrypt(out);
    }

    @Test
    public void testBasicFunctionalityUnsupportedSign() throws Exception {
        boolean exception = false;
        try {
            final KeyAccessor accessor = KeyStoreAccessor.newInstance(SymmetricCipher.AES_GCM_NONE_HMACSHA256, false);
            final byte[] in = new byte[1024];
            RANDOM.nextBytes(in);
            final byte[] out = accessor.sign(in);
        } catch (UnsupportedOperationException e) {
            exception = true;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                throw new AssertionError(e);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !exception) {
            throw new AssertionError("Expected the sign operation to be unsupported in this API level: " + Build.VERSION.SDK_INT);
        }
    }

    @Test
    public void testBasicFunctionalityUnsupportedVerify() throws Exception {
        boolean exception = false;
        try {
            final KeyAccessor accessor = KeyStoreAccessor.newInstance(SymmetricCipher.AES_GCM_NONE_HMACSHA256, false);
            final byte[] in = new byte[1024];
            RANDOM.nextBytes(in);
            accessor.verify(in, in);
        } catch (UnsupportedOperationException e) {
            exception = true;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                throw new AssertionError(e);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !exception) {
            throw new AssertionError("Expected the verify operation to be unsupported in this API level: " + Build.VERSION.SDK_INT);
        }
    }
    @Test
    public void testBasicFunctionalitySignAndVerifySupportedIfRaw() throws Exception {
        KeyAccessor accessor = KeyStoreAccessor.newInstance(SymmetricCipher.AES_GCM_NONE_HMACSHA256, true);
        byte[] in = new byte[1024];
        RANDOM.nextBytes(in);
        byte[] out = accessor.sign(in);
        Assert.assertTrue(accessor.verify(in, out));
    }

}
