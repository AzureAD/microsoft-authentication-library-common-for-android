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

import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.internal.testutils.IShellCommandExecutor;
import com.microsoft.identity.internal.testutils.kusto.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import lombok.SneakyThrows;

/**
 * This stores a list of {@link NetworkTestState} that define the different states of the network
 * that will be applied during a single test run.
 */
public class NetworkTestingManager {


    private final String TAG = NetworkTestingManager.class.getSimpleName();


    /**
     * An input CSV file may contain multiple lines each defining a list of network states to be applied to
     * a running test.
     * Therefore, we will create a {@link NetworkTestingManager} for each line of the input CSV. We will
     * also read the corresponding output file, and create {@link NetworkTestResult} for each of the
     * {@link NetworkTestingManager}.
     * If we are unable to parse
     * the input/output file, an {@link IllegalArgumentException} will be thrown.
     *
     * @param testClass            the calling test class, this will help in reading the
     *                             `androidTest/resources` directory for the specified file.
     * @param shellCommandExecutor an object to allow running shell commands to manage network state
     * @param inputFile            a string representing the name of the CSV file that contains the network
     *                             states.
     * @param expectedResultFile   a string representing the name of the CSV file that defines the expected result after
     *                             the the test is run
     * @return a list containing a {@link NetworkTestingManager}. Each
     * {@link NetworkTestingManager} represents a line in the file
     */
    public static List<NetworkTestingManager> readCSVFile(
            @NonNull final Class<?> testClass,
            @NonNull IShellCommandExecutor shellCommandExecutor,
            @NonNull final String inputFile,
            @NonNull final String expectedResultFile
    ) throws ClassNotFoundException, IOException {
        final List<NetworkTestingManager> networkTestingManagers = new ArrayList<>();

        final List<List<String>> input = readFile(testClass, inputFile);
        final List<List<String>> expectedResult = readFile(testClass, expectedResultFile);

        if (input.size() != expectedResult.size()) {
            throw new IllegalArgumentException("The input and expected result should have the same number of test runs.");
        }

        for (int i = 0; i < input.size(); i++) {
            final NetworkTestingManager networkTestingManager = new NetworkTestingManager(shellCommandExecutor);

            networkTestingManager.parseNetworkStates(input.get(i), expectedResult.get(i));

            networkTestingManagers.add(networkTestingManager);
        }

        return networkTestingManagers;
    }


    /**
     * Read a file and return the text as an array of strings, each array item defines a line
     * in the file.
     */
    private static List<List<String>> readFile(final Class<?> testClass, final String fileName) throws IOException {
        final InputStream inputStream = testClass.getResourceAsStream(fileName);

        return new CSVReader(new InputStreamReader(inputStream)).read();
    }

    private List<NetworkTestState> states;
    private final IShellCommandExecutor shellCommandExecutor;
    private String id;
    private NetworkTestResult testResult;

    public NetworkTestingManager(IShellCommandExecutor shellCommandExecutor) {
        this.shellCommandExecutor = shellCommandExecutor;
    }

    /**
     * Parse an input string that represents the network interface states. The result is stored in
     * the "states" class variable.
     *
     * @param networkStates  a list representation of the network states for a single test run. The
     *                       first column represents the ID of the state list
     *                       Example:
     *                       TEST1,WIFI,1,CELLULAR,2,NONE,7
     * @param expectedResult a list representation of the expected result after running a test under the
     *                       defined networkStatesInput.
     *                       Example:
     *                       TEST1,Fail,1,java.lang.RuntimeException,An error occurred
     */
    private void parseNetworkStates(
            @NonNull final List<String> networkStates,
            @NonNull final List<String> expectedResult
    ) throws ClassNotFoundException {
        // There should be an even number of columns, excluding the ID
        if (networkStates.size() % 2 == 0) {
            throw new IllegalArgumentException("The networkStatesInput input stream is invalid.");
        }

        if (!networkStates.get(0).equals(expectedResult.get(0))) {
            throw new IllegalArgumentException(
                    "The input [" + networkStates + "] and expected result [" + expectedResult + "] should have the same ID"
            );
        }

        this.id = networkStates.get(0);

        // Read the input

        final List<NetworkTestState> stateList = new ArrayList<>();

        // Get two columns at a time, both combined represent a NetworkTestState object
        for (int i = 1; i < networkStates.size(); i += 2) {
            NetworkTestState networkTestState = new NetworkTestState();
            networkTestState.setInterfaceType(NetworkTestConstants.InterfaceType.fromValue(networkStates.get(i)));
            networkTestState.setTime(Integer.parseInt(networkStates.get(i + 1)));

            if (networkTestState.getInterfaceType() == null) {
                throw new IllegalArgumentException(
                        "Invalid interface type [" + networkStates.get(i) + "]. Interface should be one of: "
                                + Arrays.toString(NetworkTestConstants.InterfaceType.values())
                );
            }

            stateList.add(networkTestState);
        }

        // Read the expected result
        this.testResult = NetworkTestResult.fromInput(expectedResult);
        this.states = Collections.unmodifiableList(stateList);
    }

    public List<NetworkTestState> getStates() {
        return states;
    }

    /**
     * Start a thread that will switch the network based on the state defined.
     */
    public Thread execute() {
        if (states.isEmpty()) {
            throw new IllegalArgumentException("Cannot execute when no network states have been defined.");
        }

        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (NetworkTestState state : states) {
                        switchState(state);
                        Thread.sleep(state.getTime() * 1000);
                    }
                } catch (InterruptedException ignored) {
                }
            }
        });

    }


    /**
     * Handle the switching of the network from one state to another. We first set the state to NONE, then switch.
     *
     * @param nextState the next network state
     */
    public void switchState(@NonNull final NetworkTestState nextState) {
        changeNetworkState(shellCommandExecutor, nextState.getInterfaceType());

        Logger.info(TAG, "Switching network state to [" + nextState.getInterfaceType() + "] for " + nextState.getTime() + "s ");
    }

    /**
     * Get the ID of the state list.
     *
     * @return a string representation of the id of the network states
     */
    public String getId() {
        return id;
    }

    /**
     * Get the expected result of the test after the network states are applied.
     *
     * @return a {@link NetworkTestResult}
     */
    public NetworkTestResult getTestResult() {
        return testResult;
    }

    /**
     * Changes the state of the network by executing shell commands to turn WIFI and mobile data ON/OFF.
     *
     * @param shellCommandExecutor to run a shell command on the device
     * @param interfaceType        a {@link NetworkTestConstants.InterfaceType}
     */
    public static void changeNetworkState(
            @NonNull final IShellCommandExecutor shellCommandExecutor,
            @NonNull final NetworkTestConstants.InterfaceType interfaceType
    ) {
        shellCommandExecutor.execute("svc wifi " + (interfaceType.wifiActive() ? "enable" : "disable"));
        shellCommandExecutor.execute("svc data " + (interfaceType.cellularActive() ? "enable" : "disable"));
    }
}
