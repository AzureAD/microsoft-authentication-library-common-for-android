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
package com.microsoft.identity.common.java.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.Nullable;

public enum CredentialType {
    /**
     * RefreshToken.
     */
    RefreshToken,

    /**
     * AccessToken.
     */
    AccessToken,

    /**
     * IdToken.
     */
    IdToken,

    /**
     * V1IdToken.
     */
    V1IdToken,

    /**
     * Password.
     */
    Password,

    /**
     * Cookie.
     */
    Cookie,

    /**
     * Certificate.
     */
    Certificate,

    /**
     * AccessToken_With_AuthScheme.
     */
    AccessToken_With_AuthScheme,

    /**
     * PrimaryRefreshToken
     */
    PrimaryRefreshToken;

    public static final Collection<CredentialType> ID_TOKEN_TYPES = Collections.unmodifiableList(Arrays.asList(IdToken, V1IdToken));

    /**
     * Get the credential type name set.
     */
    public static Set<String> valueSet() {
        final Set<String> strTypes = new HashSet<>();

        for (final CredentialType type : values()) {
            strTypes.add(type.name());
        }

        return Collections.unmodifiableSet(strTypes);
    }

    /**
     * Returns the enum representation of the supplied String.
     *
     * @param name The sought type (case insensitive) or null.
     * @return The matching CredentialType or null, if supplied name was null.
     */
    @Nullable
    public static CredentialType fromString(@Nullable final String name) {
        for (final CredentialType credentialType : values()) {
            if (credentialType.name().equalsIgnoreCase(name)) {
                return credentialType;
            }
        }

        return null;
    }
}
