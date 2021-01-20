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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A utility which collects any event's information and also provides functionality to retrieve the events in csv format.
 */
public class CodeMarkerManager {

    private boolean enableCodeMarker = false;
    // MAX_SIZE_CODE_MARKER is the maximum number of markers this utility can have.
    private static int MAX_SIZE_CODE_MARKER = 1000;
    private volatile List<CodeMarker> codeMarkers = new ArrayList<CodeMarker>();

    //baseMilliSeconds is the time in milliseconds when first codemarker was captured.
    private long baseMilliSeconds = 0;
    private String scenarioCode = null;
    private static CodeMarkerManager sCodeMarkerManager = null;

    public static CodeMarkerManager getInstance() {
        if(CodeMarkerManager.sCodeMarkerManager == null) {
            synchronized (CodeMarkerManager.class) {
                if(CodeMarkerManager.sCodeMarkerManager == null) {
                    CodeMarkerManager.sCodeMarkerManager = new CodeMarkerManager();
                }
            }
        }
        return CodeMarkerManager.sCodeMarkerManager;
    }

    private CodeMarkerManager() {
    }

    public void setPrefixScenarioCode(String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    /**
     * This method captures a particular marker and records the timestamp on which this has been received.
     * @param marker : A string code which represents a particular place in code.
     */
    public void markCode(String marker) {
        if(this.enableCodeMarker) {
            if(this.codeMarkers.size() >= CodeMarkerManager.MAX_SIZE_CODE_MARKER) {
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
        return CodeMarkerUtil.getCSVContent(this.codeMarkers);
    }
}
