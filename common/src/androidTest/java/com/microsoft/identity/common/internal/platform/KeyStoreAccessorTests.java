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

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

public class KeyStoreAccessorTests {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    public void testBasicFunctionality() throws Exception {
        KeyAccessor accessor = KeyStoreAccessor.newInstance(SymmetricCipher.AES_GCM_NONE_HMACSHA256);
        byte[] in = new byte[1024];
        RANDOM.nextBytes(in);
        byte[] out = accessor.encrypt(in);
        byte[] around = accessor.decrypt(out);
        Assert.assertArrayEquals(in, around);

        byte[] signature = accessor.sign(in, IDevicePopManager.SigningAlgorithm.SHA_256_WITH_RSA);
        boolean verified = accessor.verify(in, IDevicePopManager.SigningAlgorithm.SHA_256_WITH_RSA, signature);
        Assert.assertTrue(verified);
    }
}
