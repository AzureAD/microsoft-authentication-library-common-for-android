package com.microsoft.identity.client.ui.automation;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

/**
 * An interface describing a test that can leverage a {@link ITestBroker} installed on the device
 */
public interface IBrokerTest {

    ITestBroker getBroker();

}
