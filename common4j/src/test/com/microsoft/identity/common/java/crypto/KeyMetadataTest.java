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

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

public class KeyMetadataTest {

    private static final String VERSION_ID = "K001";
    private static final long CREATED_AT_MILLIS = 1_700_000_000_000L;
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;

    @Test
    public void testBuilder_setsAllFields() {
        final KeyMetadata metadata = new KeyMetadata.Builder()
                .versionId(VERSION_ID)
                .createdAtMillis(CREATED_AT_MILLIS)
                .algorithm(ALGORITHM)
                .keySize(KEY_SIZE)
                .isDeprecated(false)
                .build();

        Assert.assertEquals(VERSION_ID, metadata.getVersionId());
        Assert.assertEquals(CREATED_AT_MILLIS, metadata.getCreatedAtMillis());
        Assert.assertEquals(ALGORITHM, metadata.getAlgorithm());
        Assert.assertEquals(KEY_SIZE, metadata.getKeySize());
        Assert.assertFalse(metadata.isDeprecated());
    }

    @Test
    public void testBuilder_defaultValues() {
        final KeyMetadata metadata = new KeyMetadata.Builder()
                .versionId(VERSION_ID)
                .createdAtMillis(CREATED_AT_MILLIS)
                .build();

        Assert.assertEquals(KeyMetadata.DEFAULT_ALGORITHM, metadata.getAlgorithm());
        Assert.assertEquals(KeyMetadata.DEFAULT_KEY_SIZE, metadata.getKeySize());
        Assert.assertFalse(metadata.isDeprecated());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_throwsWhenVersionIdMissing() {
        new KeyMetadata.Builder()
                .createdAtMillis(CREATED_AT_MILLIS)
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_throwsWhenVersionIdEmpty() {
        new KeyMetadata.Builder()
                .versionId("")
                .createdAtMillis(CREATED_AT_MILLIS)
                .build();
    }

    @Test
    public void testToJson_producesValidJson() throws JSONException {
        final KeyMetadata metadata = new KeyMetadata.Builder()
                .versionId(VERSION_ID)
                .createdAtMillis(CREATED_AT_MILLIS)
                .algorithm(ALGORITHM)
                .keySize(KEY_SIZE)
                .isDeprecated(true)
                .build();

        final String json = metadata.toJson();
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains(VERSION_ID));
        Assert.assertTrue(json.contains(String.valueOf(CREATED_AT_MILLIS)));
        Assert.assertTrue(json.contains(ALGORITHM));
        Assert.assertTrue(json.contains(String.valueOf(KEY_SIZE)));
        Assert.assertTrue(json.contains("true")); // isDeprecated
    }

    @Test
    public void testFromJson_reconstructsObject() throws JSONException {
        final KeyMetadata original = new KeyMetadata.Builder()
                .versionId(VERSION_ID)
                .createdAtMillis(CREATED_AT_MILLIS)
                .algorithm(ALGORITHM)
                .keySize(KEY_SIZE)
                .isDeprecated(false)
                .build();

        final String json = original.toJson();
        final KeyMetadata reconstructed = KeyMetadata.fromJson(json);

        Assert.assertEquals(original.getVersionId(), reconstructed.getVersionId());
        Assert.assertEquals(original.getCreatedAtMillis(), reconstructed.getCreatedAtMillis());
        Assert.assertEquals(original.getAlgorithm(), reconstructed.getAlgorithm());
        Assert.assertEquals(original.getKeySize(), reconstructed.getKeySize());
        Assert.assertEquals(original.isDeprecated(), reconstructed.isDeprecated());
    }

    @Test
    public void testFromJson_reconstructsDeprecatedKey() throws JSONException {
        final KeyMetadata original = new KeyMetadata.Builder()
                .versionId("K002")
                .createdAtMillis(CREATED_AT_MILLIS)
                .isDeprecated(true)
                .build();

        final KeyMetadata reconstructed = KeyMetadata.fromJson(original.toJson());

        Assert.assertEquals("K002", reconstructed.getVersionId());
        Assert.assertTrue(reconstructed.isDeprecated());
    }

    @Test(expected = JSONException.class)
    public void testFromJson_throwsOnMalformedJson() throws JSONException {
        KeyMetadata.fromJson("not valid json");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilder_throwsOnInvalidKeySize() {
        new KeyMetadata.Builder()
                .versionId(VERSION_ID)
                .createdAtMillis(CREATED_AT_MILLIS)
                .keySize(0)
                .build();
    }
}
