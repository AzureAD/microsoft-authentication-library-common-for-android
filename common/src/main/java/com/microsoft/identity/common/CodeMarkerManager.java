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
    private List<CodeMarker> codeMarkers = new ArrayList<CodeMarker>();

    //baseMilliSeconds is the time in milliseconds when first codemarker was captured.
    private long baseMilliSeconds = 0;
    private String scenarioCode = null;
    private static CodeMarkerManager sCodeMarkerManager = new CodeMarkerManager();

    public static CodeMarkerManager getInstance() {
        return CodeMarkerManager.sCodeMarkerManager;
    }

    private CodeMarkerManager() {
    }

    /**
     * This method sets a scenarioCode Defined in {@link PerfConstants.ScenarioConstants}.
     * This scenario code will be pre-fixed to every code marker.
     * @param scenarioCode
     */
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

    /**
     * This method enables or disables the {@link CodeMarkerManager} as per the argument passed to this.
     * Only enabled {@link CodeMarkerManager} will be able to capture the codemarkers.
     * @param enableCodeMarker
     */
    public void setEnableCodeMarker(boolean enableCodeMarker) {
        this.enableCodeMarker = enableCodeMarker;
    }

    /**
     * This medhod clears all the existing markers.
     * This medhod can be used to start another iteration after capturing the csv content.
     */
    public void clearMarkers(){
        this.codeMarkers.clear();
    }

    /**
     * This method clears all the existing markers as well as the scenario code which might have been set earlier.
     */
    public void clearAll() {
        this.codeMarkers.clear();
        this.scenarioCode = null;
    }

    /**
     * This method returns the content of all the codemarkers available till the time converted to CSV which can be directly written to a file.
     * @return
     */
    public String getFileContent() {
        return CodeMarkerUtil.getCSVContent(this.codeMarkers);
    }
}
