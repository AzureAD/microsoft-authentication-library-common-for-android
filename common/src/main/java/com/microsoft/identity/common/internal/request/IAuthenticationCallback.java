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

package com.microsoft.identity.common.internal.request;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.result.IBaseAuthenticationResult;

/**
 * Callback passed with token acquisition. {@link IBaseAuthenticationResult} or {@link Exception} will be returned back via callback.
 */
public interface IAuthenticationCallback {

    /**
     * Authentication finishes successfully.
     *
     * @param authenticationResult {@link IBaseAuthenticationResult} that contains the success response.
     */
    void onSuccess(final IBaseAuthenticationResult authenticationResult);


    /**
     * Error occurs during the authentication.
     *
     * @param exception The {@link BaseException} contains the error code, error message and cause if applicable. The exception
     *                  returned in the callback could be
     *                  {@link com.microsoft.identity.common.exception.ClientException},
     *                  {@link com.microsoft.identity.common.exception.ArgumentException},
     *                  {@link com.microsoft.identity.common.exception.ServiceException} or
     *                  {@link com.microsoft.identity.common.exception.UiRequiredException}.
     */
    void onError(final BaseException exception);

    /**
     * Will be called if user cancels the flow.
     */
    void onCancel();
}