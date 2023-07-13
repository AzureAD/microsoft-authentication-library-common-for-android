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

package com.microsoft.identity.common.internal.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.broker.BrokerValidator;

import java.util.List;
import java.util.Set;

/**
 * Utility class for anything relating to process.
 */
public class ProcessUtil {

    private ProcessUtil() {
    }

    /**
     * Returns true if the calling app is the auth process.
     */
    public static boolean isBrokerProcess(@NonNull final Context context) {
        final String processName = getProcessName(context);

        final Set<BrokerData> validBrokers = BrokerData.getKnownBrokerApps();

        for (final BrokerData brokerData : validBrokers) {
            final String authProcess = brokerData.getPackageName() + ":auth";
            if (authProcess.equalsIgnoreCase(processName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the running process name.
     */
    private static @Nullable
    String getProcessName(@NonNull final Context context) {
        final int pid = android.os.Process.myPid();
        final ActivityManager am = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
        if (runningProcesses != null) {
            for (final ActivityManager.RunningAppProcessInfo procInfo : runningProcesses) {
                if (procInfo.pid == pid) {
                    return procInfo.processName;
                }
            }
        }

        return null;
    }

    /**
     * Returns a handler based on the current looper.
     */
    public static @NonNull
    Handler getPreferredHandler() {
        if (null != Looper.myLooper()) {
            return new Handler(Looper.myLooper());
        } else {
            return new Handler(Looper.getMainLooper());
        }
    }
}
