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
package com.microsoft.identity.common.internal.ui;

import static org.junit.Assert.assertTrue;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for {@link ReturnToCallerActivity}. These verify the trampoline's core contract:
 * it reveals the caller's task and finishes immediately, never lingering.
 *
 * <p>Note: the actual task foregrounding (reusing the caller's task via NEW_TASK + taskAffinity)
 * is a windowing behavior that cannot be asserted under Robolectric and must be validated on a
 * real device across OEMs / API levels.</p>
 */
@RunWith(RobolectricTestRunner.class)
public class ReturnToCallerActivityTest {

    @Test
    public void onCreate_finishesImmediately() {
        final ReturnToCallerActivity activity =
                Robolectric.buildActivity(ReturnToCallerActivity.class).create().get();

        assertTrue("ReturnToCallerActivity must finish immediately after revealing the caller task",
                activity.isFinishing());
    }

    @Test
    public void onCreate_withReturnAction_finishesImmediately() {
        final Intent intent = new Intent()
                .setAction(ReturnToCallerActivity.ACTION_RETURN_FROM_VID);
        final ReturnToCallerActivity activity =
                Robolectric.buildActivity(ReturnToCallerActivity.class, intent).create().get();

        assertTrue("ReturnToCallerActivity must finish immediately regardless of the launching intent",
                activity.isFinishing());
    }
}
