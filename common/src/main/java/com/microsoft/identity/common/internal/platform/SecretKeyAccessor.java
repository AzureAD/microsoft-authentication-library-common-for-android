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

import com.microsoft.identity.common.exception.ClientException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import lombok.Builder;
import lombok.experimental.Accessors;

import static com.microsoft.identity.common.exception.ClientException.BAD_PADDING;
import static com.microsoft.identity.common.exception.ClientException.INVALID_ALG_PARAMETER;
import static com.microsoft.identity.common.exception.ClientException.INVALID_BLOCK_SIZE;
import static com.microsoft.identity.common.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.exception.ClientException.INVALID_PROTECTION_PARAMS;
import static com.microsoft.identity.common.exception.ClientException.IO_ERROR;
import static com.microsoft.identity.common.exception.ClientException.KEYSTORE_NOT_INITIALIZED;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.exception.ClientException.NO_SUCH_PADDING;

@Builder
@Accessors(prefix = "m")
public class SecretKeyAccessor implements KeyAccessor {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final DeviceKeyManager<KeyStore.SecretKeyEntry> mKeyManager;
    private final CryptoSuite suite;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public byte[] encrypt(byte[] plaintext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            SecretKey key = entry.getSecretKey();
            Cipher c = Cipher.getInstance(suite.cipherName());
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = c.getIV();
            byte[] enc = c.doFinal(plaintext);
            byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return out;
        } catch (final UnrecoverableEntryException e) {
            errCode = INVALID_PROTECTION_PARAMS;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final KeyStoreException e) {
            errCode = KEYSTORE_NOT_INITIALIZED;
            exception = e;
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        } catch (BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        } /* catch (InvalidAlgorithmParameterException e) {
            errCode = INVALID_ALG_PARAMETER;
            exception = e;
        }*/
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public byte[] decrypt(byte[] ciphertext) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            SecretKey key = entry.getSecretKey();
            Cipher c = Cipher.getInstance(suite.cipherName());
            final GCMParameterSpec ivSpec = new GCMParameterSpec(128, ciphertext, 0, 12);
            c.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] out = Arrays.copyOfRange(ciphertext, 12, ciphertext.length);
            return c.doFinal(out);
        } catch (final UnrecoverableEntryException e) {
            errCode = INVALID_PROTECTION_PARAMS;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final KeyStoreException e) {
            errCode = KEYSTORE_NOT_INITIALIZED;
            exception = e;
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        } catch (BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        } catch (InvalidAlgorithmParameterException e) {
            errCode = INVALID_ALG_PARAMETER;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    public byte[] sign(byte[] text, IDevicePopManager.SigningAlgorithm alg) throws ClientException {
        final String errCode;
        final Exception exception;
        try {
            final KeyStore.SecretKeyEntry entry = mKeyManager.getEntry();
            SecretKey key = entry.getSecretKey();
            Mac c = Mac.getInstance(suite.macName());
            c.init(key);
            return c.doFinal(text);
        } catch (final UnrecoverableEntryException e) {
            errCode = INVALID_PROTECTION_PARAMS;
            exception = e;
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final KeyStoreException e) {
            errCode = KEYSTORE_NOT_INITIALIZED;
            exception = e;
        } catch (InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        }
        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    public boolean verify(byte[] text, IDevicePopManager.SigningAlgorithm alg, byte[] signature) throws ClientException {
        return Arrays.equals(signature, sign(text, alg));
    }

    @Override
    public byte[] getThumprint() throws ClientException {
        return mKeyManager.getThumbprint();
    }
}
