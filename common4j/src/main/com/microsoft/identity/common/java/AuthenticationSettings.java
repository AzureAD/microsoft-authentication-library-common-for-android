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
package com.microsoft.identity.common.java;

import com.microsoft.identity.common.java.challengehandlers.IDeviceCertificate;

import edu.umd.cs.findbugs.annotations.NonNull;

public enum AuthenticationSettings {
    INSTANCE;

    private Class<?> mClazzDeviceCertProxy;

    /**
     * Set class for work place join related API.
     * This is only used in the broker process.
     *
     * @param clazz class for workplace join
     */
    public void setDeviceCertificateProxyClass(@NonNull final Class<?> clazz) {
        if (IDeviceCertificate.class.isAssignableFrom(clazz)) {
            mClazzDeviceCertProxy = clazz;
        } else {
            throw new IllegalArgumentException("clazz");
        }
    }

    /**
     * Get class for work place join related API.
     * This is only used in the broker process.
     *
     * @return Class
     */
    public Class<?> getDeviceCertificateProxy() {
        return mClazzDeviceCertProxy;
    }

    /**
     * Remove class for work place join related API.
     * This is only used in the broker process.
     */
    public void removeDeviceCertificateProxy() {
        mClazzDeviceCertProxy = null;
    }
}
