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
package com.microsoft.identity.common.java.commands.parameters;

import static org.mockito.Mockito.mock;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import org.junit.Assert;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TokenCommandParametersTest {

    /**
     * Test that isLookupMode returns true when all required conditions are met:
     * - nativebroker=1 parameter is present
     * - nativebroker_mode=Lookup parameter is present
     */
    @Test
    public void testIsLookupMode_AllConditionsMet_ReturnsTrue() {
        final List<Map.Entry<String, String>> extraTokenBodyParameters = new ArrayList<>();
        extraTokenBodyParameters.add(new AbstractMap.SimpleEntry<>(AuthenticationConstants.Broker.NATIVEBROKER_KEY, AuthenticationConstants.Broker.NATIVEBROKER_VALUE));
        extraTokenBodyParameters.add(new AbstractMap.SimpleEntry<>(AuthenticationConstants.Broker.NATIVEBROKER_MODE_KEY, AuthenticationConstants.Broker.LOOKUP_MODE_VALUE));

        final TokenCommandParameters parameters = TokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class)).
                extraTokenBodyParameters(extraTokenBodyParameters)
                .build();

        final boolean result = parameters.isLookupMode();
        Assert.assertTrue("isLookupMode should return true when all conditions are met", result);
    }

    /**
     * Test that isLookupMode returns false when nativebroker parameter is missing.
     */
    @Test
    public void testIsLookupMode_MissingNativeBrokerParameter_ReturnsFalse() {
        final List<Map.Entry<String, String>> extraTokenBodyParameters = new ArrayList<>();
        extraTokenBodyParameters.add(new AbstractMap.SimpleEntry<>(AuthenticationConstants.Broker.NATIVEBROKER_MODE_KEY, AuthenticationConstants.Broker.LOOKUP_MODE_VALUE));

        final TokenCommandParameters parameters = TokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .extraTokenBodyParameters(extraTokenBodyParameters)
                .build();

        final boolean result = parameters.isLookupMode();
        Assert.assertFalse("isLookupMode should return false when nativebroker parameter is missing", result);
    }

    /**
     * Test that isLookupMode returns false when nativebroker_mode parameter is missing.
     */
    @Test
    public void testIsLookupMode_MissingNativeBrokerModeParameter_ReturnsFalse() {
        final List<Map.Entry<String, String>> extraTokenBodyParameters = new ArrayList<>();
        extraTokenBodyParameters.add(new AbstractMap.SimpleEntry<>(AuthenticationConstants.Broker.NATIVEBROKER_KEY, AuthenticationConstants.Broker.NATIVEBROKER_VALUE));

        final TokenCommandParameters parameters = TokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .extraTokenBodyParameters(extraTokenBodyParameters)
                .build();

        final boolean result = parameters.isLookupMode();
        Assert.assertFalse("isLookupMode should return false when nativebroker_mode parameter is missing", result);
    }

    /**
     * Test that isLookupMode returns false when extraTokenBodyParameters is null.
     */
    @Test
    public void testIsLookupMode_NullParameters_ReturnsFalse() {
        final TokenCommandParameters parameters = TokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .extraTokenBodyParameters(null)
                .build();

        final boolean result = parameters.isLookupMode();
        Assert.assertFalse("isLookupMode should return false when parameters are null", result);
    }

    /**
     * Test that isLookupMode is case-sensitive for parameter keys and values.
     */
    @Test
    public void testIsLookupMode_CaseSensitiveParameters_ReturnsFalse() {
        final List<Map.Entry<String, String>> extraTokenBodyParameters = new ArrayList<>();
        extraTokenBodyParameters.add(new AbstractMap.SimpleEntry<>("NativeBroker", AuthenticationConstants.Broker.NATIVEBROKER_VALUE));
        extraTokenBodyParameters.add(new AbstractMap.SimpleEntry<>("NATIVEBROKER_MODE", "lookup"));

        final TokenCommandParameters parameters = TokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .extraTokenBodyParameters(extraTokenBodyParameters)
                .build();

        final boolean result = parameters.isLookupMode();
        Assert.assertFalse("isLookupMode should be case-sensitive for parameter keys", result);
    }

    /**
     * Test that isLookupMode returns false when parameter values are incorrect.
     */
    @Test
    public void testIsLookupMode_IncorrectParameterValues_ReturnsFalse() {
        final List<Map.Entry<String, String>> extraTokenBodyParameters = new ArrayList<>();
        extraTokenBodyParameters.add(new AbstractMap.SimpleEntry<>(AuthenticationConstants.Broker.NATIVEBROKER_KEY, "0"));
        extraTokenBodyParameters.add(new AbstractMap.SimpleEntry<>(AuthenticationConstants.Broker.NATIVEBROKER_MODE_KEY, AuthenticationConstants.Broker.LOOKUP_MODE_VALUE));

        final TokenCommandParameters parameters = TokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .extraTokenBodyParameters(extraTokenBodyParameters)
                .build();

        final boolean result = parameters.isLookupMode();
        Assert.assertFalse("isLookupMode should return false when parameter values are incorrect", result);
    }

    /**
     * Test that isLookupMode handles empty parameter list correctly.
     */
    @Test
    public void testIsLookupMode_EmptyParameterList_ReturnsFalse() {
        final List<Map.Entry<String, String>> extraTokenBodyParameters = new ArrayList<>();

        final TokenCommandParameters parameters = TokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .extraTokenBodyParameters(extraTokenBodyParameters)
                .build();

        final boolean result = parameters.isLookupMode();
        Assert.assertFalse("isLookupMode should return false with empty parameter list", result);
    }
}
