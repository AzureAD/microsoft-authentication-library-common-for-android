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
package com.microsoft.identity.common.java.providers.oauth2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class OpenIdConnectPromptParameterTest  {

    @Test
    public void testPromptBehaviorNull(){
       final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior(null);
        assertEquals(promptParameter, OpenIdConnectPromptParameter.UNSET);
    }

    @Test
    public void testPromptBehaviorAuto(){
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior("Auto");
        assertEquals(promptParameter, OpenIdConnectPromptParameter.UNSET);
    }

    @Test
    public void testPromptBehaviorAlways(){
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior("Always");
        assertEquals(promptParameter, OpenIdConnectPromptParameter.UNSET);
    }

    @Test
    public void testPromptBehaviorRefreshSession(){
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior("REFRESH_SESSION");
        assertEquals(promptParameter, OpenIdConnectPromptParameter.UNSET);
    }

    @Test
    public void testPromptBehaviorForcePrompt(){
        final OpenIdConnectPromptParameter promptParameter = OpenIdConnectPromptParameter._fromPromptBehavior("FORCE_PROMPT");
        assertEquals(promptParameter, OpenIdConnectPromptParameter.LOGIN);
    }
}
