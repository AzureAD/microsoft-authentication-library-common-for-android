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
package com.microsoft.identity.common.java.nativeauth.commands.parameters;

import org.jetbrains.annotations.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * A set of Reset Password Start command parameters for sending the start request to trigger the self service password reset flow.
 * extends from {@link BaseNativeAuthCommandParameters}
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class ResetPasswordStartCommandParameters extends BaseNativeAuthCommandParameters {
    private static final String TAG = ResetPasswordStartCommandParameters.class.getSimpleName();

    /**
     * The email address of the user.
     */
    @NonNull
    public final String username;

    @NotNull
    @Override
    public String toUnsanitizedString() {
        return "ResetPasswordStartCommandParameters(username=" + username + "authority=" + authority + ", challengeTypes=" + challengeType + ")";
    }

    @Override
    public boolean containsPii() {
        return !toString().equals(toUnsanitizedString());
    }

    @NotNull
    @Override
    public String toString() {
        return "ResetPasswordStartCommandParameters(authority=" + authority + ", challengeTypes=" + challengeType + ")";
    }
}
