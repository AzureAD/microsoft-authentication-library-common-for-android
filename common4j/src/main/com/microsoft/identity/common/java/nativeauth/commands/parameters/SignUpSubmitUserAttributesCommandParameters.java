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

import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * A set of Sign Up Submit User Attributes command parameters for submitting the collected user attributes set in the user flow.
 * extends from {@link SignUpContinueCommandParameters}
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SignUpSubmitUserAttributesCommandParameters extends SignUpContinueCommandParameters {
    private static final String TAG = SignUpSubmitUserAttributesCommandParameters.class.getSimpleName();

    /**
     * The user attributes of the user set in the user flow need to be collected.
     */
    @NonNull
    public final Map<String, String> userAttributes;

    @NotNull
    @Override
    public String toUnsanitizedString() {
        return "SignUpSubmitUserAttributesCommandParameters(userAttributes=" + userAttributes + ", authority=" + authority + ", challengeTypes=" + challengeType + ")";
    }

    @Override
    public boolean containsPii() {
        return !toString().equals(toUnsanitizedString());
    }

    @NotNull
    @Override
    public String toString() {
        return "SignUpSubmitUserAttributesCommandParameters(authority=" + authority + ", challengeTypes=" + challengeType + ")";
    }
}
