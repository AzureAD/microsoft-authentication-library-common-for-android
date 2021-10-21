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

import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.APP_RECEIVED;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.APP_SENT;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.BATTERY;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.CPU_TOTAL;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.CPU_USED;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.MARKER;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.RESIDENT_SIZE;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.SYSTEM_DISK_READ;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.SYSTEM_DISK_WRITE;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.THREAD;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.TIME;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.TIMESTAMP;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.VIRTUAL_SIZE;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.WAN_RECEIVED;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.WAN_SENT;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.WIFI_RECEIVED;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerParameters.WIFI_SENT;

import java.util.LinkedHashMap;

/**
 * A Class containing information of a code marker which is an event in code.
 * Marker is a string which is prefixed by scenario code and is defined in class {@link PerfConstants}
 */
public class CodeMarker {

    private static final String csvNoValue = "NA";
    private static final char csvSeparator = ',';
    private final String marker;
    /* timeInMilliseconds represents time in milliseconds from the time of creation of first codemarker.
    If timeInMilliseconds is zero(0) then it means that this is the first codemarker of the scenario.*/
    private final long timeInMilliseconds;
    // timeStamp is the system time at the time of the capture of the codemarker.
    private final String timeStamp;
    private final long threadId;
    private String cpuUsed = null;
    private String cpuTotal = null;
    private String residentSize = null;
    private String virtualSize = null;
    private String wifiSent = null;
    private String wifiRecv = null;
    private String wwanSent = null;
    private String wwanRecv = null;
    private String appSent = null;
    private String appRecv = null;
    private String battery = null;
    private String systemDiskRead = null;
    private String systemDiskWrite = null;

    public CodeMarker(
            final String marker,
            final long timeInMilliseconds,
            final String timeStamp,
            final long threadId) {
        this.marker = marker;
        this.timeInMilliseconds = timeInMilliseconds;
        this.timeStamp = timeStamp;
        this.threadId = threadId;
    }

    /**
     * Returns list of pairs of (key,value) where key is the representation of heading in csv and value is the measurable value.
     * Used a LinkedHashMap to preserve the insertion order
     */
    private LinkedHashMap<String, String> getKeyValuePairsOfCodeMarker() {
        final LinkedHashMap<String, String> csvKeyValuePairs = new LinkedHashMap<>();
        csvKeyValuePairs.put(TIMESTAMP, timeStamp == null ? CodeMarker.csvNoValue : timeStamp);
        csvKeyValuePairs.put(MARKER, marker == null ? CodeMarker.csvNoValue : marker);
        csvKeyValuePairs.put(TIME, Long.toString(timeInMilliseconds));
        csvKeyValuePairs.put(THREAD, Long.toString(threadId));
        csvKeyValuePairs.put(CPU_USED, cpuUsed == null ? CodeMarker.csvNoValue : cpuUsed);
        csvKeyValuePairs.put(CPU_TOTAL, cpuTotal == null ? CodeMarker.csvNoValue : cpuTotal);
        csvKeyValuePairs.put(
                RESIDENT_SIZE, residentSize == null ? CodeMarker.csvNoValue : residentSize);
        csvKeyValuePairs.put(
                VIRTUAL_SIZE, virtualSize == null ? CodeMarker.csvNoValue : virtualSize);
        csvKeyValuePairs.put(WIFI_SENT, wifiSent == null ? CodeMarker.csvNoValue : wifiSent);
        csvKeyValuePairs.put(WIFI_RECEIVED, wifiRecv == null ? CodeMarker.csvNoValue : wifiRecv);
        csvKeyValuePairs.put(WAN_SENT, wwanSent == null ? CodeMarker.csvNoValue : wwanSent);
        csvKeyValuePairs.put(WAN_RECEIVED, wwanRecv == null ? CodeMarker.csvNoValue : wwanRecv);
        csvKeyValuePairs.put(APP_SENT, appSent == null ? CodeMarker.csvNoValue : appSent);
        csvKeyValuePairs.put(APP_RECEIVED, appRecv == null ? CodeMarker.csvNoValue : appRecv);
        csvKeyValuePairs.put(BATTERY, battery == null ? CodeMarker.csvNoValue : battery);
        csvKeyValuePairs.put(
                SYSTEM_DISK_READ, systemDiskRead == null ? CodeMarker.csvNoValue : systemDiskRead);
        csvKeyValuePairs.put(
                SYSTEM_DISK_WRITE,
                systemDiskWrite == null ? CodeMarker.csvNoValue : systemDiskWrite);
        return csvKeyValuePairs;
    }

    /**
     * Loop through the headers and have them as a csv string
     *
     * @return csv string
     */
    public String getCsvHeader() {
        final StringBuilder csvStringBuilder = new StringBuilder();
        final LinkedHashMap<String, String> csvKeyValuePairs = getKeyValuePairsOfCodeMarker();
        int index = 0;
        for (final String key : csvKeyValuePairs.keySet()) {
            if (index != 0) {
                csvStringBuilder.append(CodeMarker.csvSeparator);
            }
            csvStringBuilder.append(key);
            index++;
        }
        return csvStringBuilder.toString();
    }

    /**
     * Loop through the values and have them as a csv string
     *
     * @return csv string
     */
    public String getCsvLine() {
        final StringBuilder csvStringBuilder = new StringBuilder();
        final LinkedHashMap<String, String> csvKeyValuePairs = getKeyValuePairsOfCodeMarker();
        int index = 0;
        for (final String value : csvKeyValuePairs.values()) {
            if (index != 0) {
                csvStringBuilder.append(CodeMarker.csvSeparator);
            }
            csvStringBuilder.append(value);
            index++;
        }
        return csvStringBuilder.toString();
    }
}
