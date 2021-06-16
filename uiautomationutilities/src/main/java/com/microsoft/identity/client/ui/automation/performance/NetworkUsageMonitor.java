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

import android.net.TrafficStats;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Collects network traffic information by the current process
 * It will tend to store the previous traffic information in order to provide a diff showing how much more bytes have been sent/received.
 */
public class NetworkUsageMonitor implements PerformanceProfileMonitor<NetworkUsageMonitor.TrafficInfo> {

    private static TrafficInfo prevTrafficInfo = null;

    //     Load the traffic information as early as possible
    static {
        loadTrafficInfo(DeviceMonitor.getApplicationUid());
    }

    @Override
    public TrafficInfo getStats(ProcessInfo processInfo) {
        return loadTrafficInfo(processInfo.getUid());
    }


    private static TrafficInfo loadTrafficInfo(int uid) {
        TrafficInfo trafficInfo = new TrafficInfo();

        trafficInfo.setTrafficInfo(
                TrafficStats.getUidTxBytes(uid),
                TrafficStats.getUidRxBytes(uid),
                prevTrafficInfo
        );

        prevTrafficInfo = trafficInfo;
        return trafficInfo;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(prefix = "m")
    public static class TrafficInfo {
        private long mTotalBytesSent;
        private long mTotalBytesReceived;
        private long mDiffBytesSent;
        private long mDiffBytesReceived;

        /**
         * Updates the total bytes sent by the application since device boot, and also stores information on how much more bytes
         * have been sent since the last query
         *
         * @param bytesSent       the total bytes sent so far
         * @param bytesReceived   the total bytes received so far
         * @param prevTrafficInfo the previous TrafficInfo queried.
         */
        private void setTrafficInfo(long bytesSent, long bytesReceived, TrafficInfo prevTrafficInfo) {
            mTotalBytesReceived = bytesReceived;
            mTotalBytesSent = bytesSent;
            if (prevTrafficInfo != null) {
                mDiffBytesReceived = mTotalBytesReceived - prevTrafficInfo.getTotalBytesReceived();
                mDiffBytesSent = mTotalBytesSent - prevTrafficInfo.getTotalBytesSent();
            }
        }
    }
}
