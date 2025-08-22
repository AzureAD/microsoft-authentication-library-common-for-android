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
package com.microsoft.identity.common.internal.ui.webview.certbasedauth;

import android.app.Activity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.annotation.NonNull;

/**
 * Unit tests for {@link DialogHolder} class with focus on exception scenarios.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows={ShadowExceptionSmartcardDialog.class})
public class DialogHolderTest {

    private DialogHolder mDialogHolder;

    @Before
    public void setUp() {
        ActivityController<Activity> activityController = Robolectric.buildActivity(Activity.class);
        Activity activity = activityController.get();
        mDialogHolder = new DialogHolder(activity);
    }

    @Test
    @Config(shadows={ShadowUserChoiceDialog.class})
    public void testExceptionThrownFromUserChoiceDialog() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mDialogHolder.showUserChoiceDialog(
                new UserChoiceDialog.PositiveButtonListener() {
                    @Override
                    public void onClick(int checkedPosition) {
                        // This shouldn't be used.
                        fail("Should not be here");
                    }
                },
                new ICancelCbaCallback() {
                    @Override
                    public void onCancel() {
                        // If we get here, the test has passed.
                        latch.countDown();
                    }
                }
        );
        assertTrue("Cancel callback was not called", latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    @Config(shadows={ShadowSmartcardCertPickerDialog.class})
    public void testExceptionThrownFromCertPickerDialog() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<ICertDetails> certList = new ArrayList<>();
        mDialogHolder.showCertPickerDialog(
                certList,
                new SmartcardCertPickerDialog.PositiveButtonListener() {
                    @Override
                    public void onClick(@NonNull ICertDetails selectedCertDetails) {
                        // This shouldn't be used.
                        fail("Should not be here");
                    }
                },
                new ICancelCbaCallback() {
                    @Override
                    public void onCancel() {
                        // If we get here, the test has passed.
                        latch.countDown();
                    }
                }
        );
        assertTrue("Cancel callback was not called", latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    @Config(shadows={ShadowSmartcardPinDialog.class})
    public void testExceptionThrownFromPinDialog() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mDialogHolder.showPinDialog(
                new SmartcardPinDialog.PositiveButtonListener() {
                    @Override
                    public void onClick(final char[] pin) {
                        // This shouldn't be used.
                        fail("Should not be here");
                    }
                },
                new ICancelCbaCallback() {
                    @Override
                    public void onCancel() {
                        // If we get here, the test has passed.
                        latch.countDown();
                    }
                }
        );
        assertTrue("Cancel callback was not called", latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    @Config(shadows={ShadowSmartcardErrorDialog.class})
    public void testExceptionThrownFromErrorDialog() {
        // We can't easily verify the callback is called since it's an anonymous inner class
        // in the implementation, but we can verify no exceptions are thrown
        mDialogHolder.showErrorDialog(
                1,
                1
        );
    }

    @Test
    @Config(shadows={ShadowSmartcardErrorDialog.class})
    public void testExceptionThrownFromErrorDialogWithCustomButton() {
        // We can't easily verify the callback is called since it's an anonymous inner class
        // in the implementation, but we can verify no exceptions are thrown
        mDialogHolder.showErrorDialog(
                1,
                1,
                1
        );
    }

    @Test
    @Config(shadows={ShadowSmartcardPromptDialog.class})
    public void testExceptionThrownFromSmartcardPromptDialog() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mDialogHolder.showSmartcardPromptDialog(
                new ICancelCbaCallback() {
                    @Override
                    public void onCancel() {
                        // If we get here, the test has passed.
                        latch.countDown();
                    }
                }
        );
        assertTrue("Cancel callback was not called", latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    @Config(shadows={ShadowSmartcardNfcLoadingDialog.class})
    public void testExceptionThrownFromSmartcardNfcLoadingDialog() {
        // No callback to verify, but we can verify no exceptions are thrown
        mDialogHolder.showSmartcardNfcLoadingDialog();
    }

    @Test
    @Config(shadows={ShadowSmartcardNfcPromptDialog.class})
    public void testExceptionThrownFromSmartcardNfcPromptDialog() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mDialogHolder.showSmartcardNfcPromptDialog(
                new ICancelCbaCallback() {
                    @Override
                    public void onCancel() {
                        // If we get here, the test has passed.
                        latch.countDown();
                    }
                }
        );
        assertTrue("Cancel callback was not called", latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    @Config(shadows={ShadowSmartcardNfcReminderDialog.class})
    public void testExceptionThrownFromSmartcardNfcReminderDialog() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mDialogHolder.showSmartcardNfcReminderDialog(
                new IDismissCallback() {
                    @Override
                    public void onDismiss() {
                        // If we get here, the test has passed.
                        latch.countDown();
                    }
                }
        );
        assertTrue("Dismiss callback was not called", latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    @Config(shadows={ShadowSmartcardRemovalPromptDialog.class})
    public void testExceptionThrownFromSmartcardRemovalPromptDialog() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mDialogHolder.showSmartcardRemovalPromptDialog(
                new IDismissCallback() {
                    @Override
                    public void onDismiss() {
                        // If we get here, the test has passed.
                        latch.countDown();
                    }
                }
        );
        assertTrue("Dismiss callback was not called", latch.await(500, TimeUnit.MILLISECONDS));
    }
}
