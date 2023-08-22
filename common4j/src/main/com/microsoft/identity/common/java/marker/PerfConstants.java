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

package com.microsoft.identity.common.java.marker;

/**
 * A class having the code marker value of particular event's description.
 * One or many of these can be used while capturing {@link CodeMarker} event by sending as an argument in method call to {@link CodeMarkerManager#markCode(String)} of class {@link CodeMarkerManager}.
 */
public class PerfConstants {

    public static class CodeMarkerConstants {
        public static final String BROKER_OPERATION_EXECUTION_START = "10110";
        public static final String BROKER_PROCESS_START = "10111";
        public static final String BROKER_PROCESS_END = "10120";
        public static final String ACQUIRE_TOKEN_SILENT_START = "10011";
        public static final String ACQUIRE_TOKEN_SILENT_EXECUTOR_START = "10012";
        public static final String ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_START = "10013";
        public static final String ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_END = "10014";
        public static final String ACQUIRE_TOKEN_DCF_START = "10015";
        public static final String ACQUIRE_TOKEN_DCF_EXECUTOR_START = "10016";
        public static final String ACQUIRE_TOKEN_DCF_COMMAND_EXECUTION_START = "10017";
        public static final String ACQUIRE_TOKEN_DCF_COMMAND_EXECUTION_END = "10018";
        public static final String ACQUIRE_TOKEN_DCF_FUTURE_OBJECT_CREATION_END = "10019";
        public static final String ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END = "10020";

        // AT/PoP Code Markers...
        public static final String GENERATE_AT_POP_ASYMMETRIC_KEYPAIR_START = "30001";
        public static final String GENERATE_AT_POP_ASYMMETRIC_KEYPAIR_END = "30002";
    }

    public static class ScenarioConstants {
        public static final String SCENARIO_NON_BROKERED_ACQUIRE_TOKEN_SILENTLY = "100";
        public static final String SCENARIO_BROKERED_ACQUIRE_TOKEN_SILENTLY = "200";
        public static final String SCENARIO_GENERATE_AT_POP_ASYMMETRIC_KEYPAIR = "300";
    }

    public static class CodeMarkerParameters {
        public static final String TIMESTAMP = "TimeStamp";
        public static final String MARKER = "Marker";
        public static final String TIME = "Time";
        public static final String THREAD = "Thread";
        public static final String CPU_USED = "CpuUsed";
        public static final String CPU_TOTAL = "CpuTotal";
        public static final String RESIDENT_SIZE = "ResidentSize";
        public static final String VIRTUAL_SIZE = "VirtualSize";
        public static final String WIFI_SENT = "WifiSent";
        public static final String WIFI_RECEIVED = "WifiRecv";
        public static final String WAN_SENT = "WwanSent";
        public static final String WAN_RECEIVED = "WwanRecv";
        public static final String APP_SENT = "AppSent";
        public static final String APP_RECEIVED = "AppRecv";
        public static final String BATTERY = "Battery";
        public static final String SYSTEM_DISK_READ = "SystemDiskRead";
        public static final String SYSTEM_DISK_WRITE = "SystemDiskWrite";
    }
}
