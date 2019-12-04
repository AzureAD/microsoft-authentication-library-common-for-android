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
package com.microsoft.identity.common.adal.internal;

import android.os.Build;

import com.microsoft.identity.common.internal.encryption.BrokerEncryptionManager;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;

public abstract class AndroidSecretKeyEnabledHelper extends AndroidTestHelper {

    private static final int MIN_SDK_VERSION = 18;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null && Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            setAdalSecretKeyData();
        }

        setLegacyBrokerSecretKeysData();
        BrokerEncryptionManager.setMockPackageName(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);
    }

    public static void setAdalSecretKeyData() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        // use same key for tests
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        final SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("mock-password".toCharArray(), "mock-byte-code-for-salt".getBytes("UTF-8"), 100, 256));
        final SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
        AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
    }

    public static void setLegacyBrokerSecretKeysData() throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException {
        final Map<String, byte[]> secretKeys = new HashMap<String, byte[]>(2);
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithSHA256And256BitAES-CBC-BC");

        final SecretKey authAppTempkey = keyFactory.generateSecret(new PBEKeySpec("mock-password".toCharArray(), "AZURE_AUTHENTICATOR_APP_SALT".getBytes("UTF-8"), 100, 256));
        final SecretKey authAppSecretKey = new SecretKeySpec(authAppTempkey.getEncoded(), "AES");
        secretKeys.put(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, authAppSecretKey.getEncoded());

        final SecretKey cpTempkey = keyFactory.generateSecret(new PBEKeySpec("mock-password".toCharArray(), "COMPANY_PORTAL_APP_SALT".getBytes("UTF-8"), 100, 256));
        final SecretKey cpSecretKey = new SecretKeySpec(cpTempkey.getEncoded(), "AES");
        secretKeys.put(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, cpSecretKey.getEncoded());

        AuthenticationSettings.INSTANCE.setBrokerSecretKeys(secretKeys);
    }
}
