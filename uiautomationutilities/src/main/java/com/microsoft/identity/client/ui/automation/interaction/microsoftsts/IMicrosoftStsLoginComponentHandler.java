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

import com.microsoft.identity.client.ui.automation.interaction.IOAuth2LoginComponentHandler;
import com.microsoft.identity.client.ui.automation.interaction.UiResponse;

/**
 * A Login Component Handler for Microsoft STS.
 */
public interface IMicrosoftStsLoginComponentHandler extends IOAuth2LoginComponentHandler {

    /**
     * Respond to the speed bump encountered during an authorization request.
     */
    void handleSpeedBump();

    /**
     * Confirm that we have received the enroll page during authorize request.
     */
    void confirmEnrollPageReceived();

    /**
     * Respond to the enroll page by accepting enrollment.
     */
    void acceptEnroll();

    /**
     * Respond to the enroll page declining enrollment.
     */
    void declineEnroll();

    /**
     * Respond to the register page during an authorization request.
     */
    void handleRegistration();

    /**
     * Respond to the Get the app page
     */
    void handleGetTheAppPage();

    /**
     * Clicks yes or no on the "Stay signed in?" screen that gets shown after user signs in.
     *
     * @param staySignedInResponse denotes whether to accept or decline the staySignedIn prompt.
     */
    void handleStaySignedIn(UiResponse staySignedInResponse);

    /**
     * Clicks the call option in the verify your identity page to allow auto mfa account to proceed with
     * interactive request.
     */
    void handleVerifyYourIdentity();

    /**
     * Clickes "Select" when prompted with the choose certificate prompt
     */
    void handleChooseCertificate();

    /**
     * Handle the How would you like to sign in
     */
    void handleHowWouldYouLikeToSignIn();
}
