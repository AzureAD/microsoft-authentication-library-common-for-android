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

public class NetworkTestConstants {

    /**
     * Defines the different network interfaces we can switch to during a test run.
     * The value of the interface holds bit information regarding whether WIFI and CELLULAR are active.
     *
     * 0 - 0,0 [Both WIFI and CELLULAR are OFF]
     * 1 - 0,1 [WIFI is turned ON]
     * 2 - 1,0 [CELLULAR is turned ON]
     * 3 - 1,1 [Both WIFI and CELLULAR are ON]
     */
    public enum InterfaceType {
        NONE("NONE", 0),
        WIFI("WIFI", 1),
        CELLULAR("CELLULAR", 2),
        WIFI_AND_CELLULAR("WIFI_AND_CELLULAR", 3);


        private final String key;
        private final byte value;

        InterfaceType(String key, int value) {
            this.key = key;
            this.value = (byte) value;
        }

        public String getKey() {
            return key;
        }

        /**
         * WIFI active state is stored in the first bit.
         *
         * @return a boolean representing whether WIFI is enabled in this InterfaceType
         */
        public boolean wifiActive() {
            return ((value) & 1) == 1;
        }

        /**
         * CELLULAR active state is stored in the second bit
         *
         * @return a boolean representing whether CELLULAR is enabled in this InterfaceType
         */
        public boolean cellularActive() {
            return ((value >> 1) & 1) == 1;
        }

        @Override
        public String toString() {
            return String.valueOf(key);
        }


        /**
         * Set the interfaceType from a string representation of {@link NetworkTestConstants.InterfaceType}
         *
         * @param value a string representation of {@link NetworkTestConstants.InterfaceType}
         */
        public static InterfaceType fromValue(final String value) {
            for (NetworkTestConstants.InterfaceType interfaceType : NetworkTestConstants.InterfaceType.values()) {
                if (interfaceType.getKey().equals(value)) {
                    return interfaceType;
                }
            }
            return null;
        }
    }

    public static class TimelineEntities {
        public static final String NETWORK_TEST_RUN = "Network test run";
        public static final String TEST_EXECUTION_STAGE = "Test execution stage";
        public static final String NETWORK_STATES = "Applied network states";
    }
}
