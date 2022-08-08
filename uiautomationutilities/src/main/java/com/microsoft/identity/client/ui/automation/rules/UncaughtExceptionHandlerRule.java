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
package com.microsoft.identity.client.ui.automation.rules;

import android.util.Log;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A Test Rule to set a default handler for uncaught exceptions from threads that may crash during automation.
 */
public class UncaughtExceptionHandlerRule implements TestRule {

    private final Queue<Throwable> caughtThrowables = new ConcurrentLinkedQueue<>();

    private final static String TAG = UncaughtExceptionHandlerRule.class.getSimpleName();

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    // Create the exception handler
                    final Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread t, Throwable e) {
                            Log.i(TAG, "Exception caught from " + t.getName());
                            e.printStackTrace();
                            caughtThrowables.add(e);
                        }
                    };

                    // Set the handler as the default for uncaught exceptions
                    Thread.setDefaultUncaughtExceptionHandler(handler);

                    base.evaluate();
                } finally {
                    // After Test is executed, check to see if there are uncaught throwables
                    if (!caughtThrowables.isEmpty()) {
                        final StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Caught the following exception from non-main threads:\n");
                        for (Throwable e : caughtThrowables) {
                            stringBuilder.append(e.getMessage());
                            stringBuilder.append("\n");
                        }

                        throw new AssertionError(stringBuilder.toString());
                    }
                }
            }
        };
    }
}
