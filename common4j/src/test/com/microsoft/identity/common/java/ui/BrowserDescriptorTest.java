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
package com.microsoft.identity.common.java.ui;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link BrowserDescriptor}, verifying the hard-coded switch-browser safelist
 * contains the expected packages and signature hashes. These tests also serve as a regression
 * guard for the Microsoft Edge multi-signer fix (see MSAL #2414): both Edge signing certificate
 * hashes must remain in the descriptor so Edge installs are trusted across the APK signing
 * certificate rotation lifecycle.
 */
public class BrowserDescriptorTest {

    private static final String EDGE_PACKAGE_NAME = "com.microsoft.emmx";
    private static final String EDGE_ORIGINAL_SIGNATURE_HASH =
            "Ivy-Rk6ztai_IudfbyUrSHugzRqAtHWslFvHT0PTvLMsEKLUIgv7ZZbVxygWy_M5mOPpfjZrd3vOx3t-cA6fVQ==";
    private static final String EDGE_ROTATED_SIGNATURE_HASH =
            "KxJRZ8RFW-6BQa-e4xNE7UmeGU6BWIR_6dzgaAOQWh0rWVENxsXU5TjnWuTR9GqOFbCKMilXKIu7as6VJRjuSw==";
    private static final String CHROME_PACKAGE_NAME = "com.android.chrome";

    @Test
    public void switchBrowserSafeListContainsEdgeWithBothSignatureHashes() {
        final List<BrowserDescriptor> safeList = BrowserDescriptor.getBrowserSafeListForSwitchBrowser();

        BrowserDescriptor edge = null;
        for (final BrowserDescriptor descriptor : safeList) {
            if (EDGE_PACKAGE_NAME.equals(descriptor.getPackageName())) {
                edge = descriptor;
                break;
            }
        }

        Assert.assertNotNull("Edge entry must be present in switch-browser safelist", edge);
        Assert.assertTrue(
                "Original Edge signing certificate hash must remain in the safelist",
                edge.getSignatureHashes().contains(EDGE_ORIGINAL_SIGNATURE_HASH));
        Assert.assertTrue(
                "Rotated Edge signing certificate hash must be present in the safelist (regression for MSAL #2414)",
                edge.getSignatureHashes().contains(EDGE_ROTATED_SIGNATURE_HASH));
    }

    @Test
    public void switchBrowserSafeListContainsChrome() {
        final List<BrowserDescriptor> safeList = BrowserDescriptor.getBrowserSafeListForSwitchBrowser();

        boolean chromeFound = false;
        for (final BrowserDescriptor descriptor : safeList) {
            if (CHROME_PACKAGE_NAME.equals(descriptor.getPackageName())) {
                chromeFound = true;
                break;
            }
        }

        Assert.assertTrue("Chrome entry must be present in switch-browser safelist", chromeFound);
    }

    @Test
    public void brokerSafeListContainsChromeOnly() {
        final List<BrowserDescriptor> safeList = BrowserDescriptor.getBrowserSafeListForBroker();

        Assert.assertEquals("Broker safelist is expected to contain a single entry", 1, safeList.size());
        Assert.assertEquals(CHROME_PACKAGE_NAME, safeList.get(0).getPackageName());
    }
}
