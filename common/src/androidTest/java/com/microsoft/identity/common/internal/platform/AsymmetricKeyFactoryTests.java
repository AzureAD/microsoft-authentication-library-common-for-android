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

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.exception.ClientException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AsymmetricKeyFactoryTests {

    private static final String SAMPLE_KEY_1 = "sample_key_1";
    private static final String SAMPLE_KEY_2 = "sample_key_2";

    private AsymmetricKeyFactory mKeyFactory;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getTargetContext();
        mKeyFactory = new AndroidKeystoreAsymmetricKeyFactory(context);
    }

    @Test
    public void testCanGenerateAsymmetricKey() throws ClientException {
        final AsymmetricKey asymmetricKey = mKeyFactory.generateAsymmetricKey(SAMPLE_KEY_1);
        Assert.assertNotNull(asymmetricKey);
        Assert.assertNotNull(asymmetricKey.getThumbprint());
    }

    @Test
    public void testCanGenerateMultipleKeys() throws ClientException {
        final AsymmetricKey asymmetricKey1 = mKeyFactory.generateAsymmetricKey(SAMPLE_KEY_1);
        final AsymmetricKey asymmetricKey2 = mKeyFactory.generateAsymmetricKey(SAMPLE_KEY_2);

        Assert.assertNotNull(asymmetricKey1);
        Assert.assertNotNull(asymmetricKey1.getThumbprint());

        Assert.assertNotNull(asymmetricKey1);
        Assert.assertNotNull(asymmetricKey1.getThumbprint());

        // Assert they're not the same
        Assert.assertNotEquals(
                asymmetricKey1.getThumbprint(),
                asymmetricKey2.getThumbprint()
        );
    }

    @Test
    public void testClearingOneKeyPreservesAnother() throws ClientException {
        final AsymmetricKey asymmetricKey1 = mKeyFactory.generateAsymmetricKey(SAMPLE_KEY_1);
        final AsymmetricKey asymmetricKey2 = mKeyFactory.generateAsymmetricKey(SAMPLE_KEY_2);

        final String key1Thumbprint = asymmetricKey1.getThumbprint();

        // Clear our second key
        mKeyFactory.clearAsymmetricKey(SAMPLE_KEY_2);

        // Reload our first key
        final AsymmetricKey reloadedFirstKey = mKeyFactory.loadAsymmetricKey(SAMPLE_KEY_1);

        // Reload the first key - assert that signature hasn't changed
        Assert.assertEquals(
                key1Thumbprint,
                reloadedFirstKey.getThumbprint()
        );

        // Assert the alias is the same
        Assert.assertEquals(
                asymmetricKey1.getAlias(),
                reloadedFirstKey.getAlias()
        );
    }

    @Test
    public void testCanGenerateKeyWhenLoadCalledBeforeGeneration() throws ClientException {
        final AsymmetricKey key1 = mKeyFactory.loadAsymmetricKey(SAMPLE_KEY_1);
        Assert.assertEquals(SAMPLE_KEY_1, key1.getAlias());
    }

    @Test
    public void testCanClearAsymmetricKey() throws ClientException {
        // Generate a key
        final AsymmetricKey asymmetricKey1 = mKeyFactory.generateAsymmetricKey(SAMPLE_KEY_1);

        // Get its thumbprint
        final String key1Thumbprint = asymmetricKey1.getThumbprint();

        // Clear that key
        mKeyFactory.clearAsymmetricKey(SAMPLE_KEY_1);

        // Generate a new key under the same alias
        final AsymmetricKey asymmetricKey2 = mKeyFactory.generateAsymmetricKey(SAMPLE_KEY_1);

        // Confirm that the thumbprint has changed
        Assert.assertNotEquals(
                key1Thumbprint,
                asymmetricKey2.getThumbprint()
        );
    }

}
