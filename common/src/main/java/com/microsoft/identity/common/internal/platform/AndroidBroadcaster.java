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
package com.microsoft.identity.common.internal.platform;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.util.IBroadcaster;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Android implementation of IBroadcaster.
 */
@AllArgsConstructor
public class AndroidBroadcaster implements IBroadcaster {

    @NonNull
    private final Context mContext;

    @Override
    public void sendBroadcast(@NonNull final String broadcastId, @Nullable final PropertyBag propertyBag) {
        final Intent intent = new Intent();
        intent.setAction(broadcastId);
        if(propertyBag != null) {
            for (final String key : propertyBag.keySet()) {
                intent.putExtra(key, propertyBag.<Parcelable[]>get(key));
            }
        }
        mContext.sendBroadcast(intent);
    }

}
