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
package com.microsoft.identity.common.java.net;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Really a marker interface that takes a callable returning a type, and tries to produce
 * an object of that type from it.  Implementations of this interface may examine the object
 * returned and any exceptions that result from the call and decide to execute the supplier
 * again in order to achieve a different result.
 * @param <T> the type of the object on return.
 */
public interface IRetryPolicy<T> {
    /**
     * Evaluate the object returned from a callable and return the result.
     * @param supplier an object to call for a result.
     * @return the result of calling the supplier.
     * @throws IOException if an IO error occurs.
     */
    T attempt (Callable<T> supplier) throws IOException;
}
