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

package com.microsoft.identity.common.java.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.NameValuePair;

@RunWith(JUnit4.class)
public class BrokerProtocolVersionUtilTest {

    @Test
    public void testNegotiatedIsLargerThanRequired(){
        final String negotiatedBrokerProtocol = "10.0";
        final String requiredBrokerProtocol = "5.0";
        Assert.assertTrue(
                BrokerProtocolVersionUtil
                        .isProvidedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                                negotiatedBrokerProtocol,
                                requiredBrokerProtocol)
        );
    }

    @Test
    public void testNegotiatedIsSmallerThanRequired(){
        final String negotiatedBrokerProtocol = "5.0";
        final String requiredBrokerProtocol = "10.0";
        Assert.assertFalse(
                BrokerProtocolVersionUtil
                        .isProvidedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                                negotiatedBrokerProtocol,
                                requiredBrokerProtocol)
        );
    }

    @Test
    public void testNegotiatedIsEqualToRequired(){
        final String negotiatedBrokerProtocol = "10.0";
        final String requiredBrokerProtocol = "10.0";
        Assert.assertTrue(
                BrokerProtocolVersionUtil
                        .isProvidedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                                negotiatedBrokerProtocol,
                                requiredBrokerProtocol)
        );
    }

    @Test
    public void testIfNegotiatedIsNullReturnFalse(){
        final String negotiatedBrokerProtocol = null;
        final String requiredBrokerProtocol = "10.0";
        Assert.assertFalse(
                BrokerProtocolVersionUtil
                        .isProvidedBrokerProtocolLargerOrEqualThanRequiredBrokerProtocol(
                                negotiatedBrokerProtocol,
                                requiredBrokerProtocol)
        );
    }

    @Test
    public void testCanCompressBrokerPayloads_NegotiatedLargerThanRequired(){
        Assert.assertTrue(
                BrokerProtocolVersionUtil.canCompressBrokerPayloads("6.0")
        );
    }

    @Test
    public void testCanCompressBrokerPayloads_NegotiatedEqualToRequired(){
        Assert.assertTrue(
                BrokerProtocolVersionUtil.canCompressBrokerPayloads("5.0")
        );
    }

    @Test
    public void testCanCompressBrokerPayloads_NegotiatedSmallerThanRequired(){
        Assert.assertFalse(
                BrokerProtocolVersionUtil.canCompressBrokerPayloads("4.0")
        );
    }

    @Test
    public void testCanCompressBrokerPayloads_NegotiatedIsNull(){
        Assert.assertFalse(
                BrokerProtocolVersionUtil.canCompressBrokerPayloads(null)
        );
    }

    @Test
    public void testCanFociAppsConstructAccountsFromPrtIdTokens_NegotiatedLargerThanRequired(){
        Assert.assertTrue(
                BrokerProtocolVersionUtil.canFociAppsConstructAccountsFromPrtIdTokens("9.0")
        );
    }

    @Test
    public void testCanFociAppsConstructAccountsFromPrtIdTokens_NegotiatedEqualToRequired(){
        Assert.assertTrue(
                BrokerProtocolVersionUtil.canFociAppsConstructAccountsFromPrtIdTokens("8.0")
        );
    }

    @Test
    public void testCanFociAppsConstructAccountsFromPrtIdTokens_NegotiatedSmallerThanRequired(){
        Assert.assertFalse(
                BrokerProtocolVersionUtil.canFociAppsConstructAccountsFromPrtIdTokens("7.0")
        );
    }

    @Test
    public void testCanFociAppsConstructAccountsFromPrtIdTokens_NegotiatedNull(){
        Assert.assertFalse(
                BrokerProtocolVersionUtil.canFociAppsConstructAccountsFromPrtIdTokens(null)
        );
    }

    @Test
    public void testCanSendPKeyAuthHeaderToTheTokenEndpoint_NegotiatedLargerThanRequired(){
        Assert.assertTrue(
                BrokerProtocolVersionUtil.canSendPKeyAuthHeaderToTheTokenEndpoint("10.0")
        );
    }

    @Test
    public void testCanSendPKeyAuthHeaderToTheTokenEndpoint_NegotiatedEqualToRequired(){
        Assert.assertTrue(
                BrokerProtocolVersionUtil.canSendPKeyAuthHeaderToTheTokenEndpoint("9.0")
        );
    }

    @Test
    public void testCanSendPKeyAuthHeaderToTheTokenEndpoint_NegotiatedSmallerThanRequired(){
        Assert.assertFalse(
                BrokerProtocolVersionUtil.canSendPKeyAuthHeaderToTheTokenEndpoint("8.0")
        );
    }

    @Test
    public void testCanSendPKeyAuthHeaderToTheTokenEndpoint_NegotiatedNull(){
        Assert.assertFalse(
                BrokerProtocolVersionUtil.canSendPKeyAuthHeaderToTheTokenEndpoint(null)
        );
    }

    @Test
    public void testcanSupportMsaAccountsInBroker_NegotiatedEqualToRequired(){
        Assert.assertTrue(
                BrokerProtocolVersionUtil.canSupportMsaAccountsInBroker("14.0")
        );
    }

    @Test
    public void testcanSupportMsaAccountsInBroker_NegotiatedLargerThanRequired(){
        Assert.assertTrue(
                BrokerProtocolVersionUtil.canSupportMsaAccountsInBroker("15.0")
        );
    }

    @Test
    public void testcanSupportMsaAccountsInBroker_NegotiatedSmallerThanRequired(){
        Assert.assertFalse(
                BrokerProtocolVersionUtil.canSupportMsaAccountsInBroker("13.0")
        );
    }

    @Test
    public void testcanSupportMsaAccountsInBroker_NegotiatedNull(){
        Assert.assertFalse(
                BrokerProtocolVersionUtil.canSupportMsaAccountsInBroker(null)
        );
    }

}
