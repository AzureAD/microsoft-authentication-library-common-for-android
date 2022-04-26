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
package com.microsoft.identity.common.java.configuration;

import com.microsoft.identity.common.java.logging.Logger;

import lombok.NonNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Holds configuration settings that are global in scope.
 * Apply to all public client application instances and all requests.
 *
 * @deprecated This class is now replaced with GlobalSettings.
 */
@Getter
@EqualsAndHashCode()
@Builder()
@Deprecated
public class LibraryConfiguration {

    private static final String TAG = LibraryConfiguration.class.getSimpleName();
    private static LibraryConfiguration sInstance = null;

    // static method to create instance of Singleton class
    public synchronized static LibraryConfiguration getInstance() {
        if (sInstance == null) {
            sInstance = createDefaultInstance();
        }

        return sInstance;
    }

    public static synchronized void intializeLibraryConfiguration(@NonNull final LibraryConfiguration config) {
        if (sInstance == null) {
            sInstance = config;
        } else {
            Logger.warn(TAG, "MsalConfiguration was already initialized");
        }
    }

    private static synchronized LibraryConfiguration createDefaultInstance() {
        return LibraryConfiguration.builder().authorizationInCurrentTask(false).refreshInEnabled(false).build();
    }

    /**
     * Controls whether interactive authorization activities (Browser, Embedded, Broker) are
     * launched in the task associated with the activity provided as a parameter to interactive requests
     * The current default behavior of common is to launch the activity in a new Task.
     * This creates effectively 2 task stacks (which can appear as 2 windows in multi-window configurations)
     * The 2 task stacks allows for unexpected user experience when navigating away for authorization UI
     * when the authorizaiton is still in process.
     */
    private boolean authorizationInCurrentTask;

    /**
     * Determined whether refresh_in feature is enabled by client.
     */
    private boolean refreshInEnabled;

}
