package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

import org.junit.Assert;
import org.junit.Test;

public class AuthenticationConstantsTest {
    @Test
    public void testComputeMaxBrokerHostVersion() {
        Assert.assertEquals("3.0", AuthenticationConstants.Broker.computeMaxHostBrokerProtocol());
    }
}
