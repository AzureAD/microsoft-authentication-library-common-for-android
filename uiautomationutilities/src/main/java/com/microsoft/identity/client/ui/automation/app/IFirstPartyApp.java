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
package com.microsoft.identity.client.ui.automation.app;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;

/**
 * An interface describing a first party application and the actions that can be performed on them
 * during a UI Test.
 */
public interface IFirstPartyApp extends IApp {

    /**
     * Add the first user account to this first party app.
     *
     * @param username                the username of the account to add
     * @param password                the password of the account to add
     * @param promptHandlerParameters the prompt handler parameters indicating how to handle prompt
     */
    void addFirstAccount(@NonNull final String username,
                         @NonNull final String password,
                         @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters);

    /**
     * Add another account to this first party app. This must only be called if an account was
     * previously added to this first party app.
     *
     * @param username                the username of the account to add
     * @param password                the password of the account to add
     * @param promptHandlerParameters the prompt handler parameters indicating how to handle prompt
     */
    void addAnotherAccount(@NonNull final String username,
                           @NonNull final String password,
                           @NonNull final FirstPartyAppPromptHandlerParameters promptHandlerParameters);

    /**
     * This method can be called to handle the welcome screens in the first party app that appear on
     * the successful addition of an account to that first party app.
     */
    void onAccountAdded();

    /**
     * Confirms whether the supplied user exists (signed in) in this first party app.
     *
     * @param username the username of the account to confirm
     */
    void confirmAccount(@NonNull final String username);


}
