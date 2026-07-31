// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.broker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PackageHelperTest {

    private static final String TEST_PACKAGE = "com.example.test";

    private PackageManager mockPackageManager;
    private PackageHelper packageHelper;
    private IFlightsProvider mockFlightsProvider;

    @Before
    public void setUp() {
        mockPackageManager = mock(PackageManager.class);
        packageHelper = new PackageHelper(mockPackageManager);

        mockFlightsProvider = mock(IFlightsProvider.class);
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.USE_ENABLED_SETTING_FOR_PACKAGE_CHECK))
                .thenReturn(false);

        final MockCommonFlightsManager mockFlightsManager = new MockCommonFlightsManager();
        mockFlightsManager.setMockCommonFlightsProvider(mockFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(mockFlightsManager);
    }

    @After
    public void tearDown() {
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void testIsPackageInstalledAndEnabled_packageNotFound_returnsFalse() throws Exception {
        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE, 0))
                .thenThrow(new NameNotFoundException(TEST_PACKAGE));

        assertFalse(packageHelper.isPackageInstalledAndEnabled(TEST_PACKAGE));
    }

    @Test
    public void testIsPackageInstalledAndEnabled_applicationInfoNull_returnsFalse() throws Exception {
        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE, 0))
                .thenReturn(null);

        assertFalse(packageHelper.isPackageInstalledAndEnabled(TEST_PACKAGE));
    }

    @Test
    public void testIsPackageInstalledAndEnabled_packageEnabled_returnsTrue() throws Exception {
        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.enabled = true;
        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE, 0))
                .thenReturn(appInfo);

        assertTrue(packageHelper.isPackageInstalledAndEnabled(TEST_PACKAGE));
    }

    @Test
    public void testIsPackageInstalledAndEnabled_packageDisabled_returnsFalse() throws Exception {
        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.enabled = false;
        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE, 0))
                .thenReturn(appInfo);

        assertFalse(packageHelper.isPackageInstalledAndEnabled(TEST_PACKAGE));
    }

    @Test
    public void testIsPackageInstalledAndEnabled_flightEnabled_usesEnabledSetting() throws Exception {
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.USE_ENABLED_SETTING_FOR_PACKAGE_CHECK))
                .thenReturn(true);

        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.enabled = false;
        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE, 0))
                .thenReturn(appInfo);
        when(mockPackageManager.getApplicationEnabledSetting(TEST_PACKAGE))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        assertTrue(packageHelper.isPackageInstalledAndEnabled(TEST_PACKAGE));
    }

    @Test
    public void testIsPackageInstalledAndEnabled_flightEnabled_enabledSettingThrows_fallsBackToAppInfo() throws Exception {
        when(mockFlightsProvider.isFlightEnabled(CommonFlight.USE_ENABLED_SETTING_FOR_PACKAGE_CHECK))
                .thenReturn(true);

        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.enabled = true;
        when(mockPackageManager.getApplicationInfo(TEST_PACKAGE, 0))
                .thenReturn(appInfo);
        when(mockPackageManager.getApplicationEnabledSetting(TEST_PACKAGE))
                .thenThrow(new IllegalArgumentException("test"));

        assertTrue(packageHelper.isPackageInstalledAndEnabled(TEST_PACKAGE));
    }
}
