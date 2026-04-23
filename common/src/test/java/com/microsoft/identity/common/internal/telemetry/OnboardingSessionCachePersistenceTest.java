// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.identity.common.internal.telemetry;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OnboardingSessionCachePersistenceTest {

    private OnboardingSessionCachePersistence mPersistence;

    @Before
    public void setup() {
        mPersistence = new OnboardingSessionCachePersistence(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testLoad_Empty_ReturnsEmptyString() {
        Assert.assertEquals("", mPersistence.load());
    }

    @Test
    public void testSaveAndLoad() {
        final String json = "{\"key|scope\":{\"id\":\"uuid\",\"ts\":1234567890}}";
        mPersistence.save(json);
        Assert.assertEquals(json, mPersistence.load());
    }

    @Test
    public void testSave_OverwritesPrevious() {
        mPersistence.save("first");
        mPersistence.save("second");
        Assert.assertEquals("second", mPersistence.load());
    }

    @Test
    public void testSave_EmptyString() {
        mPersistence.save("some data");
        mPersistence.save("");
        Assert.assertEquals("", mPersistence.load());
    }
}
