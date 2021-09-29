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
package com.microsoft.identity.client.ui.automation.network;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;

import org.junit.Assert;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * It defines the expected test result after running a test under a specific state
 * defined in #{@link NetworkTestStateManager}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NetworkTestResult {
    private String id;
    // Whether the test should pass or fail
    private boolean passed;
    // A regex for the expected result string representation
    private Pattern resultRegex;


    public void verifyResult(final long timeoutSeconds, final ResultFuture<String, Exception> resultFuture, final long startTime) {
        String result;

        boolean testPassed = false;

        try {
            result = resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            testPassed = true;
        } catch (Throwable throwable) {
            result = throwable.getMessage();
        }

        final long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
        final NetworkTestConstants.InterfaceType currentInterface = NetworkTestStateManager.getCurrentInterface();


        Assert.assertTrue(
                String.format(Locale.getDefault(),
                        "[Time: %d, NetworkInterface: %s] => [%s] does not match [%s]", totalSeconds, currentInterface, result, resultRegex.pattern()),
                String.valueOf(result).matches(resultRegex.pattern())
        );

        if (testPassed) {
            Assert.assertTrue(
                    String.format(Locale.getDefault(), "[Time: %d, NetworkInterface: %s] => Passed but it should have failed.", totalSeconds, currentInterface),
                    this.passed
            );
        } else {
            Assert.assertFalse(
                    String.format(Locale.getDefault(), "[Time: %d, NetworkInterface: %s] => Failed but it should have passed.", totalSeconds, currentInterface),
                    this.passed
            );
        }
    }


    /**
     * Parses an input string to create a {@link NetworkTestResult} object.
     *
     * @param input a list representing an expected test result.
     *              Example: <b>TEST1,Fail,1,An error occurred</b>
     * @return an object representing a network test failure
     * @throws ClassNotFoundException if the test failure exception class does not exist
     */
    public static NetworkTestResult fromInput(final @NonNull List<String> input) throws ClassNotFoundException {
        final NetworkTestResult networkTestResult = new NetworkTestResult();

        if (input.size() < 3) {
            throw new IllegalArgumentException("Invalid test result input string: " + input);
        }

        networkTestResult.setId(input.get(0));

        if (!(input.get(1).equalsIgnoreCase("Fail") || input.get(1).equalsIgnoreCase("Pass"))) {
            throw new IllegalArgumentException("Invalid pass status \"" + input.get(1) + "\"");
        }

        networkTestResult.setPassed(input.get(1).equalsIgnoreCase("Pass"));
        networkTestResult.setResultRegex(Pattern.compile(input.get(2)));

        return networkTestResult;
    }
}
