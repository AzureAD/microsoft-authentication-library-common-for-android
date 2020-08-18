// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.internal.testutils.kusto;

import android.util.Log;

import com.microsoft.identity.common.BuildConfig;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.sql.Timestamp;

/**
 * A JUnit rule to capture test results and save to a CSV File.
 */
public class CaptureKustoTestResultRule implements TestRule {

    private final String TAG = CaptureKustoTestResultRule.class.getSimpleName();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Log.i(TAG, "Starting to write test results to file.");
                final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                final String runnerInstance = "MSAL RobolectricTest";
                final String runnerVersion = "";
                final String scaleUnit = BuildConfig.DC;
                final String testName = description.getDisplayName();
                String result = "", errorMessage = "";
                try {
                    base.evaluate();
                    result = "PASS";
                } catch (final Throwable throwable) {
                    result = "FAIL";
                    errorMessage = throwable.getMessage();
                    throw throwable;
                } finally {
                    final EstsKustoClientTestTableData clientTestTableData =
                            EstsKustoClientTestTableData.builder()
                                    .testName(testName)
                                    .timestamp(timestamp)
                                    .runnerInstance(runnerInstance)
                                    .runnerVersion(runnerVersion)
                                    .result(result)
                                    .scaleUnit(scaleUnit)
                                    .errorMessage(errorMessage)
                                    .build();

                    TestResultFileUtils.writeTestResultsToCsv(
                            clientTestTableData
                    );
                }
            }
        };
    }
}
