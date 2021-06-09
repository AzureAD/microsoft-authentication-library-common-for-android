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

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.logging.DiagnosticContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Tests for {@link com.microsoft.identity.common.java.platform.Device}.
 */
public class DeviceTest {

    final String NOT_SET = "NOT_SET";
    final String TEST_VERSION = "TEST_VERSION";

    @After
    public void tearDown() {
        Device.clearDeviceMetadata();
    }

    @Test
    public void testGetDataWhenMetadataIsNotSet(){
        // Shouldn't crash.
        final Map<String, String> platformParameter = Device.getPlatformIdParameters();
        Assert.assertEquals(3, platformParameter.size());
        Assert.assertEquals(NOT_SET, platformParameter.get(Device.PlatformIdParameters.CPU_PLATFORM));
        Assert.assertEquals(NOT_SET, platformParameter.get(Device.PlatformIdParameters.DEVICE_MODEL));
        Assert.assertEquals(NOT_SET, platformParameter.get(Device.PlatformIdParameters.OS));

        Assert.assertEquals(NOT_SET, Device.getManufacturer());
        Assert.assertEquals(NOT_SET, Device.getModel());
        Assert.assertEquals(NOT_SET, Device.getProductVersion());
    }

    @Test
    public void testGetPlatformIdParameters(){
        Device.setDeviceMetadata(new MockDeviceMetadata());

        final Map<String, String> platformParameter = Device.getPlatformIdParameters();
        Assert.assertEquals(3, platformParameter.size());
        Assert.assertEquals(MockDeviceMetadata.TEST_CPU, platformParameter.get(Device.PlatformIdParameters.CPU_PLATFORM));
        Assert.assertEquals(MockDeviceMetadata.TEST_DEVICE_MODEL, platformParameter.get(Device.PlatformIdParameters.DEVICE_MODEL));
        Assert.assertEquals(MockDeviceMetadata.TEST_OS, platformParameter.get(Device.PlatformIdParameters.OS));
    }

    @Test
    public void testGetCpu(){
        Device.setDeviceMetadata(new MockDeviceMetadata());
        Assert.assertEquals(MockDeviceMetadata.TEST_CPU, Device.getCpu());
    }

    @Test
    public void testGetOs(){
        Device.setDeviceMetadata(new MockDeviceMetadata());
        Assert.assertEquals(MockDeviceMetadata.TEST_OS, Device.getOs());
    }

    @Test
    public void testGetProductVersion_DiagContextNotSet(){
        Assert.assertEquals(NOT_SET, Device.getProductVersion());
    }

    @Test
    public void testGetProductVersion(){
        DiagnosticContext.INSTANCE.getRequestContext().put(AuthenticationConstants.SdkPlatformFields.VERSION, TEST_VERSION);
        Assert.assertEquals(TEST_VERSION, Device.getProductVersion());

    }

    @Test
    public void testGetManufacturer(){
        Device.setDeviceMetadata(new MockDeviceMetadata());
        Assert.assertEquals(MockDeviceMetadata.TEST_MANUFACTURER, Device.getManufacturer());
    }


    @Test
    public void testGetModel(){
        Device.setDeviceMetadata(new MockDeviceMetadata());
        Assert.assertEquals(MockDeviceMetadata.TEST_DEVICE_MODEL, Device.getModel());
    }
}

