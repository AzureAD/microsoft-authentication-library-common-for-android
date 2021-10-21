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
package com.microsoft.identity.client.ui.automation.logging.appender;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.logging.Logger;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A registry that holds all the loggers in use by the {@link Logger}.
 */
public class AppenderRegistry {

    private static final AppenderRegistry INSTANCE = new AppenderRegistry();

    private final Set<IAppender> mRegisteredAppenders = new LinkedHashSet<>();

    private AppenderRegistry() {}

    public static AppenderRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register a new appender to be used with {@link Logger}.
     *
     * @param appender the appender to register
     */
    public void registerAppender(@NonNull final IAppender appender) {
        mRegisteredAppenders.add(appender);
    }

    /**
     * Unregister a appender that was being used with {@link Logger}.
     *
     * @param appender the appender to unregister
     */
    public void unregisterAppender(@NonNull final IAppender appender) {
        mRegisteredAppenders.remove(appender);
    }

    /**
     * Get all the appenders currently registered with the {@link Logger}.
     *
     * @return a set containging loggers
     */
    public Set<IAppender> getRegisteredAppenders() {
        return mRegisteredAppenders;
    }

    /**
     * Remove all appenders currently registered with {@link Logger}.
     */
    public void unregisterAllAppenders() {
        mRegisteredAppenders.clear();
    }
}
