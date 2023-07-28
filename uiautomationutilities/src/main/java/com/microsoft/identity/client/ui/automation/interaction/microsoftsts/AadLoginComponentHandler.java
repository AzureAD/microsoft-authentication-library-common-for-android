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

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.interaction.UiResponse;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

/**
 * A login component handler for AAD.
 */
public class AadLoginComponentHandler implements IMicrosoftStsLoginComponentHandler {

    private final static String TAG = AadLoginComponentHandler.class.getSimpleName();

    public final static String ACCOUNT_PICKER_DID_NOT_APPEAR_ERROR = "Account picker screen did not show up";

    private final long mFindLoginUiElementTimeout;

    public AadLoginComponentHandler() {
        mFindLoginUiElementTimeout = CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
    }

    /**
     * Custom timeout to wait for an UI element on aad login component. All UI interaction would use this value.
     */
    public AadLoginComponentHandler(final long findLoginUiElementTimeout) {
        mFindLoginUiElementTimeout = findLoginUiElementTimeout;
    }

    @Override
    public void handleEmailField(@NonNull final String username) {
        UiAutomatorUtils.handleInput("i0116", username, mFindLoginUiElementTimeout);
        handleNextButton();
    }

    @Override
    public void handlePasswordField(@NonNull final String password) {
        Logger.i(TAG, "Handle Aad Login Password UI..");
        UiAutomatorUtils.handleInput("i0118", password, mFindLoginUiElementTimeout);
        handleNextButton();
    }

    @Override
    public void handleBackButton() {
        UiAutomatorUtils.handleButtonClick("idBtn_Back", mFindLoginUiElementTimeout);
    }

    @Override
    public void handleNextButton() {
        UiAutomatorUtils.handleButtonClick("idSIButton9", mFindLoginUiElementTimeout);
    }

    @Override
    public void handleAccountPicker(@NonNull final String username) {
        Logger.i(TAG, "Handle Account Picker UI..");
        final UiDevice uiDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Confirm On Account Picker
        final UiObject accountPicker = UiAutomatorUtils.obtainUiObjectWithResourceId("tilesHolder", mFindLoginUiElementTimeout);

        if (!accountPicker.waitForExists(FIND_UI_ELEMENT_TIMEOUT)) {
            fail(ACCOUNT_PICKER_DID_NOT_APPEAR_ERROR);
        }

        final UiObject account = uiDevice.findObject(new UiSelector()
                .textContains(username)
        );

        account.waitForExists(FIND_UI_ELEMENT_TIMEOUT);

        try {
            account.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private UiObject getConsentScreen() {
        return UiAutomatorUtils.obtainUiObjectWithResourceId("appDomainLinkToAppInfo", mFindLoginUiElementTimeout);
    }

    @Override
    public void confirmConsentPageReceived() {
        Logger.i(TAG, "Confirm Consent on Consent Page Received..");
        final UiObject consentScreen = getConsentScreen();
        Assert.assertTrue(
                "Consent screen does not appear",
                consentScreen.exists()
        );
    }

    @Override
    public void acceptConsent() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        confirmConsentPageReceived();
        handleNextButton();
    }

    public void declineConsent() {
        confirmConsentPageReceived();
        handleBackButton();
    }

    @Override
    public void handleSpeedBump() {
        Logger.i(TAG, "Handle Speed Bump UI..");
        // Confirm On Speed Bump Screen
        final UiObject speedBump = UiAutomatorUtils.obtainUiObjectWithResourceId("appConfirmTitle", mFindLoginUiElementTimeout);

        if (!speedBump.exists()) {
            fail("Speed Bump screen did not show up");
        }

        handleNextButton();
    }

    @Override
    public void confirmEnrollPageReceived() {
        final UiObject enrollmentHeader = UiAutomatorUtils.obtainUiObjectWithText("Set up your device to get access", mFindLoginUiElementTimeout);
        Assert.assertTrue("Enroll Page appears.", enrollmentHeader.exists());
    }

    @Override
    public void acceptEnroll() {
        confirmEnrollPageReceived();
        handleNextButton();
    }

    @Override
    public void declineEnroll() {
        confirmEnrollPageReceived();
        handleBackButton();
    }

    @Override
    public void handleRegistration() {
        Logger.i(TAG, "Handle Registration Page Received..");
        final UiObject registerBtn = UiAutomatorUtils.obtainUiObjectWithText("Register", mFindLoginUiElementTimeout);
        Assert.assertTrue("Register page appears.", registerBtn.exists());

        handleNextButton();
    }

    @Override
    public void handleStaySignedIn(final UiResponse staySignedInResponse) {
        final UiObject staySignedInView = UiAutomatorUtils.obtainUiObjectWithText("Stay signed in?", mFindLoginUiElementTimeout);

        if (!staySignedInView.exists()) {
            fail("Stay signed in page did not show up");
        }

        if (staySignedInResponse == UiResponse.ACCEPT) {
            handleNextButton();
        } else {
            handleBackButton();
        }
    }

    @Override
    public void handleVerifyYourIdentity() {
        Logger.i(TAG, "Handle Verify Your Identity Page..");

        final UiObject verifyYourIdentity = UiAutomatorUtils.obtainUiObjectWithResourceId("idDiv_SAOTCS_Title", mFindLoginUiElementTimeout);
        if (!verifyYourIdentity.exists()) {
            fail("Verify your identity page did not show up");
        }
        UiAutomatorUtils.handleButtonClickForObjectWithText("Call +X XXXXXXXX21");
    }

    @Override
    public void handleHowWouldYouLikeToSignIn() {
        // Looks like we sometimes see this UI prompt asking "How would like to sign in?"
        // We press button1, which is "Ok" to confirm the default selection of using device certificate.
        UiAutomatorUtils.handleButtonClickSafely("android:id/button1", mFindLoginUiElementTimeout);
    }
}
