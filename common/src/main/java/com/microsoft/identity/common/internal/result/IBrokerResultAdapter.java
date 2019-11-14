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
package com.microsoft.identity.common.internal.result;

import android.os.Bundle;

import com.microsoft.identity.common.exception.BaseException;

public interface IBrokerResultAdapter {

    /**
     * Returns a success bundle with properties from Authenticator Result.
     * @param authenticationResult
     * @return Bundle
     */
    Bundle bundleFromAuthenticationResult(ILocalAuthenticationResult authenticationResult);

    /**
     * Returns an error bundle with properties from Exception.
     * @param exception
     * @return
     */
    Bundle bundleFromBaseException(BaseException exception);

    /**
     * Returns authentication result from Broker result bundle
     *
     * @param resultBundle
     * @return {@link ILocalAuthenticationResult}
     */
    ILocalAuthenticationResult authenticationResultFromBundle(Bundle resultBundle);

    /**
     * Returns a BaseException from Broker result bundle.The exception
     * returned in the callback could also be sub class of {@link BaseException}
     * @param resultBundle
     * @return
     */
    BaseException getBaseExceptionFromBundle(Bundle resultBundle);

}
