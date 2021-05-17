package com.microsoft.identity.common.java.platform;

public class MockDeviceMetadata implements IDeviceMetadata {

    static final String testCPU = "TestCPU";
    static final String testOS = "TestOS";
    static final String testDeviceModel = "TestDeviceModel";
    static final String testManufacturer = "TestManufacturer";

    @Override
    public String getCpu() {
        return testCPU;
    }

    @Override
    public String getOs() {
        return testOS;
    }

    @Override
    public String getDeviceModel() {
        return testDeviceModel;
    }

    @Override
    public String getManufacturer() {
        return testManufacturer;
    }
}

