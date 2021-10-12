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
package com.microsoft.identity.common.java.platform;

import lombok.NonNull;

/**
 * An interface representing metadata of the device.
 * The implementation of its child varies from platform to platform.
 */
public interface IDeviceMetadata {

    @NonNull
    String getDeviceType();

    /**
     * Get the CPU name of this device.
     *
     * @return a String representing the CPU name
     */
    @NonNull
    String getCpu();

    /**
     * Get the OS of this device to be sent to eSTS.
     *
     * We have this because in Android, we're sending [SDK VERSION] (i.e. 24) to eSTS,
     * not the [OS VERSION] (i.e. 7.0), and eSTS are using these [SDK VERSION] to gate features.
     *
     * @return a String representing the OS information
     */
    @NonNull
    String getOsForEsts();

    /**
     * Get the OS of this device to be sent to DRS.
     *
     * @return a String representing the OS information
     */
    @NonNull
    String getOsForDrs();

    /**
     * Get the OS of this device to be sent to MATS.
     */
    @NonNull
    String getOsForMats();

    /**
     * Get the model name of this device.
     *
     * @return a String representing the device's model
     */
    @NonNull
    String getDeviceModel();

    /**
     * Get the manufacturer of this device.
     *
     * @return a String representing this device's manufacturer
     */
    @NonNull
    String getManufacturer();

    /**
     * Get all metadata about this device.
     *
     * @return a String containing all metadata about this device
     */
    String getAllMetadata();
}
