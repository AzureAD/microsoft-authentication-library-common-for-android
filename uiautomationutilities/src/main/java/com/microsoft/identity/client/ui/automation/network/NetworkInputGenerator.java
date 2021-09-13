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

import java.util.Random;

/**
 * Generates random network states for running a test.
 */
public class NetworkInputGenerator {

    // The maximum number of seconds a network interface can be active for.
    private static final int MAX_STATE_TIME = 10;

    /**
     * @param numTests  the number of tests that will be run from this input
     * @param maxTime   the maximum time (seconds) in which each test will run.
     * @param separator the CSV column separator
     * @return a CSV string output of the generated network states.
     */
    public static String generate(final int numTests, final int maxTime, final char separator) {
        final StringBuilder stringBuilder = new StringBuilder();

        final NetworkTestConstants.InterfaceType[] interfaceTypes = NetworkTestConstants.InterfaceType.values();

        final Random random = new Random();

        for (int i = 0; i < numTests; i++) {
            int currentTime = 0;

            stringBuilder.append("TEST").append(i + 1).append(separator); // generate test id

            while (currentTime < maxTime) {
                int randomTime = Math.min(random.nextInt(MAX_STATE_TIME), maxTime - currentTime);

                NetworkTestConstants.InterfaceType randomInterface =
                        interfaceTypes[random.nextInt(interfaceTypes.length)];

                stringBuilder.append(randomInterface.getKey()).append(separator)
                        .append(randomTime);

                currentTime += randomTime;

                if (currentTime != maxTime) {
                    stringBuilder.append(separator);
                }
            }

            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }
}
