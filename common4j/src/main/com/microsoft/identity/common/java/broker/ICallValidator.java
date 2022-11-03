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
package com.microsoft.identity.common.java.broker;

import com.microsoft.identity.common.java.exception.ClientException;

import java.util.Map;

import lombok.NonNull;

/**
 * An interface that throws an exception if the caller is not acceptable given a UID.
 */
public interface ICallValidator {
    /**
     * Throws a ClientException if the caller cannot be validated or is unauthorized.  In android,
     * this will end up taking a map of package name to string of signatures.  In Linux, this is
     * probably a list of client ids.
     * @param methodTag the method name for logging purposes.
     * @param callingUid the identifier for the caller, platform dependent.
     * @param allowedApplications A map of name to iterable of verification string for calling apps.
     * @throws ClientException if the caller cannot be validated.
     */
    void throwIfNotInvokedByAcceptableApp(@NonNull String methodTag,
                                          int callingUid,
                                          @NonNull Map<String, Iterable<String>> allowedApplications)
            throws ClientException;
}
