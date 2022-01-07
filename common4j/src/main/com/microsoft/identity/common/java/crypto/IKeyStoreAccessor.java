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
package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.text.ParseException;

import edu.umd.cs.findbugs.annotations.NonNull;

public interface IKeyStoreAccessor {
    IKeyAccessor forAlias(@NonNull IPlatformComponents commonComponents, @NonNull String alias, @NonNull CryptoSuite suite)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException;

    IKeyAccessor newInstance(@NonNull IPlatformComponents commonComponents,
                             @NonNull IDevicePopManager.Cipher cipher,
                             @NonNull SigningAlgorithm signingAlg)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException;

    IKeyAccessor newInstance(@NonNull CryptoSuite cipher, @NonNull boolean needRawAccess)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException,
            NoSuchProviderException, InvalidAlgorithmParameterException;

    IKeyAccessor importSymmetricKey(@NonNull IPlatformComponents context,
                                    @NonNull CryptoSuite cipher,
                                    @NonNull String keyAlias,
                                    @NonNull String key_jwe,
                                    @NonNull IKeyAccessor stk_accessor)
            throws ParseException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, ClientException;
}
