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
package com.microsoft.identity.common.unit;

import android.util.Log;

import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.controllers.BaseCommand;
import com.microsoft.identity.common.internal.request.generated.SilentTokenCommandParameters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashSet;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;


@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class CommandParametersTest {

    @Test
    public void test_TokenCommandParametersBuilder() {

        SilentTokenCommandParameters.Builder builder = SilentTokenCommandParameters.builder();
        String scopes[] = {"User.Read"};

        SilentTokenCommandParameters params = builder.setAuthority(Authority.getAuthorityFromAuthorityUrl("https://login.microsoftonline.com"))
                .setClientId("client_id")
                .setRedirectUri("redirect_uri")
                .setScopes(new HashSet<>(Arrays.asList(scopes)))
                .build();

        Log.i("asdf", "test");

    }

    @Test
    public void test_EqualsVerifySilentTokenCommandParameters() {

        EqualsVerifier.forClass(SilentTokenCommandParameters.class)
                .usingGetClass()
                .suppress(Warning.INHERITED_DIRECTLY_FROM_OBJECT).verify();

    }

    @Test
    public void test_EqualsVerifyInteractiveTokenCommandParameters() {

        EqualsVerifier.forClass(SilentTokenCommandParameters.class)
                .usingGetClass()
                .suppress(Warning.INHERITED_DIRECTLY_FROM_OBJECT).verify();

    }

    @Test
    public void test_EqualsVerifyBaseCommand() {

        EqualsVerifier.forClass(BaseCommand.class)
                .usingGetClass()
                .suppress(Warning.INHERITED_DIRECTLY_FROM_OBJECT).verify();

    }


}
