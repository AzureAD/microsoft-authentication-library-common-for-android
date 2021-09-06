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

import com.microsoft.identity.common.java.util.StringUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This stores a list of {@link NetworkTestState} that define the different states of the network
 * that will be applied during a single test run. The {@link NetworkTestState} are stored in a
 * {@link ConcurrentLinkedQueue} so that each state will be applied in First-In-First-Out order.
 */
public class NetworkTestStateManager {


    /**
     * A CSV file may contain multiple lines each defining a list of network states to be applied to
     * a running test.
     * Therefore, we will create a {@link NetworkTestStateManager} for each line of the CSV. If we
     * are unable to parse
     * the input file, an {@link IllegalArgumentException} will be thrown.
     *
     * @param testClass the calling test class, this will help in reading the
     *                  `androidTest/resources` directory for the specified file.
     * @param fileName  a string representing the name of the CSV file that contains the network
     *                  states.
     * @return a list containing a {@link NetworkTestStateManager}. Each
     * {@link NetworkTestStateManager} represents a line in the file
     */
    public static List<NetworkTestStateManager> readCSVFile(@NonNull final Class<?> testClass, @NonNull final String fileName) {
        final List<NetworkTestStateManager> networkTestStateManagers = new ArrayList<>();

        final InputStream inputStream = testClass.getResourceAsStream(fileName);

        final Scanner scanner = new Scanner(inputStream);

        while (scanner.hasNextLine()) {
            final String line = scanner.nextLine();
            final NetworkTestStateManager networkTestStateManager = new NetworkTestStateManager();

            networkTestStateManager.parseNetworkStates(line);

            networkTestStateManagers.add(networkTestStateManager);
        }

        return networkTestStateManagers;
    }

    private final ConcurrentLinkedQueue<NetworkTestState> states = new ConcurrentLinkedQueue<>();

    public NetworkTestStateManager() {

    }

    /**
     * Parse an input string that represents the network interface states. The result is stored in
     * the "states" class variable.
     *
     * @param networkStates a string representation of the network states for a single test run.
     *                      Example:
     *                      WIFI,1,CELLULAR,2,NONE,7
     */
    private void parseNetworkStates(@NonNull final String networkStates) {
        if (StringUtil.isNullOrEmpty(networkStates)) {
            throw new IllegalArgumentException("The networkStates input string cannot be empty.");
        }

        String[] content = networkStates.split("");

        // There should be an even number of columns
        if (content.length % 2 != 0) {
            throw new IllegalArgumentException("The networkStates input stream is invalid.");
        }

        // Get two columns at a time, both combined represent a NetworkTestState object
        for (int i = 0; i < content.length; i += 2) {
            NetworkTestState networkTestState = new NetworkTestState();
            networkTestState.setInterfaceType(content[i]);
            networkTestState.setTime(Integer.parseInt(content[i + 1]));

            this.states.add(networkTestState);
        }
    }

    public ConcurrentLinkedQueue<NetworkTestState> getStates() {
        return states;
    }
}
