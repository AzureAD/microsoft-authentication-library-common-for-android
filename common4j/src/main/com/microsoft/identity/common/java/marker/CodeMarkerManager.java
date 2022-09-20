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

import com.microsoft.identity.common.java.logging.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A utility which collects any event's information and also provides functionality to retrieve the events in csv format.
 */
public class CodeMarkerManager {

    private static final String TAG = CodeMarkerManager.class.getSimpleName();
    private boolean enableCodeMarker = false;
    // MAX_SIZE_CODE_MARKER is the maximum number of markers this utility can have.
    private static final int MAX_SIZE_CODE_MARKER = 1000;
    private final List<CodeMarker> codeMarkers = Collections.synchronizedList(new ArrayList<CodeMarker>());
    //baseMilliSeconds is the time in milliseconds when first code marker was captured.
    private long baseMilliSeconds = 0;
    private String scenarioCode = null;

    private CodeMarkerManager() {
    }

    private static class CodeMarkerHolder {
        static final CodeMarkerManager INSTANCE = new CodeMarkerManager();
    }

    public static CodeMarkerManager getInstance() {
        return CodeMarkerHolder.INSTANCE;
    }

    /**
     * This method captures a particular marker and records the timestamp on which this has been received.
     *
     * @param marker A string code which represents a particular place in code.
     */
    public void markCode(final String marker) {
        if (enableCodeMarker) {
            Logger.info(TAG + ":markCode", "Marking code with " + marker);

            if (codeMarkers.size() >= CodeMarkerManager.MAX_SIZE_CODE_MARKER) {
                clearMarkers();
            }

            final long currentMilliSeconds = System.currentTimeMillis();
            if (codeMarkers.size() == 0) {
                baseMilliSeconds = currentMilliSeconds;
            }

            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            final String applicableMarker = (scenarioCode == null ? "" : scenarioCode) + marker;
            final long timeDiff = currentMilliSeconds - baseMilliSeconds;
            final String date = dateFormat.format(new Date());
            final long threadId = Thread.currentThread().getId();
            final CodeMarker codeMarker = new CodeMarker(applicableMarker, timeDiff, date, threadId);
            codeMarkers.add(codeMarker);
        }
    }

    /**
     * This method sets a scenarioCode Defined in {@link PerfConstants.ScenarioConstants}.
     * This scenario code will be pre-fixed to every code marker.
     *
     * @param scenarioCode a code representing one of the scenarios defined
     */
    public void setPrefixScenarioCode(final String scenarioCode) {
        this.scenarioCode = scenarioCode;
    }

    /**
     * @return whether code marker capturing is enabled
     */
    public boolean codeMarkerIsEnabled() {
        return enableCodeMarker;
    }

    /**
     * This method enables or disables the {@link CodeMarkerManager} as per the argument passed to this.
     * Only enabled {@link CodeMarkerManager} will be able to capture the code markers.
     *
     * @param enableCodeMarker whether to enable code markers
     */
    public void setEnableCodeMarker(final boolean enableCodeMarker) {
        this.enableCodeMarker = enableCodeMarker;
    }

    /**
     * This method clears all the existing markers.
     * This method can be used to start another iteration after capturing the csv content.
     */
    public void clearMarkers() {
        codeMarkers.clear();
    }

    /**
     * This method clears all the existing markers as well as the scenario code which might have been set earlier.
     */
    public void clearAll() {
        clearMarkers();
        scenarioCode = null;
    }

    /**
     * @return the content of all the code markers available till the time converted to CSV which can be directly written to a file.
     */
    public String getFileContent() {
        return getCsvContent();
    }

    /**
     * This method converts list of code markers to csv content which can be written to a file.
     *
     * @return string to save
     */
    public String getCsvContent() {
        if (codeMarkers.isEmpty()) {
            return "";
        }

        final StringBuilder content = new StringBuilder();
        content.append(codeMarkers.get(0).getCsvHeader());

        synchronized (codeMarkers) {
            for (final CodeMarker codeMarker : codeMarkers) {
                content.append('\n');
                content.append(codeMarker.getCsvLine());
            }
        }
        return content.toString();
    }
}
