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

import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.crypto.key.AES256KeyLoader;
import com.microsoft.identity.common.java.crypto.key.ISecretKeyLoader;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

@RunWith(RobolectricTestRunner.class)
public class AndroidSdkStorageEncryptionManagerTest {

    final byte[] mockKeyRawBytes = new byte[]{22, 78, -69, -66, 84, -65, 119, -9, -34, -80, 60, 67, -12, -117, 86, -47, -84, -24, -18, 121, 70, 32, -110, 51, -93, -10, -93, -110, 124, -68, -42, -119};
    final SecretKey secretKeyMock = new SecretKeySpec(mockKeyRawBytes, "AES");

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
    public void testGetEncryptionKey(){
        final AndroidSdkStorageEncryptionManager manager = new AndroidSdkStorageEncryptionManager(context, null);

        final ISecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.assertTrue(loader instanceof AndroidWrappedKeyLoader);
        Assert.assertNotEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(loader));
    }

    @Test
    public void testGetEncryptionKey_UserDefinedKeyProvided(){
        AuthenticationSettings.INSTANCE.setSecretKey(mockKeyRawBytes);
        final AndroidSdkStorageEncryptionManager manager = new AndroidSdkStorageEncryptionManager(context, null);

        final ISecretKeyLoader loader = manager.getKeyLoaderForEncryption();
        Assert.assertTrue(loader instanceof PredefinedKeyLoader);
        Assert.assertEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(loader));
    }

    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithKeyStoreKey(){
        final AndroidSdkStorageEncryptionManager manager = new AndroidSdkStorageEncryptionManager(context, null);
        final List<ISecretKeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(androidKeyStoreEncryptedText);

        Assert.assertEquals(1, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof AndroidWrappedKeyLoader);
        Assert.assertNotEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(keyLoaderList.get(0)));
    }

    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithKeyStoreKey_UserDefinedKeyProvided(){
        AuthenticationSettings.INSTANCE.setSecretKey(mockKeyRawBytes);
        final AndroidSdkStorageEncryptionManager manager = new AndroidSdkStorageEncryptionManager(context, null);
        final List<ISecretKeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(androidKeyStoreEncryptedText);

        Assert.assertEquals(1, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof AndroidWrappedKeyLoader);
        Assert.assertNotEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(keyLoaderList.get(0)));
    }

    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithUserDefinedKey(){
        final AndroidSdkStorageEncryptionManager manager = new AndroidSdkStorageEncryptionManager(context, null);
        final List<ISecretKeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(userDefinedEncryptedText);

        Assert.assertEquals(0, keyLoaderList.size());
    }

    @Test
    public void testGetDecryptionKey_ForDataEncryptedWithUserDefinedKey_UserDefinedKeyProvided(){
        AuthenticationSettings.INSTANCE.setSecretKey(mockKeyRawBytes);
        final AndroidSdkStorageEncryptionManager manager = new AndroidSdkStorageEncryptionManager(context, null);
        final List<ISecretKeyLoader> keyLoaderList = manager.getKeyLoaderForDecryption(userDefinedEncryptedText);

        Assert.assertEquals(1, keyLoaderList.size());
        Assert.assertTrue(keyLoaderList.get(0) instanceof PredefinedKeyLoader);
        Assert.assertEquals(KeyUtil.getKeyThumbPrint(secretKeyMock), KeyUtil.getKeyThumbPrint(keyLoaderList.get(0)));
    }
}
