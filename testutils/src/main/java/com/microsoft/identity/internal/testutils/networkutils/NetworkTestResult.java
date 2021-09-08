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
package com.microsoft.identity.internal.testutils.networkutils;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.util.ResultFuture;

import org.junit.Assert;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * It defines the expected test result after running a test under a specific state
 * defined in #{@link NetworkTestingManager}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NetworkTestResult {
    private String id;
    // Whether the test should pass or fail
    private boolean passed;
    // For how long should the test run. (in seconds)
    private int time;
    // A regex for the expected result string representation
    private Pattern resultRegex;
    // The expected exception class
    private Class<?> exceptionClass;
    // The expected exception message
    private String exceptionMessage;


    public <T> void verifyResult(ResultFuture<T> resultFuture, long startTime) {
        final String testId = "Test [" + this.id + "]";
        long timeTaken;
        try {
            final T result = resultFuture.get();

            timeTaken = Math.round((System.currentTimeMillis() - startTime) / 1000.0);

            Assert.assertTrue(testId, this.resultRegex.matcher(String.valueOf(result)).find());
            Assert.assertTrue(testId + " passed but it should have failed.", this.passed);

        } catch (Exception ex) {
            timeTaken = Math.round((System.currentTimeMillis() - startTime) / 1000.0);

            Assert.assertEquals(testId, this.exceptionMessage, ex.getMessage());
            Assert.assertEquals(testId, this.exceptionClass.getName(), ex.getCause().getClass().getName());
            Assert.assertFalse(String.format("%s failed but it should have passed.", testId), this.passed);
        }
        Assert.assertEquals(testId, this.time, timeTaken);
    }


    /**
     * Parses an input string to create a {@link NetworkTestResult} object.
     *
     * @param input a list representing an expected test result.
     *              Example: <b>TEST1,Fail,1,java.lang.RuntimeException,An error occurred</b>
     * @return an object representing a network test failure
     * @throws ClassNotFoundException if the test failure exception class does not exist
     */
    public static NetworkTestResult fromInput(final @NonNull List<String> input) throws ClassNotFoundException {
        final NetworkTestResult networkTestResult = new NetworkTestResult();

        if (input.size() < 4) {
            throw new IllegalArgumentException("Invalid test result input string: " + input);
        }

        networkTestResult.setId(input.get(0));

        if (!(input.get(1).equalsIgnoreCase("Fail") || input.get(1).equalsIgnoreCase("Pass"))) {
            throw new IllegalArgumentException("Invalid pass status \"" + input.get(1) + "\"");
        }

        networkTestResult.setPassed(input.get(1).equalsIgnoreCase("Pass"));
        networkTestResult.setTime(Integer.parseInt(input.get(2)));

        if (networkTestResult.isPassed()) {
            networkTestResult.setResultRegex(Pattern.compile(input.get(3)));
        } else {
            if (input.size() < 5) {
                throw new IllegalArgumentException("Invalid test result input string: " + input);
            }
            networkTestResult.setExceptionMessage(input.get(3));
            networkTestResult.setExceptionClass(Class.forName(input.get(4)));
        }
        return networkTestResult;
    }
}
