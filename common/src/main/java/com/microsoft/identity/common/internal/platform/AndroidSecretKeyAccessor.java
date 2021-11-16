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

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.IKeyStoreKeyManager;
import com.microsoft.identity.common.java.crypto.IManagedKeyAccessor;
import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.exception.ClientException;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;

import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Accessors(prefix = "m")
public class AndroidSecretKeyAccessor extends SecretKeyAccessor implements IManagedKeyAccessor<KeyStore.SecretKeyEntry> {

    public AndroidSecretKeyAccessor(DeviceKeyManager<KeyStore.SecretKeyEntry> keyManager, CryptoSuite suite) {
        super(keyManager, suite);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public byte[] encrypt(@NonNull final byte[] plaintext) throws ClientException {
        return super.encrypt(plaintext);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public byte[] decrypt(@NonNull final byte[] ciphertext) throws ClientException {
        return super.decrypt(ciphertext);
    }

    @Override
    public byte[] sign(@NonNull final byte[] text) throws ClientException {
        return super.sign(text);
    }

    @Override
    public boolean verify(@NonNull final byte[] text, @NonNull final byte[] signature) throws ClientException {
        return Arrays.equals(signature, sign(text));
    }

    @Override
    public byte[] getThumbprint() throws ClientException {
        return mKeyManager.getThumbprint();
    }

    @Override
    public Certificate[] getCertificateChain() throws ClientException {
        return mKeyManager.getCertificateChain();
    }

    @Override
    public SecureHardwareState getSecureHardwareState() throws ClientException {
        return mKeyManager.getSecureHardwareState();
    }

    @Override
    public IKeyAccessor generateDerivedKey(final byte[] label, final byte[] ctx, final CryptoSuite suite) throws ClientException {
        throw new UnsupportedOperationException("This operation is not supported by inaccessible keys");
    }

    @Override
    public IKeyStoreKeyManager<KeyStore.SecretKeyEntry> getManager() {
        return mKeyManager;
    }
}
