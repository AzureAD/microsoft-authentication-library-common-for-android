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
package com.microsoft.identity.common.internal.authscheme;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/**
 * Abstract base class for AuthenticationSchemes.
 */
public abstract class AbstractAuthenticationScheme implements INameable {

    private static final long serialVersionUID = -2437270903389813253L;

    public static class SerializedNames {
        public static final String NAME = "name";
    }

    /**
     * The name of this scheme.
     */
    @SerializedName(SerializedNames.NAME)
    private final String mName;

    /**
     * Constructs a new AbstractAuthenticationScheme.
     *
     * @param name The name of this scheme.
     */
    public AbstractAuthenticationScheme(@NonNull final String name) {
        mName = name;
    }

    /**
     * Gets the name of this AbstractAuthenticationScheme.
     *
     * @return The name to get.
     */
    public final String getName() {
        return mName;
    }

    @Override
    public String toString() {
        return "AbstractAuthenticationScheme{" +
                "mName='" + mName + '\'' +
                '}';
    }
}
