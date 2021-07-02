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
package com.microsoft.identity.common;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.microsoft.identity.common.internal.cache.IAuthorityValidationMetadataCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAuthorityValidationMetadataCache;
import com.microsoft.identity.common.java.exception.ClientException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AuthorityValidationMetadataCacheTest {

    private IAuthorityValidationMetadataCache mAuthorityValidationMetadataCache;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        // Context and related init
        mContext = ApplicationProvider.getApplicationContext();
        mAuthorityValidationMetadataCache = new SharedPreferencesAuthorityValidationMetadataCache(mContext);
    }

    @After
    public void tearDown() {
        mAuthorityValidationMetadataCache.clearCache();
    }

    @Test
    public void saveAuthorityValidationMetadataSuccess() throws ClientException {
        String environment = "login.microsoftonline.com";
        String metadata = "{metadata}";
        String readMetadata = mAuthorityValidationMetadataCache.getAuthorityValidationMetadata(environment);
        Assert.assertEquals(readMetadata, null);
        mAuthorityValidationMetadataCache.saveAuthorityValidationMetadata(environment, metadata);
        readMetadata = mAuthorityValidationMetadataCache.getAuthorityValidationMetadata(environment);
        Assert.assertEquals(readMetadata, metadata);
    }

    @Test(expected = IllegalArgumentException.class)
    public void readAuthorityValidationMetadataWithNullKey(){
        mAuthorityValidationMetadataCache.getAuthorityValidationMetadata(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void readAuthorityValidationMetadataWithEmptyKey(){
        mAuthorityValidationMetadataCache.getAuthorityValidationMetadata("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeAuthorityValidationMetadataWithNullValue(){
        String environment = "login.microsoftonline.com";
        mAuthorityValidationMetadataCache.saveAuthorityValidationMetadata(environment, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void writeAuthorityValidationMetadataWithEmptyValue(){
        String environment = "login.microsoftonline.com";
        mAuthorityValidationMetadataCache.saveAuthorityValidationMetadata(environment, "");
    }
}
