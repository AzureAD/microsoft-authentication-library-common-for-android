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
package com.microsoft.identity.common.java.util;

import org.junit.Assert;
import org.junit.Test;

import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.RETURN_AUTHORIZATION_REQUEST_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.RESULT_CODE;;

public class LocalBroadcasterTest {

    @Test
    public void testClearReceivers() {
        LocalBroadcaster.INSTANCE.registerCallback(RETURN_AUTHORIZATION_REQUEST_RESULT, propertyBag -> {
            LocalBroadcaster.INSTANCE.unregisterCallback(RETURN_AUTHORIZATION_REQUEST_RESULT);
        });
        Assert.assertEquals(LocalBroadcaster.INSTANCE.hasReceivers(RETURN_AUTHORIZATION_REQUEST_RESULT), true);
        LocalBroadcaster.INSTANCE.clearReceivers();
        Assert.assertEquals(LocalBroadcaster.INSTANCE.hasReceivers(RETURN_AUTHORIZATION_REQUEST_RESULT), false);
    }

    @Test
    public void testResetBroadcast() {
        LocalBroadcaster.INSTANCE.registerCallback(RETURN_AUTHORIZATION_REQUEST_RESULT, propertyBag -> {
            LocalBroadcaster.INSTANCE.unregisterCallback(RETURN_AUTHORIZATION_REQUEST_RESULT);
        });
        Assert.assertEquals(LocalBroadcaster.INSTANCE.hasReceivers(RETURN_AUTHORIZATION_REQUEST_RESULT), true);
        LocalBroadcaster.INSTANCE.clearReceivers();
        LocalBroadcaster.resetBroadcast();
        Assert.assertEquals(LocalBroadcaster.INSTANCE.hasReceivers(RETURN_AUTHORIZATION_REQUEST_RESULT), false);
    }

    @Test
    public void testBroadcast() {
        LocalBroadcaster.INSTANCE.registerCallback(
                RETURN_AUTHORIZATION_REQUEST_RESULT, new LocalBroadcaster.IReceiverCallback() {
                    @Override
                    public void onReceive(PropertyBag propertyBag) {
                        int resultCode = propertyBag.getOrDefault(RESULT_CODE, 0);
                        Assert.assertEquals(resultCode, 1);
                    }
                });
        final PropertyBag propertyBag = new PropertyBag();
        propertyBag.put(RESULT_CODE, 1);
        LocalBroadcaster.INSTANCE.broadcast(RETURN_AUTHORIZATION_REQUEST_RESULT, propertyBag);
    }
}