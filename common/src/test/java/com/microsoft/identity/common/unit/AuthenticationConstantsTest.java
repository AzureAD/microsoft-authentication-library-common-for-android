package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AuthenticationConstantsTest {
    @Test
    public void testComputeMaxBrokerHostVersion() {
        Assert.assertEquals("5.0", AuthenticationConstants.Broker.computeMaxHostBrokerProtocol());
    }
}
