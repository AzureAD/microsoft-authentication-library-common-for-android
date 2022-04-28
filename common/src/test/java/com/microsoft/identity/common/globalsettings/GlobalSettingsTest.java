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
package com.microsoft.identity.common.globalsettings;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.logging.LoggerConfiguration;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.R;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.File;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class GlobalSettingsTest {
    public static final String DEFAULT_GLOBAL_CONFIG_FILE_PATH = "src/main/res/raw/default_global_config.json";
    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        final Activity mActivity = Mockito.mock(Activity.class);
        Mockito.when(mActivity.getApplicationContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        GlobalSettings.resetInstance();
    }

    @Test
    public void testCanInitializeGlobalWithFile() {
        final File globalConfigFile = new File(DEFAULT_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getSuccessGlobalListener());
    }

    @Test
    public void testCanInitializeGlobalWithResourceID() {
        GlobalSettings.loadGlobalConfigurationFile(mContext, R.raw.default_global_config, getSuccessGlobalListener());
    }

    @Test
    public void testCannotInitializeGlobalTwice() {
        final File globalConfigFile = new File(DEFAULT_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getSuccessGlobalListener());

        final File anotherGlobalConfigFile = new File(DEFAULT_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, anotherGlobalConfigFile, getSecondInitFailureGlobalListener());
    }

    @Test
    public void testGlobalFieldsRead() {
        GlobalSettings.loadGlobalConfigurationFile(mContext, R.raw.default_global_config, getSuccessGlobalListener());
        GlobalSettingsConfiguration config = GlobalSettings.getInstance().getGlobalSettingsConfiguration();
        Assert.assertEquals(config.getRequiredBrokerProtocolVersion(), "3.0");
        Assert.assertTrue(config.isWebViewZoomControlsEnabled());
        Assert.assertTrue(config.isWebViewZoomEnabled());
        Assert.assertTrue(config.isPowerOptCheckEnabled());
        Assert.assertFalse(config.isHandleNullTaskAffinityEnabled());
        Assert.assertFalse(config.isAuthorizationInCurrentTask());

        LoggerConfiguration loggerConfiguration = config.getLoggerConfiguration();
        Assert.assertFalse(loggerConfiguration.isPiiEnabled());
        Assert.assertEquals(Logger.LogLevel.WARN, loggerConfiguration.getLogLevel());
        Assert.assertTrue(loggerConfiguration.isLogcatEnabled());

        List<BrowserDescriptor> browserList = config.getBrowserSafeList();
        Assert.assertEquals(17, browserList.size());
        Assert.assertEquals("com.android.chrome", browserList.get(0).getPackageName());
    }

    @Test
    public void testGlobalFieldsReadAltered() {
        GlobalSettings.loadGlobalConfigurationFile(mContext, R.raw.test_altered_global_config, getSuccessGlobalListener());
        GlobalSettingsConfiguration config = GlobalSettings.getInstance().getGlobalSettingsConfiguration();
        Assert.assertEquals(config.getRequiredBrokerProtocolVersion(), "2.0");
        Assert.assertFalse(config.isWebViewZoomControlsEnabled());
        Assert.assertFalse(config.isWebViewZoomEnabled());
        Assert.assertFalse(config.isPowerOptCheckEnabled());
        Assert.assertTrue(config.isHandleNullTaskAffinityEnabled());
        Assert.assertTrue(config.isAuthorizationInCurrentTask());

        LoggerConfiguration loggerConfiguration = config.getLoggerConfiguration();
        Assert.assertTrue(loggerConfiguration.isPiiEnabled());
        Assert.assertEquals(Logger.LogLevel.INFO, loggerConfiguration.getLogLevel());
        Assert.assertFalse(loggerConfiguration.isLogcatEnabled());

        List<BrowserDescriptor> browserList = config.getBrowserSafeList();
        Assert.assertEquals(1, browserList.size());
        Assert.assertEquals("org.mozilla.firefox", browserList.get(0).getPackageName());
    }

    private GlobalSettings.GlobalSettingsListener getSuccessGlobalListener() {
        return new GlobalSettings.GlobalSettingsListener() {
            @Override
            public void onSuccess(@NonNull String message) {
                // Nothing
            }

            @Override
            public void onError(@NonNull ClientException exception) {
                Assert.fail(exception.getMessage());
            }
        };
    }

    private GlobalSettings.GlobalSettingsListener getSecondInitFailureGlobalListener() {
        return new GlobalSettings.GlobalSettingsListener() {
            @Override
            public void onSuccess(@NonNull String message) {
                Assert.fail("Second initialization was allowed.");
            }

            @Override
            public void onError(@NonNull ClientException exception) {
                Assert.assertEquals(GlobalSettings.GLOBAL_ALREADY_INITIALIZED_ERROR_CODE, exception.getErrorCode());
                Assert.assertEquals(GlobalSettings.GLOBAL_ALREADY_INITIALIZED_ERROR_MESSAGE, exception.getMessage());
            }
        };
    }
}
