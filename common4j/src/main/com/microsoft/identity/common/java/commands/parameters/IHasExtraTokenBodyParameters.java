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
package com.microsoft.identity.common.java.commands.parameters;

import java.util.List;
import java.util.Map;

/**
 * Interface indicating that a class carries a set of string/string parameters that are intended
 * to be merged into the OAuth2 token request body only.
 */
public interface IHasExtraTokenBodyParameters {

    /**
     * Get the {@link List} of pairs of String, String parameters.
     *
     * @return a list of pairs of String, String parameters - this may be null.  There are no guarantees
     * made with regards to the mutability of this structure, but it <strong>should not</strong> be
     * mutated by the caller, and attempts to do so <strong>may</strong>may throw
     * {@link UnsupportedOperationException} or result in undefined behavior.
     */
    List<Map.Entry<String, String>> getExtraTokenBodyParameters();
}
