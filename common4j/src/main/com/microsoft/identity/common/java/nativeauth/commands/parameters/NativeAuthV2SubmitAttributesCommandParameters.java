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

import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * Parameters for the V2 submit-attributes step of the sign-up flow.
 * Carries the attributes to submit (keyed by attribute name) and the opaque continuation state
 * from the preceding collect-attributes response. Attribute values are never logged because they
 * may contain PII. Extends {@link BaseSignInTokenCommandParameters}.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class NativeAuthV2SubmitAttributesCommandParameters extends BaseSignInTokenCommandParameters {

    /**
     * The attributes to submit, keyed by attribute name.
     */
    @NonNull
    public final Map<String, String> attributes;

    /**
     * The opaque continuation state from the preceding collect-attributes response.
     */
    @NonNull
    public final NativeAuthV2ContinuationState continuationState;

    @NonNull
    @Override
    public String toUnsanitizedString() {
        return "NativeAuthV2SubmitAttributesCommandParameters(authority=" + authority
                + ", challengeTypes=" + challengeType + ", attributeNames=" + attributes.keySet() + ")";
    }

    @Override
    public boolean containsPii() {
        return !toString().equals(toUnsanitizedString());
    }

    @NonNull
    @Override
    public String toString() {
        return toUnsanitizedString();
    }
}
