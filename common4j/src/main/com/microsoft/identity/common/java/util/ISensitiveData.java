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
package com.microsoft.identity.common.java.util;

/**
 * Interface for objects that hold sensitive data (tokens, secrets, credentials) in memory.
 * <p>
 * Implementations should null out all sensitive {@link String} field references in
 * {@link #clearSensitiveData()} so they become eligible for garbage collection as soon as possible.
 * <p>
 * <strong>JVM limitation:</strong> Java {@code String} objects are immutable — their internal
 * backing arrays cannot be zeroed. Calling {@code clearSensitiveData()} removes the strong
 * reference held by the object, which allows the GC to reclaim the String sooner. This reduces the
 * time window during which an attacker with root/memory-dump access could extract the data, but
 * cannot guarantee immediate erasure from the process heap.
 *
 * @see <a href="https://cwe.mitre.org/data/definitions/316.html">CWE-316: Cleartext Storage of
 * Sensitive Information in Memory</a>
 */
public interface ISensitiveData {

    /**
     * Clears all sensitive data fields held by this object by nulling out references.
     * <p>
     * After this method is called, the object should be considered unusable for any operation
     * that requires the cleared fields. Callers should invoke this as soon as the sensitive data
     * is no longer needed.
     * <p>
     * <strong>Thread-safety and shared ownership:</strong> This method is not synchronized.
     * Callers must ensure that the object is not shared with other consumers who may still need
     * the sensitive data. In particular, when result objects are shared across deduplicated
     * requests (e.g., via {@code CommandDispatcher}), callers should extract the data they need
     * before clearing, and should not clear a result that is still in use by another consumer.
     */
    void clearSensitiveData();
}

