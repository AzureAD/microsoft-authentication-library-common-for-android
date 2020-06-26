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
package com.microsoft.identity.client.ui.automation.utils;

import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static org.junit.Assert.fail;

/**
 * This class contains utility methods for leveraging UI Automator to interact with UI elements
 */
public class UiAutomatorUtils {

    /**
     * Obtain an instance of the UiObject for a given resource id
     *
     * @param resourceId the resource id of the element to obtain
     * @return the UiObject associated to the supplied resource id
     */
    public static UiObject obtainUiObjectWithResourceId(@NonNull final String resourceId) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final UiObject uiObject = mDevice.findObject(new UiSelector()
                .resourceId(resourceId));

        uiObject.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        return uiObject;
    }

    /**
     * Obtain an instance of the UiObject for the given text
     *
     * @param text the text of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithText(@NonNull final String text) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final UiObject uiObject = mDevice.findObject(new UiSelector()
                .textContains(text));

        uiObject.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        return uiObject;
    }

    /**
     * Obtain an instance of the UiObject for the given text and class name
     *
     * @param text      the text of the element to obtain
     * @param className the class name of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithTextAndClassType(@NonNull final String text,
                                                              @NonNull Class className) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final UiObject uiObject = mDevice.findObject(new UiSelector()
                .className(className)
                .textContains(text));

        uiObject.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
        return uiObject;
    }

    /**
     * Obtain a child element inside a scrollable view by specifying resource id and text
     *
     * @param scrollableResourceId the resource id of the parent scroll view
     * @param childText            the text on the child view
     * @return the UiObject associated to the desired child element
     */
    public static UiObject obtainChildInScrollable(@NonNull final String scrollableResourceId,
                                                   @NonNull final String childText) {
        final UiSelector scrollSelector = new UiSelector().resourceId(scrollableResourceId);

        final UiScrollable recyclerView = new UiScrollable(scrollSelector);

        final UiSelector childSelector = new UiSelector()
                .textContains(childText);

        try {
            final UiObject child = recyclerView.getChildByText(
                    childSelector,
                    childText
            );

            child.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            return child;
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        return null;
    }

    /**
     * Fills the supplied text into the input element associated to the supplied resource id
     *
     * @param resourceId the resource id of the input element
     * @param inputText  the text to enter
     */
    public static void handleInput(@NonNull final String resourceId,
                                   @NonNull final String inputText) {
        final UiObject inputField = obtainUiObjectWithResourceId(resourceId);

        try {
            inputField.setText(inputText);
            closeKeyboardIfNeeded();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Clicks the button element associated to the supplied resource id
     *
     * @param resourceId the resource id of the button to click
     */
    public static void handleButtonClick(@NonNull final String resourceId) {
        final UiObject button = obtainUiObjectWithResourceId(resourceId);

        try {
            button.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Presses the device back button on the Android device
     */
    public static void pressBack() {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        mDevice.pressBack();
    }

    private static boolean isKeyboardOpen() {
        for (AccessibilityWindowInfo window : InstrumentationRegistry.getInstrumentation().getUiAutomation().getWindows()) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                return true;
            }
        }
        return false;
    }

    private static void closeKeyboardIfNeeded() {
        if (isKeyboardOpen()) {
            final UiDevice uiDevice =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            uiDevice.pressBack();
        }
    }
}
