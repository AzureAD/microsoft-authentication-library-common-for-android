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

public class MockDeviceMetadata extends AbstractDeviceMetadata {

    public static final String TEST_DEVICE_TYPE = "TestDeviceType";
    public static final String TEST_CPU = "TestCPU";
    public static final String TEST_OS_ESTS = "TestOSEsts";
    public static final String TEST_OS_MATS = "TestOSMats";
    public static final String TEST_OS_DRS = "TestOSDrs";
    public static final String TEST_DEVICE_MODEL = "TestDeviceModel";
    public static final String TEST_MANUFACTURER = "TestManufacturer";

    @Override
    public @NonNull String getDeviceType() {
        return TEST_DEVICE_TYPE;
    }

    @Override
    public @NonNull String getCpu() {
        return TEST_CPU;
    }

    @Override
    public @NonNull String getOsForEsts() {
        return TEST_OS_ESTS;
    }

    @Override
    public @NonNull String getOsForMats() {
        return TEST_OS_MATS;
    }

    @Override
    public @NonNull String getOsForDrs() {
        return TEST_OS_DRS;
    }

    @Override
    public @NonNull String getDeviceModel() {
        return TEST_DEVICE_MODEL;
    }

    @Override
    public @NonNull String getManufacturer() {
        return TEST_MANUFACTURER;
    }
}
