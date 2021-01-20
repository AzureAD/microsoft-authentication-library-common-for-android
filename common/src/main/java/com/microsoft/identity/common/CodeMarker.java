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

/**
 * A Class containing information of a code marker which is an event in code.
 * Marker is a string which is prefixed by scenario code and is defined in class {@link CodeMarkerConstants}
 */
public class CodeMarker {

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
    private String  wifiSent = null;
    private String  wifiRecv = null;
    private String  wwanSent = null;
    private String  wwanRecv = null;
    private String  appSent = null;
    private String  appRecv = null;
    private String battery = null;
    private String systemDiskRead = null;
    private String systemDiskWrite = null;

    public CodeMarker(final String marker, final long timeInMilliseconds, final String timeStamp, final long threadId) {
        this.marker = marker;
        this.timeInMilliseconds = timeInMilliseconds;
        this.timeStamp = timeStamp;
        this.threadId = threadId;
    }

    public String getCSVString() {
        StringBuilder csvStringBuilder = new StringBuilder();
        csvStringBuilder.append(this.timeStamp == null ? "NA" : this.timeStamp);
        csvStringBuilder.append(",").append(this.marker == null ? "NA" : this.marker);
        csvStringBuilder.append(",").append(this.timeInMilliseconds);
        csvStringBuilder.append(",").append(this.threadId);
        csvStringBuilder.append(",").append(this.cpuUsed == null ? "NA" : this.cpuUsed);
        csvStringBuilder.append(",").append(this.cpuTotal == null ? "NA" : this.cpuTotal);
        csvStringBuilder.append(",").append(this.residentSize == null ? "NA" : this.residentSize);
        csvStringBuilder.append(",").append(this.virtualSize == null ? "NA" : this.virtualSize);
        csvStringBuilder.append(",").append(this.wifiSent == null ? "NA" : this.wifiSent);
        csvStringBuilder.append(",").append(this.wifiRecv == null ? "NA" : this.wifiRecv);
        csvStringBuilder.append(",").append(this.wwanSent == null ? "NA" : this.wwanSent);
        csvStringBuilder.append(",").append(this.wwanRecv == null ? "NA" : this.wwanRecv);
        csvStringBuilder.append(",").append(this.appSent == null ? "NA" : this.appSent);
        csvStringBuilder.append(",").append(this.appRecv == null ? "NA" : this.appRecv);
        csvStringBuilder.append(",").append(this.battery == null ? "NA" : this.battery);
        csvStringBuilder.append(",").append(this.systemDiskRead == null ? "NA" : this.systemDiskRead);
        csvStringBuilder.append(",").append(this.systemDiskWrite == null ? "NA" : this.systemDiskWrite);

        return csvStringBuilder.toString();
    }

}
