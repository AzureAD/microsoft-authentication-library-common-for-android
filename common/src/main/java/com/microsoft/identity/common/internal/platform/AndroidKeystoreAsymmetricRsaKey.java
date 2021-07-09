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

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.crypto.SigningAlgorithm;
import com.microsoft.identity.common.java.exception.ClientException;

import java.security.cert.Certificate;
import java.util.Date;

/**
 * Represents an asymmetric key, backed by the Android Keystore.
 */
public class AndroidKeystoreAsymmetricRsaKey implements AsymmetricRsaKey {

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static final IDevicePopManager.Cipher RSA_ECB_PKCS_1_PADDING = IDevicePopManager.Cipher.RSA_ECB_PKCS1_PADDING;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static final SigningAlgorithm SHA_256_WITH_RSA = SigningAlgorithm.SHA_256_WITH_RSA;

    /**
     * The {@link IDevicePopManager} to which we will delegate most cryptographic actions.
     */
    private final IDevicePopManager mDevicePopManager;

    /**
     * The alias is name of this keypair in the underlying keystore.
     */
    private final String mAlias;

    /**
     * Constructs a new {@link AndroidKeystoreAsymmetricRsaKey} instance.
     *
     * @param context    The application Context.
     * @param popManager The underlying {@link IDevicePopManager} to which we'll delegate.
     * @throws ClientException If asymmetric key generation fails.
     */
    AndroidKeystoreAsymmetricRsaKey(@NonNull final Context context,
                                    @NonNull final IDevicePopManager popManager,
                                    @NonNull final String alias)
            throws ClientException {
        mDevicePopManager = popManager;
        mAlias = alias;

        if (!mDevicePopManager.asymmetricKeyExists()) {
            mDevicePopManager.generateAsymmetricKey(context);
        }
    }

    @Override
    public String getAlias() {
        return mAlias;
    }

    @Override
    public Date getCreatedOn() throws ClientException {
        return mDevicePopManager.getAsymmetricKeyCreationDate();
    }

    @Override
    public String getThumbprint() throws ClientException {
        return mDevicePopManager.getAsymmetricKeyThumbprint();
    }

    @Override
    public String getPublicKey() throws ClientException {
        return mDevicePopManager.getPublicKey(IDevicePopManager.PublicKeyFormat.JWK);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public String sign(@NonNull final String data) throws ClientException {
        return mDevicePopManager.sign(SHA_256_WITH_RSA, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean verify(@NonNull final String plainText, @NonNull final String signatureStr) {
        return mDevicePopManager.verify(SHA_256_WITH_RSA, plainText, signatureStr);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public String encrypt(@NonNull final String plaintext) throws ClientException {
        return mDevicePopManager.encrypt(RSA_ECB_PKCS_1_PADDING, plaintext);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public String decrypt(@NonNull final String ciphertext) throws ClientException {
        return mDevicePopManager.decrypt(RSA_ECB_PKCS_1_PADDING, ciphertext);
    }

    @Override
    public SecureHardwareState getSecureHardwareState() throws ClientException {
        return mDevicePopManager.getSecureHardwareState();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public byte[] encrypt(byte[] plaintext) throws ClientException {
        return mDevicePopManager.encrypt(RSA_ECB_PKCS_1_PADDING, plaintext);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public byte[] decrypt(byte[] ciphertext) throws ClientException {
        return mDevicePopManager.encrypt(RSA_ECB_PKCS_1_PADDING, ciphertext);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public byte[] sign(byte[] text) throws ClientException {
        return mDevicePopManager.sign(SHA_256_WITH_RSA, text);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean verify(byte[] text, byte[] signature) throws ClientException {
        return mDevicePopManager.verify(SHA_256_WITH_RSA, text, signature);
    }

    @Override
    public Certificate[] getCertificateChain() throws ClientException {
        return mDevicePopManager.getCertificateChain();
    }
}
