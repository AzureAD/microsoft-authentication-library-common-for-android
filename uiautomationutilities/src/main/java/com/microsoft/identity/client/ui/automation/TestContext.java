package com.microsoft.identity.client.ui.automation;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.device.Device;

import lombok.Getter;
import lombok.NonNull;

/**
 * A class describing the context under which the test is being executed. This includes things like
 * details about the device on which the test is being run.
 */
@Getter
public class TestContext {

    private final Device device;
    private final Context applicationContext;

    private static TestContext sTestContext;

    private TestContext(@NonNull final Device device, @NonNull final Context context) {
        this.device = device;
        this.applicationContext = context;
    }

    public static TestContext getTestContext() {
        if (sTestContext == null) {
            sTestContext = createTestContext();
        }

        return sTestContext;
    }

    private static TestContext createTestContext() {
        final Device testDevice = createDeviceUnderTest();
        final Context context = ApplicationProvider.getApplicationContext();
        return new TestContext(testDevice, context);
    }

    private static Device createDeviceUnderTest() {
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        final int apiLevel = Build.VERSION.SDK_INT;

        return new Device(manufacturer, model, apiLevel);
    }

}
