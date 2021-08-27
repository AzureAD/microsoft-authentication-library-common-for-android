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
package com.microsoft.identity.client.ui.automation.performance;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * An object that stores the current process information.
 */
@Data
@Accessors(prefix = "m")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInfo {

    // static in order to calculate the application uid
    private static final String packageName = ApplicationProvider.getApplicationContext().getPackageName();
    private static int applicationUid = -1;

    private int mPid;
    private long mTotalSystemMemory;
    private long mUsedSystemMemory; // the memory being used by all the processes in the device
    private long mFreeSystemMemory; // the free memory in the device, should be equal to mTotalSystemMemory - mUsedSystemMemory
    private long mAvailableSystemMemory; // the free memory in the device that is available for processes to make use of
    private double mCpuUsage;
    private double mMemoryUsage; // a percentage of the system memory

    /**
     * Get the package name of the application.
     *
     * @return a string representing the application's package name
     */
    public static String getPackageName() {
        return packageName;
    }

    /**
     * Gets the current application's uid.
     *
     * @return an integer representing the application's uid
     */
    public static int getApplicationUid() {
        if (applicationUid == -1) {
            final Pattern pattern = Pattern.compile("userId=(\\d+)");
            final String shellCommand = "dumpsys package " + packageName;

            final Matcher matcher = pattern.matcher(AdbShellUtils.executeShellCommand(shellCommand));

            if (matcher.find()) {
                applicationUid = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
            } else {
                applicationUid = 0;
            }
        }
        return applicationUid;
    }
}
