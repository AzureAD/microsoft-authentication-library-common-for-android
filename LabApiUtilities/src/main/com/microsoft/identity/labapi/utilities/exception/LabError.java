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
package com.microsoft.identity.labapi.utilities.exception;

/**
 * Represents errors that can occur in a Lab Api flow. This includes flows to authenticate against
 * the lab api as well as flows to obtain an account via the Lab Api.
 */
public enum LabError {
    FAILED_TO_GET_TOKEN_FOR_KEYVAULT_USING_CLIENT_SECRET,
    FAILED_TO_GET_TOKEN_FOR_KEYVAULT_USING_CERTIFICATE,
    FAILED_TO_GET_SECRET_FROM_KEYVAULT,
    CERTIFICATE_NOT_FOUND_IN_KEY_STORE,
    FAILED_TO_GET_ACCOUNT_FROM_LAB,
    FAILED_TO_GET_SECRET_FROM_LAB,
    FAILED_TO_CREATE_TEMP_USER,
    FAILED_TO_RESET_PASSWORD,
    FAILED_TO_DELETE_DEVICE,
    FAILED_TO_LOAD_CERTIFICATE;
}
