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

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A utility which collects any event's information given by calling markCode on such event(s).
 * These events can be retrieved in csv format by calling method getFileContent.
 * The event is recognized by a string marker which is prefixed with scenario code.
 * MAX_SIZE_CODE_MARKER is the maximum number of markers this utility can have.
 */
public class CodeMarkerManager {

    private boolean enableCodeMarker = false;
    private int MAX_SIZE_CODE_MARKER = 1000;
    private volatile List<CodeMarker> codeMarkers = new ArrayList<CodeMarker>();

    //baseMilliSeconds is the time in milliseconds when first codemarker was captured.
    private long baseMilliSeconds = 0;
    private String scenarioCode = null;
    private static CodeMarkerManager instance = null;

    public static CodeMarkerManager getInstance() {
        if(CodeMarkerManager.instance == null) {
            synchronized (CodeMarkerManager.class) {
                if(CodeMarkerManager.instance == null) {
                    CodeMarkerManager.instance = new CodeMarkerManager();
                }
            }
        }
        return CodeMarkerManager.instance;
    }

    private CodeMarkerManager() {
    }

    public void setPrefixScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    public void markCode(String marker) {
        if(this.enableCodeMarker) {
            if(this.codeMarkers.size() >= MAX_SIZE_CODE_MARKER) {
                clearMarkers();
            }
            long currentMilliSeconds = System.currentTimeMillis();
            if (this.codeMarkers.size() == 0) {
                this.baseMilliSeconds = currentMilliSeconds;
            }
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            this.codeMarkers.add(new CodeMarker((this.scenarioCode == null ? "" : this.scenarioCode) + marker, currentMilliSeconds - this.baseMilliSeconds, f.format(new Date()), Thread.currentThread().getId()));
        }
    }

    public void setEnableCodeMarker(boolean enableCodeMarker) {
        this.enableCodeMarker = enableCodeMarker;
    }

    public void clearMarkers(){
        this.codeMarkers.clear();
    }

    public void clearAll() {
        this.codeMarkers.clear();
        this.scenarioCode = null;
    }

    public String getFileContent() {
        StringBuilder stringToWrite = new StringBuilder("TimeStamp,Marker,Time,Thread,CpuUsed,CpuTotal,ResidentSize,VirtualSize,WifiSent,WifiRecv,WwanSent,WwanRecv,AppSent,AppRecv,Battery,SystemDiskRead,SystemDiskWrite");

        for(CodeMarker codeMarker : this.codeMarkers) {
            StringBuilder thisLine = new StringBuilder();
            thisLine.append("\n").append(codeMarker.getTimeStamp())
                    .append(",").append(codeMarker.getMarker())
                    .append(",").append(codeMarker.getTimeInMilliseconds())
                    .append(",").append(codeMarker.getThreadId())
                    .append(",").append(",NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA,NA");
            stringToWrite.append(thisLine.toString());
        }
        return stringToWrite.toString();
    }
}
