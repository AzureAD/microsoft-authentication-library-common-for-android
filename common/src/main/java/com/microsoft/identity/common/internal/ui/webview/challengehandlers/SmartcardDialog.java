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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import android.app.Activity;
import android.app.Dialog;

import androidx.annotation.NonNull;

/**
 * Lists the main methods needed to show a simple dialog in ClientCertAuthChallengeHandler.
 * Button listeners can be implemented in child classes.
 */
public abstract class SmartcardDialog {
    protected final Activity mActivity;
    protected Dialog mDialog;

    /**
     * Creates new instance of SmartcardDialog.
     * @param activity Host activity.
     */
    public SmartcardDialog(@NonNull final Activity activity) {
        mActivity = activity;
        mDialog = null;
    }

    /**
     * Should build an Android Dialog object and set it to mDialog.
     * Dialog objects must be built/interacted with on the UI thread.
     */
    abstract void createDialog();

    /**
     * Should dismiss dialog and call the appropriate methods to help cancel the CBA flow.
     */
    abstract void onCancelCba();

    /**
     * Show mDialog on the main thread.
     */
    public void show() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.show();
            }
        });
    }

    /**
     * Dismiss mDialog.
     */
    public void dismiss() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDialog.dismiss();
            }
        });
    }
}
