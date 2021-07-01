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

import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.crypto.key.ISecretKeyLoader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

@RunWith(RobolectricTestRunner.class)
public class AndroidBrokerStorageEncryptionManagerTest {

    final byte[] mockAuthAppKeyRawBytes = new byte[]{22, 78, -69, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};

    final byte[] mockCpKeyRawBytes = new byte[]{122, 75, 49, 112, 36, 126, 5, 35, 46, 45, -61, -61, 55, 105, 9, -123, 115, 27, 35, -54, -49, 14, -16, 49, -74, -88, -29, -15, -33, -13, 100, 118};

    // Value extracted from the legacy StorageHelper.
    // Data Set 2 - encrypted by User Defined Key
    final byte[] userDefinedEncryptedText = "cE1VTAwMeHz7BCCH/27kWvMYYMsGamVenQk6w+YJ14JnFBi6fJ1D8FrdLe8ZSX/FeU1apYKsj9d1fNoMD4kR62XfPMytA3P2XpXEQtkblP6F6A5R74F".getBytes(ENCODING_UTF8);

    // Data Set 2 - encrypted by Android Keystore key
    final byte[] androidKeyStoreEncryptedText = "cE1QTAwMTDvTopC+ds4Wgm7IbhnZl1pEVWU+vt7dp0h098822NjPzaNb9JC2PfLyPi/cJuM/wKGYN9YpvRP+BA+i0DlGdCb7nOQ/fmYfjpNq9aj26Kh".getBytes(ENCODING_UTF8);

    final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Before
    public void setUp(){
        AuthenticationSettings.INSTANCE.clearSecretKeysForTestCases();
    }

    @Test
    @Config(shadows = {AndroidBrokerStorageEncryptionManager_OnAuthApp.class})
    public void testGetEncryptionKey_Authenticator(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();

        final ISecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.assertTrue(loader instanceof UserDefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_AUTHENTICATOR_APP_KEY, loader.getAlias());
    }

    @Test
    @Config(shadows = {AndroidBrokerStorageEncryptionManager_OnCP.class})
    public void testGetEncryptionKey_CP(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();

        final ISecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.assertTrue(loader instanceof UserDefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_COMPANY_PORTAL_KEY, loader.getAlias());
    }

    @Test(expected = IllegalStateException.class)
    public void testGetEncryptionKey_UnexpectedBrokerApp(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();

        final ISecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.fail();
    }

    @Test(expected = NullPointerException.class)
    public void testGetEncryptionKey_KeyNotLoaded(){
        final AndroidBrokerStorageEncryptionManager manager = new AndroidBrokerStorageEncryptionManager(context, null);
        final ISecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.fail();
    }

    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithKeyStoreKey(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();
        final List<AES256KeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(androidKeyStoreEncryptedText);

        Assert.assertEquals(1, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof AndroidWrappedKeyLoader);
    }

    @Test
    @Config(shadows = {AndroidBrokerStorageEncryptionManager_OnAuthApp.class})
    public void testGetDecryptionKey_ForDataEncryptedWithUserDefinedKey_OnAuthApp(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();
        final List<AES256KeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(userDefinedEncryptedText);

        Assert.assertEquals(2, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof UserDefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_AUTHENTICATOR_APP_KEY, keyLoaderList.get(0).getAlias());

        Assert.assertTrue(keyLoaderList.get(1) instanceof UserDefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_COMPANY_PORTAL_KEY, keyLoaderList.get(1).getAlias());
    }

    @Test
    @Config(shadows = {AndroidBrokerStorageEncryptionManager_OnCP.class})
    public void testGetDecryptionKey_ForDataEncryptedWithUserDefinedKey_OnCP(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();
        final List<AES256KeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(userDefinedEncryptedText);

        Assert.assertEquals(2, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof UserDefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_COMPANY_PORTAL_KEY, keyLoaderList.get(0).getAlias());

        Assert.assertTrue(keyLoaderList.get(1) instanceof UserDefinedKeyLoader);
        Assert.assertEquals(manager.LEGACY_AUTHENTICATOR_APP_KEY, keyLoaderList.get(1).getAlias());
    }

    @Test
    public void testGetDecryptionKey_ForMalformedData(){
        final AndroidBrokerStorageEncryptionManager manager = initManager();
        final List<AES256KeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption("SOME_MALFORMED_DATA".getBytes(ENCODING_UTF8));

        Assert.assertEquals(0, keyLoaderList.size());
    }

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
