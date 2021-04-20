package com.microsoft.identity.common.java.internal.telemetry;

import com.microsoft.identity.common.java.InMemoryStorage;

class MockTelemetryContext extends AbstractTelemetryContext {
    public MockTelemetryContext(){
        super(new InMemoryStorage());
        addApplicationInfo("TestApp", "1.0", "100XXX");
        addOsInfo("TestOS", "1.0");
        addDeviceInfo("SomeManufacturer", "SomeModel", "SomeDevice");
    }
}
