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

package com.microsoft.identity.common.internal.ui.webview;

import android.app.ActivityManager;
import android.content.Context;

public class ProcessUtil {

    public static final String AuthServiceProcess = "auth";

    /**
     * Helper method which returns true if its running on :auth process, returns false otherwise.
     *
     * @param context
     * @return
     */
    public static boolean isRunningOnAuthService(final Context context) {
        final String processName = context.getPackageName() + ":" + AuthServiceProcess;
        int pid = android.os.Process.myPid();
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null && activityManager.getRunningAppProcesses() != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
                if (processInfo.pid == pid && processInfo.processName.equalsIgnoreCase(processName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
