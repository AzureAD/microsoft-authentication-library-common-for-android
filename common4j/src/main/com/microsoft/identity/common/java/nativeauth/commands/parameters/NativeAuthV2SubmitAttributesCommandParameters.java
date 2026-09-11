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

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
     * Optional sign-up password. The caller transfers ownership of this erasable buffer to the
     * command; the controller clears it on every exit path. It is serialized only at the HTTP
     * boundary and must never be placed in {@link #attributes}.
     */
    @Nullable
    public final char[] password;

    /**
     * The opaque continuation state from the preceding collect-attributes response.
     */
    @NonNull
    public final NativeAuthV2ContinuationState continuationState;

    public abstract static class NativeAuthV2SubmitAttributesCommandParametersBuilder<
            C extends NativeAuthV2SubmitAttributesCommandParameters,
            B extends NativeAuthV2SubmitAttributesCommandParametersBuilder<C, B>>
            extends BaseSignInTokenCommandParametersBuilder<C, B> {

        private char[] password;

        /**
         * Transfers ownership of an erasable password buffer to the command.
         */
        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP2",
                justification = "The command owns this buffer and the controller clears it on"
                        + " every exit path."
        )
        public B password(@Nullable final char[] password) {
            this.password = password;
            return self();
        }
    }

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
