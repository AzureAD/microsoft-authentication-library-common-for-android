package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

import org.junit.Assert;
import org.junit.Test;

public class AuthenticationConstantsTest {
    @Test
    public void testComputeMaxBrokerHostVersion() {
        Assert.assertEquals("2.0", AuthenticationConstants.Broker.computeMaxHostBrokerProtocol());
    }
    @Test
    public void testComputeMaxMsalVersion() {
        Assert.assertEquals("7.0", AuthenticationConstants.Broker.computeMaxMsalBrokerProtocol());
    }
}
