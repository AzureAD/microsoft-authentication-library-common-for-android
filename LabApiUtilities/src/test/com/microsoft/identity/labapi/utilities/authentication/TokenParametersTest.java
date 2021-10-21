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
package com.microsoft.identity.labapi.utilities.authentication;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * A test to validate that we can create instances of {@link TokenParameters}.
 */
public class TokenParametersTest {

    private static final String CLIENT_ID = "some_client_id";
    private static final String AUTHORITY = "some_authority";
    private static final String SCOPE_1 = "some_scope_1";
    private static final String SCOPE_2 = "some_scope_2";

    @Test(expected = NullPointerException.class)
    public void testCannotCreateTokenParametersWithoutAuthority() {
        TokenParameters.builder().clientId(CLIENT_ID).scope(SCOPE_1).build();

        Assert.fail("We weren't expecting to hit this line...exception should've already occurred");
    }

    @Test
    public void testTokenParametersScopesAreEmptyIfNotProvided() {
        final TokenParameters tokenParameters =
                TokenParameters.builder().clientId(CLIENT_ID).authority(AUTHORITY).build();

        Assert.assertNotNull(tokenParameters);
        Assert.assertTrue(tokenParameters.getScopes().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testCannotCreateTokenParametersWithoutClientId() {
        TokenParameters.builder().authority(AUTHORITY).scope(SCOPE_1).build();

        Assert.fail("We weren't expecting to hit this line...exception should've already occurred");
    }

    @Test
    public void testCanCreateTokenParametersWhenRequiredDataProvided() {
        final TokenParameters tokenParameters =
                TokenParameters.builder()
                        .authority(AUTHORITY)
                        .scope(SCOPE_1)
                        .clientId(CLIENT_ID)
                        .build();

        Assert.assertNotNull(tokenParameters);
        Assert.assertEquals(AUTHORITY, tokenParameters.getAuthority());
        Assert.assertEquals(CLIENT_ID, tokenParameters.getClientId());
        Assert.assertNotNull(tokenParameters.getScopes());
        Assert.assertEquals(1, tokenParameters.getScopes().size());
        Assert.assertTrue(tokenParameters.getScopes().contains(SCOPE_1));
    }

    @Test
    public void testCanCreateTokenParametersWithMultipleScopes() {
        final TokenParameters tokenParameters =
                TokenParameters.builder()
                        .authority(AUTHORITY)
                        .clientId(CLIENT_ID)
                        .scope(SCOPE_1)
                        .scope(SCOPE_2)
                        .build();

        Assert.assertNotNull(tokenParameters);
        Assert.assertEquals(AUTHORITY, tokenParameters.getAuthority());
        Assert.assertEquals(CLIENT_ID, tokenParameters.getClientId());
        Assert.assertNotNull(tokenParameters.getScopes());
        Assert.assertEquals(2, tokenParameters.getScopes().size());
        Assert.assertTrue(tokenParameters.getScopes().contains(SCOPE_1));
        Assert.assertTrue(tokenParameters.getScopes().contains(SCOPE_2));
    }

    @Test
    public void testCanCreateTokenParametersWithScopeSet() {
        final Set<String> scopes =
                new HashSet<String>() {
                    {
                        add(SCOPE_1);
                        add(SCOPE_2);
                    }
                };

        final TokenParameters tokenParameters =
                TokenParameters.builder()
                        .authority(AUTHORITY)
                        .clientId(CLIENT_ID)
                        .scopes(scopes)
                        .build();

        Assert.assertNotNull(tokenParameters);
        Assert.assertEquals(AUTHORITY, tokenParameters.getAuthority());
        Assert.assertEquals(CLIENT_ID, tokenParameters.getClientId());
        Assert.assertNotNull(tokenParameters.getScopes());
        Assert.assertEquals(2, tokenParameters.getScopes().size());
        Assert.assertTrue(tokenParameters.getScopes().contains(SCOPE_1));
        Assert.assertTrue(tokenParameters.getScopes().contains(SCOPE_2));
    }
}
