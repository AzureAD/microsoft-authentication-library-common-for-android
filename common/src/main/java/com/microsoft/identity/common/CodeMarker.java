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
package com.microsoft.identity.common;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * A Class containing information of a code marker which is an event in code.
 * Marker is a string which is prefixed by scenario code and is defined in class {@link PerfConstants}
 */
public class CodeMarker {

    private static String csvNoValue = "NA";
    private static char csvSeparator = ',';

    private String marker;
    /* timeInMilliseconds represents time in milliseconds from the time of creation of first codemarker.
    If timeInMilliseconds is zero(0) then it means that this is the first codemarker of the scenario.*/
    private long timeInMilliseconds;
    // timeStamp is the system time at the time of the capture of the codemarker.
    private String timeStamp;
    private long threadId;
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

    public CodeMarker(final String marker, final long timeInMilliseconds, final String timeStamp, final long threadId) {
        this.marker = marker;
        this.timeInMilliseconds = timeInMilliseconds;
        this.timeStamp = timeStamp;
        this.threadId = threadId;
    }

    // Returns list of pairs of (key,value) where key is the representation of heading in csv and value is the measurable value.
    private List<Pair<String,String>> getKeyValuePairsOfCodeMarker() {
        List<Pair<String,String>> csvKeyValuePairs = new ArrayList<Pair<String,String>>();
        csvKeyValuePairs.add(new Pair("TimeStamp", this.timeStamp == null ? CodeMarker.csvNoValue : this.timeStamp));
        csvKeyValuePairs.add(new Pair("Marker", this.marker == null ? CodeMarker.csvNoValue : this.marker));
        csvKeyValuePairs.add(new Pair("Time", this.timeInMilliseconds));
        csvKeyValuePairs.add(new Pair("Thread", this.threadId));
        csvKeyValuePairs.add(new Pair("CpuUsed", this.cpuUsed == null ? CodeMarker.csvNoValue : this.cpuUsed));
        csvKeyValuePairs.add(new Pair("CpuTotal", this.cpuTotal == null ? CodeMarker.csvNoValue : this.cpuTotal));
        csvKeyValuePairs.add(new Pair("ResidentSize", this.residentSize == null ? CodeMarker.csvNoValue : this.residentSize));
        csvKeyValuePairs.add(new Pair("VirtualSize", this.virtualSize == null ? CodeMarker.csvNoValue : this.virtualSize));
        csvKeyValuePairs.add(new Pair("WifiSent", this.wifiSent == null ? CodeMarker.csvNoValue : this.wifiSent));
        csvKeyValuePairs.add(new Pair("WifiRecv", this.wifiRecv == null ? CodeMarker.csvNoValue : this.wifiRecv));
        csvKeyValuePairs.add(new Pair("WwanSent", this.wwanSent == null ? CodeMarker.csvNoValue : this.wwanSent));
        csvKeyValuePairs.add(new Pair("WwanRecv", this.wwanRecv == null ? CodeMarker.csvNoValue : this.wwanRecv));
        csvKeyValuePairs.add(new Pair("AppSent", this.appSent == null ? CodeMarker.csvNoValue : this.appSent));
        csvKeyValuePairs.add(new Pair("AppRecv", this.appRecv == null ? CodeMarker.csvNoValue : this.appRecv));
        csvKeyValuePairs.add(new Pair("Battery", this.battery == null ? CodeMarker.csvNoValue : this.battery));
        csvKeyValuePairs.add(new Pair("SystemDiskRead", this.systemDiskRead == null ? CodeMarker.csvNoValue : this.systemDiskRead));
        csvKeyValuePairs.add(new Pair("SystemDiskWrite", this.systemDiskWrite == null ? CodeMarker.csvNoValue : this.systemDiskWrite));
        return csvKeyValuePairs;
    }

    public String getCSVHeader() {
        StringBuilder csvStringBuilder = new StringBuilder();
        List<Pair<String,String>> csvKeyValuePairs = getKeyValuePairsOfCodeMarker();
        for(int i = 0; i < csvKeyValuePairs.size(); i++) {
            if(i != 0) {
                csvStringBuilder.append(CodeMarker.csvSeparator);
            }
            csvStringBuilder.append(csvKeyValuePairs.get(i).first);
        }
        return csvStringBuilder.toString();
    }

    public String getCSVLine() {
        StringBuilder csvStringBuilder = new StringBuilder();
        List<Pair<String,String>> csvKeyValuePairs = getKeyValuePairsOfCodeMarker();
        for(int i = 0; i < csvKeyValuePairs.size(); i++) {
            if(i != 0) {
                csvStringBuilder.append(CodeMarker.csvSeparator);
            }
            csvStringBuilder.append(csvKeyValuePairs.get(i).second);
        }
        return csvStringBuilder.toString();
    }

}
