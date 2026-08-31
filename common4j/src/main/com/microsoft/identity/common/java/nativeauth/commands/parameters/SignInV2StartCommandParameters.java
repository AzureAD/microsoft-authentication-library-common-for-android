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

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * Parameters for the V2 sign-in start operation.
 * Carries the username that initiates the flow and, optionally, the password to submit as soon as
 * the server offers the password first factor. The password is never retained in continuation or
 * public state; the controller clears the buffer once the request has been issued.
 * Extends {@link BaseSignInTokenCommandParameters}.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuppressFBWarnings("EI_EXPOSE_REP2")   //Suppresses spotbugs warning on the builder class
@SuperBuilder(toBuilder = true)
public class SignInV2StartCommandParameters extends BaseSignInTokenCommandParameters {
    /**
     * The username of the account signing in.
     */
    @NonNull
    public final String username;

    /**
     * The password to submit for the password first factor, or {@code null} when the app defers
     * the password to the password-required state.
     */
    @Nullable
    public final char[] password;

    @NonNull
    @Override
    public String toUnsanitizedString() {
        return "SignInV2StartCommandParameters(username=" + username + ", authority=" + authority
                + ", challengeTypes=" + challengeType + ", hasPassword=" + (password != null) + ")";
    }

    @Override
    public boolean containsPii() {
        return !toString().equals(toUnsanitizedString());
    }

    @NonNull
    @Override
    public String toString() {
        return "SignInV2StartCommandParameters(authority=" + authority + ", challengeTypes="
                + challengeType + ", hasPassword=" + (password != null) + ")";
    }
}
