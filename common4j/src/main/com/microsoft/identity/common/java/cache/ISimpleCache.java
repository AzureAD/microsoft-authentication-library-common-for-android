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
package com.microsoft.identity.common.java.cache;

import java.util.List;

/**
 * A generic caching interface.
 */
public interface ISimpleCache<T> {

    /**
     * Inserts a new T into the cache.
     *
     * @param t The item to insert.
     * @return True, if inserted. False otherwise.
     */
    boolean insert(T t);

    /**
     * Removes an existing T from the cache.
     *
     * @param t The item to remove.
     * @return True if removed or does not exist. False otherwise.
     */
    boolean remove(T t);

    /**
     * Gets all entries in the cache.
     *
     * @return The List of cache entries. May be empty, never null.
     */
    List<T> getAll();

    /**
     * Removes all entries in the cache.
     *
     * @return True if the cache has been successfully cleared. False otherwise.
     */
    boolean clear();
}
