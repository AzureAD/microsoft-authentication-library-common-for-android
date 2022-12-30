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
package com.microsoft.identity.common.internal.providers.oauth2;

import com.microsoft.identity.common.java.providers.oauth2.DefaultStateGenerator;
import com.microsoft.identity.common.logging.Logger;

import java.util.Locale;

import lombok.Getter;
import lombok.NonNull;

/**
 * Encodes the Android Task ID (taskId) into the state sent as part of the authorization request.

 * This allows the activity/task used to initiate an interactive request to be linked back to when we receive the authorization result code.
 * This extends the DefaultStateGenerator in order to ensure the state generated is non-guessable.

 */
public class AndroidTaskStateGenerator extends DefaultStateGenerator {

    private static final String SPLITTER = ":";
    private static final String TAG = "AndroidTaskStateGenerator";

    @Getter
    private int taskId;

    public AndroidTaskStateGenerator(final int taskId) {
        this.taskId = taskId;
    }

    @Override
    @NonNull
    public String generate() {
        String state = super.generate();
        // Ensure the string is formatted in only one Locale. Using Locale.US ensures the taskId is always ASCII which is URL safe.
        state = String.format(Locale.US, "%d%s%s", this.taskId, SPLITTER, state);
        return state;
    }

    public static int getTaskFromState(String state) {
        final String methodTag = TAG + ":getTaskFromState";
        String[] parts = state.split(SPLITTER);
        int returnValue = 0;
        if (parts.length >= 2) {
            try {
                returnValue = Integer.parseInt(parts[0]);
            } catch (NumberFormatException ex) {
                Logger.error(methodTag, "Unable to parse state", ex);
            }
        }
        return returnValue;
    }
}
