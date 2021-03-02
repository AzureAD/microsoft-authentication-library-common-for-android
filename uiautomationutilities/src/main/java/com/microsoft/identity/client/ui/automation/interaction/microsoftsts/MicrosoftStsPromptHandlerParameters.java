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
package com.microsoft.identity.client.ui.automation.interaction.microsoftsts;

import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * A set of values that can be used to denote the behaviour we expect to observe during an oauth
 * authorization request made against the Microsoft STS.
 * These values can then be supplied to a {@link MicrosoftStsPromptHandler} and
 * used to handle prompt in the manner as expected during the execution of a given test.
 * The test should fail if the actual behaviour observed deviates from what is denoted as expected
 * via these parameters.
 */
@SuperBuilder
@Getter
public class MicrosoftStsPromptHandlerParameters extends PromptHandlerParameters {

    /**
     * Denotes whether the account being used for this request is a federated account.
     */
    private boolean isFederated;

    /**
     * Instructs the system to use a different account when prompted.
     */
    private boolean useSignInWithADifferentAccount;
}
