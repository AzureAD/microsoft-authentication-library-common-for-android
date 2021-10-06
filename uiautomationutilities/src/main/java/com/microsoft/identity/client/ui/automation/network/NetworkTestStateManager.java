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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.internal.testutils.kusto.CSVReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This stores a list of {@link NetworkTestState} that define the different states of the network
 * that will be applied during a single test run.
 */
public class NetworkTestStateManager {


    private final String TAG = NetworkTestStateManager.class.getSimpleName();

    private static NetworkTestConstants.InterfaceType currentInterface = null;


    /**
     * Reset the network state to WIFI
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void resetNetworkState(final Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        final ConnectivityManager.NetworkCallback networkCallback;

        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
            NetworkTestStateManager.changeNetworkState(NetworkTestConstants.InterfaceType.WIFI_AND_CELLULAR);

            final CountDownLatch wifiWaiter = new CountDownLatch(1);

            connectivityManager.registerDefaultNetworkCallback(networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    wifiWaiter.countDown();
                }
            });

            try {
                // If the device is not connected to the internet, wait for WIFI to turn ON
                wifiWaiter.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }

            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }


    /**
     * Changes the state of the network by executing shell commands to turn WIFI and mobile data ON/OFF.
     *
     * @param interfaceType a {@link NetworkTestConstants.InterfaceType}
     */
    public static void changeNetworkState(
            @NonNull final NetworkTestConstants.InterfaceType interfaceType
    ) {
        currentInterface = interfaceType;
        AdbShellUtils.executeShellCommand("svc wifi " + (interfaceType.wifiActive() ? "enable" : "disable"));
        AdbShellUtils.executeShellCommand("svc data " + (interfaceType.cellularActive() ? "enable" : "disable"));
    }


    /**
     * Returns the {@link NetworkTestConstants.InterfaceType} that is currently applied.
     *
     * @return the {@link NetworkTestConstants.InterfaceType} that is currently applied to the device.
     */
    public static NetworkTestConstants.InterfaceType getCurrentInterface() {
        return currentInterface;
    }


    /**
     * An input CSV file may contain multiple lines each defining a list of network states to be applied to
     * a running test.
     * Therefore, we will create a {@link NetworkTestStateManager} for each line of the input CSV. We will
     * also read the corresponding output file, and create {@link NetworkTestResult} for each of the
     * {@link NetworkTestStateManager}.
     * If we are unable to parse
     * the input/output file, an {@link IllegalArgumentException} will be thrown.
     *
     * @param testClass the calling test class, this will help in reading the
     *                  `androidTest/resources` directory for the specified file.
     * @param inputFile a string representing the name of the CSV file that contains the network
     *                  states.
     * @return a list containing a {@link NetworkTestStateManager}. Each
     * {@link NetworkTestStateManager} represents a line in the file
     */
    public static List<NetworkTestStateManager> readCSVFile(
            @NonNull final Class<?> testClass,
            @NonNull final String inputFile
    ) throws IOException {
        final List<NetworkTestStateManager> stateManagers = new ArrayList<>();

        final List<List<String>> input = readFile(testClass, inputFile);

        for (int i = 0; i < input.size(); i++) {
            final NetworkTestStateManager networkTestStateManager = new NetworkTestStateManager();

            networkTestStateManager.parseNetworkStates(input.get(i));

            stateManagers.add(networkTestStateManager);
        }

        return stateManagers;
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
    private String id;
    private boolean ignored;

    public NetworkTestStateManager() {
    }

    /**
     * Parse an input string that represents the network interface states. The result is stored in
     * the "states" class variable.
     *
     * @param networkStates a list representation of the network states for a single test run. The
     *                      first column represents the ID of the state list
     *                      Example:
     *                      TEST1,WIFI,1,CELLULAR,2,NONE,7
     */
    private void parseNetworkStates(@NonNull final List<String> networkStates) {
        // There should be an even number of columns, excluding the ID
        if (networkStates.size() % 2 == 0) {
            throw new IllegalArgumentException("The networkStatesInput input stream is invalid.");
        }

        this.id = networkStates.get(0);

        this.ignored = this.id.endsWith("-ignore");
        this.id = this.id.replaceAll("(-ignore)$", "");

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
                        if (state.getTime() > 0) {
                            switchState(state);
                            Thread.sleep(state.getTime() * 1000);
                        } else {
                            Logger.i(TAG, "Cannot apply network state [" + state.getInterfaceType() + "] with time: " + state.getTime());
                        }
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
        changeNetworkState(nextState.getInterfaceType());

        Logger.i(TAG, "Switching network state to [" + nextState.getInterfaceType() + "] for " + nextState.getTime() + "s ");
        Log.d(TAG, "Switching network state to [" + nextState.getInterfaceType() + "] for " + nextState.getTime() + "s");
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
     * Determine whether a test will use this state manager.
     *
     * @return a boolean value that defines whether this state manager will be ignored.
     */
    public boolean isIgnored() {
        return this.ignored;
    }
}
