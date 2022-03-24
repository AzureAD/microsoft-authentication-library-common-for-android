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

package com.microsoft.identity.common.internal.broker;

import android.content.ComponentName;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.logging.Logger;

/**
 * A bound service connection.
 */
public class BoundServiceConnection implements android.content.ServiceConnection {
    private static final String TAG = BoundServiceConnection.class.getSimpleName();
    private final ResultFuture<IBinder> mFuture;

    public BoundServiceConnection(@NonNull final ResultFuture<IBinder> future) {
        mFuture = future;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        final String methodTag = TAG + ":onServiceConnected";
        Logger.info(methodTag, name.getClassName() + " is connected.");
        mFuture.setResult(service);
    }

    /*
    //Needed for API level 26 and above
    @Override
    public void onBindingDied(ComponentName name){

    }

    //Needed for API level 28 and above
    @Override
    public void onNullBinding(ComponentName name){

    }
    */

    @Override
    public void onServiceDisconnected(@NonNull final ComponentName name) {
        final String methodTag = TAG + ":onServiceDisconnected";
        Logger.info(methodTag, name.getClassName() + " is disconnected.");
    }
}