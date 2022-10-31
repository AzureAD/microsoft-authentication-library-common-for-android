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
package com.microsoft.identity.common.internal.commands.parameters;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.util.ClockSkewManager;
import com.microsoft.identity.common.java.util.UrlUtil;
import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class GenerateShrCommandParametersTest {
    private static final CommandCallback EMPTY_CALLBACK = new CommandCallback() {
        @Override public void onCancel() {  }
        @Override public void onError(Object error) {  }
        @Override public void onTaskCompleted(Object o) {  }
    };

    public static final GenerateShrCommandParameters PARAMS_ONE = GenerateShrCommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .homeAccountId("One")
            .popParameters(PopAuthenticationSchemeInternal.builder()
                    .clientClaims("claims")
                    .httpMethod("GET")
                    .url(UrlUtil.makeUrlSilent("https://url"))
                    .nonce("one")
                    .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                    .build())
            .build();
    public static final GenerateShrCommandParameters PARAMS_ONE_CLONE = GenerateShrCommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .homeAccountId("One")
            .popParameters(PopAuthenticationSchemeInternal.builder()
                    .clientClaims("claims")
                    .httpMethod("GET")
                    .url(UrlUtil.makeUrlSilent("https://url"))
                    .nonce("one")
                    .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                    .build())
            .build();
    public static final GenerateShrCommandParameters PARAMS_TWO = GenerateShrCommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .homeAccountId("One")
            .popParameters(PopAuthenticationSchemeInternal.builder()
                    .clientClaims("claims")
                    .httpMethod("GET")
                    .url(UrlUtil.makeUrlSilent("https://url"))
                    .nonce("two")
                    .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                    .build())
            .build();

    @Test
    public void testMappability() throws Exception {
        GenerateShrCommandParameters commandOne = GenerateShrCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .homeAccountId("One")
                .popParameters(PopAuthenticationSchemeInternal.builder()
                        .clientClaims("claims")
                        .httpMethod("GET")
                        .url(UrlUtil.makeUrlSilent("https://url"))
                        .nonce("one")
                        .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                        .build())
                .build();
        GenerateShrCommandParameters commandTwo = GenerateShrCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .homeAccountId("One")
                .popParameters(PopAuthenticationSchemeInternal.builder()
                        .clientClaims("claims")
                        .httpMethod("GET")
                        .url(UrlUtil.makeUrlSilent("https://url"))
                        .nonce("two")
                        .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                        .build())
                .build();

        Map<CommandParameters, Boolean> map = new HashMap<>();
        map.put(commandOne, true);
        Assert.assertEquals(1, map.size());
        map.put(commandTwo, true);
        Assert.assertEquals(2, map.size());
    }
    @Test
    public void testHashCode_equals() throws Exception {
        Assert.assertEquals(PARAMS_ONE.hashCode(), PARAMS_ONE_CLONE.hashCode());
    }

    @Test
    public void testHashCode_notEquals() throws Exception {
        Assert.assertNotEquals(PARAMS_ONE.hashCode(), PARAMS_TWO.hashCode());
    }

    @Test
    public void testEquals_equals() throws Exception {
        Assert.assertEquals(PARAMS_ONE, PARAMS_ONE_CLONE);
    }
    @Test
    public void testEquals_notEqualNull() throws Exception {
        Assert.assertNotEquals(PARAMS_ONE, null);
    }
    @Test
    public void testEquals_equalsSame() throws Exception {
        Assert.assertEquals(PARAMS_ONE, PARAMS_ONE);
    }
    @Test
    public void testEquals_notEqualDifferenceInNonce() {
        Assert.assertNotEquals(PARAMS_ONE, PARAMS_TWO);
    }

}
