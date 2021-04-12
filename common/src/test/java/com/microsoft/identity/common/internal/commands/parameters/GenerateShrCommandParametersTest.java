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

import com.microsoft.identity.common.internal.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.internal.cache.MapBackedPreferencesManager;
import com.microsoft.identity.common.internal.commands.Command;
import com.microsoft.identity.common.internal.commands.GenerateShrCommand;
import com.microsoft.identity.common.internal.util.ClockSkewManager;
import com.microsoft.identity.common.internal.util.IClockSkewManager;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GenerateShrCommandParametersTest {
    @Test
    public void testMappability() throws Exception {
        GenerateShrCommandParameters commandOne = GenerateShrCommandParameters.builder()
                .homeAccountId("One")
                .popParameters(PopAuthenticationSchemeInternal.builder()
                        .clientClaims("claims")
                        .httpMethod("GET")
                        .url(new URL("https://url"))
                        .nonce("one")
                        .clockSkewManager(new ClockSkewManager(new MapBackedPreferencesManager("name")))
                        .build())
                .build();
        GenerateShrCommandParameters commandTwo = GenerateShrCommandParameters.builder()
                .homeAccountId("One")
                .popParameters(PopAuthenticationSchemeInternal.builder()
                        .clientClaims("claims")
                        .httpMethod("GET")
                        .url(new URL("https://url"))
                        .nonce("two")
                        .clockSkewManager(new ClockSkewManager(new MapBackedPreferencesManager("name")))
                        .build())
                .build();

        Map<CommandParameters, Boolean> map = new HashMap<>();
        map.put(commandOne, true);
        Assert.assertEquals(1, map.size());
        map.put(commandTwo, true);
        Assert.assertEquals(2, map.size());
    }

}
