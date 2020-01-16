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

package com.microsoft.identity.common.internal.providers.oauth2;

import android.content.Intent;

import com.microsoft.identity.common.internal.logging.Logger;

import java.lang.ref.WeakReference;

public class InteractiveAuthResultBroadcaster {
    private static final String TAG = InteractiveAuthResultBroadcaster.class.getSimpleName();

    private static WeakReference<IInteractiveAuthResultCallback> mCallbackReference;

    /**
     * Registers a weak reference to the callback object to be invoked when the result is sent.
     * The caller is responsible for holding reference to this callback object.
     */
    public synchronized static void register(final IInteractiveAuthResultCallback callback) {
        mCallbackReference = new WeakReference<>(callback);
    }

    /**
     * Broadcast a result to the registered callback.
     */
    public synchronized static void broadcast(final int requestCode,
                                              final int resultCode,
                                              final Intent resultIntent) {
        final String methodName = ":broadcast";

        if (mCallbackReference == null) {
            Logger.warn(TAG + methodName, "mCallbackReference is null.");
            return;
        }

        final IInteractiveAuthResultCallback callback = mCallbackReference.get();
        if (callback == null) {
            Logger.warn(TAG + methodName, "This reference object has been cleared.");
            return;
        }

        callback.onGetResult(requestCode, resultCode, resultIntent);
    }
}
