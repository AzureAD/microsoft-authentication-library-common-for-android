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

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.crypto.key.AbstractSecretKeyLoader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.crypto.MockData.ANOTHER_PREDEFINED_KEY;
import static com.microsoft.identity.common.crypto.MockData.PREDEFINED_KEY;
import static com.microsoft.identity.common.crypto.MockData.TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY;
import static com.microsoft.identity.common.crypto.MockData.TEXT_ENCRYPTED_BY_PREDEFINED_KEY;
import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

@RunWith(RobolectricTestRunner.class)
public class AndroidBrokerStorageEncryptionManagerTest {

    final byte[] mockAuthAppKeyRawBytes = PREDEFINED_KEY;
    final byte[] mockCpKeyRawBytes = ANOTHER_PREDEFINED_KEY;

    final Context context = ApplicationProvider.getApplicationContext();;

    @Before
    public void setUp(){
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
    }

    @Test
    @Config(shadows = {AndroidBrokerStorageEncryptionManager_OnAuthApp.class})
    public void testGetEncryptionKey_Authenticator(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();

        final AbstractSecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.assertTrue(loader instanceof PredefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_AUTHENTICATOR_APP_KEY_ALIAS, loader.getAlias());
    }

    @Test
    @Config(shadows = {AndroidBrokerStorageEncryptionManager_OnCP.class})
    public void testGetEncryptionKey_CP(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();

        final AbstractSecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.assertTrue(loader instanceof PredefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_COMPANY_PORTAL_KEY_ALIAS, loader.getAlias());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetEncryptionKey_UnexpectedBrokerApp(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();

        final AbstractSecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.fail();
    }

    @Test(expected = NullPointerException.class)
    public void testGetEncryptionKey_KeyNotLoaded(){
        final AndroidBrokerStorageEncryptionManager manager = new AndroidBrokerStorageEncryptionManager(context, null);
        final AbstractSecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.fail();
    }

    /**
     * Given a data encrypted with a wrapped key,
     * try getting a decryption key.
     */
    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithWrappedKey(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();
        final List<AbstractSecretKeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(TEXT_ENCRYPTED_BY_ANDROID_WRAPPED_KEY);

        Assert.assertEquals(1, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof AndroidWrappedKeyLoader);
    }

    /**
     * On AuthApp,
     * Given a data encrypted with a predefined key,
     * try getting a decryption key.
     */
    @Test
    @Config(shadows = {AndroidBrokerStorageEncryptionManager_OnAuthApp.class})
    public void testGetDecryptionKey_ForDataEncryptedWithUserDefinedKey_OnAuthApp(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();
        final List<AbstractSecretKeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);

        Assert.assertEquals(2, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof PredefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_AUTHENTICATOR_APP_KEY_ALIAS, keyLoaderList.get(0).getAlias());

        Assert.assertTrue(keyLoaderList.get(1) instanceof PredefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_COMPANY_PORTAL_KEY_ALIAS, keyLoaderList.get(1).getAlias());
    }

    /**
     * On CP,
     * Given a data encrypted with the AuthApp key,
     * try getting a decryption key.
     */
    @Test
    @Config(shadows = {AndroidBrokerStorageEncryptionManager_OnCP.class})
    public void testGetDecryptionKey_ForDataEncryptedWithUserDefinedKey_OnCP(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();
        final List<AbstractSecretKeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(TEXT_ENCRYPTED_BY_PREDEFINED_KEY);

        Assert.assertEquals(2, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof PredefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_COMPANY_PORTAL_KEY_ALIAS, keyLoaderList.get(0).getAlias());

        Assert.assertTrue(keyLoaderList.get(1) instanceof PredefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_AUTHENTICATOR_APP_KEY_ALIAS, keyLoaderList.get(1).getAlias());
    }

    /**
     * Try getting a decryption key with a malformed data.
     */
    @Test
    public void testGetDecryptionKey_ForMalformedData(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();
        final List<AbstractSecretKeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption("SOME_MALFORMED_DATA".getBytes(ENCODING_UTF8));

        Assert.assertEquals(0, keyLoaderList.size());
    }

    /**
     * Loads mock CP and AuthApp keys.
     */
    private AndroidBrokerStorageEncryptionManager initManager() {
        final Map<String, byte[]> secretKeys = new HashMap<String, byte[]>(2);
        secretKeys.put(AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME,
                mockAuthAppKeyRawBytes);
        secretKeys.put(AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME,
                mockCpKeyRawBytes);
        AuthenticationSettings.INSTANCE.setBrokerSecretKeys(secretKeys);
        return new AndroidBrokerStorageEncryptionManager(context, null);
    }
}
