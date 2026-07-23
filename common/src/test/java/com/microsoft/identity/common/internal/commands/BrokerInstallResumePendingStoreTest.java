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
package com.microsoft.identity.common.internal.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for {@link BrokerInstallResumePendingStore} — the durable, on-disk half of the MAM
 * broker-install request-resume. Verifies the persist / peek / clear round-trip that the SDK auto-init
 * relies on to re-drive an onboarding after a process restart.
 */
@RunWith(RobolectricTestRunner.class)
public class BrokerInstallResumePendingStoreTest {

    private Context context() {
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void peek_returnsNull_whenNothingPersisted() {
        final Context context = context();
        BrokerInstallResumePendingStore.clear(context);

        assertNull(BrokerInstallResumePendingStore.peekResumeReady(context));
    }

    @Test
    public void markThenPeek_returnsPersistedRecord() {
        final Context context = context();
        BrokerInstallResumePendingStore.clear(context);

        BrokerInstallResumePendingStore.markBrokerInstallPending(
                context,
                "user@contoso.com",
                "https://graph.microsoft.com/.default",
                "market://details?id=com.microsoft.windowsintune.companyportal",
                BrokerInstallResumePendingStore.DEFAULT_TTL_MILLISECONDS);

        final BrokerInstallResumePendingStore.Record record =
                BrokerInstallResumePendingStore.peekResumeReady(context);

        assertNotNull(record);
        assertEquals("user@contoso.com", record.upn);
        assertEquals("https://graph.microsoft.com/.default", record.target);
        assertEquals("market://details?id=com.microsoft.windowsintune.companyportal", record.appLink);
    }

    @Test
    public void clear_removesPersistedRecord() {
        final Context context = context();
        BrokerInstallResumePendingStore.markBrokerInstallPending(
                context, "user@contoso.com", "target", "app_link",
                BrokerInstallResumePendingStore.DEFAULT_TTL_MILLISECONDS);
        assertNotNull(BrokerInstallResumePendingStore.peekResumeReady(context));

        BrokerInstallResumePendingStore.clear(context);

        assertNull(BrokerInstallResumePendingStore.peekResumeReady(context));
    }

    @Test
    public void markThenPeek_toleratesNullFields() {
        final Context context = context();
        BrokerInstallResumePendingStore.clear(context);

        BrokerInstallResumePendingStore.markBrokerInstallPending(
                context, null, null, null,
                BrokerInstallResumePendingStore.DEFAULT_TTL_MILLISECONDS);

        final BrokerInstallResumePendingStore.Record record =
                BrokerInstallResumePendingStore.peekResumeReady(context);

        assertNotNull(record);
        assertNull(record.upn);
        assertNull(record.target);
        assertNull(record.appLink);
    }
}
