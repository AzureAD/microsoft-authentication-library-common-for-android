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
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

/**
 * This class contains utility methods for leveraging UI Automator to interact with UI elements.
 */
public class UiAutomatorUtils {

    private final static String TAG = UiAutomatorUtils.class.getSimpleName();

    /**
     * Obtain an instance of the UiObject for a given resource id.
     *
     * @param resourceId the resource id of the element to obtain
     * @return the UiObject associated to the supplied resource id
     */
    public static UiObject obtainUiObjectWithResourceId(@NonNull final String resourceId) {
        Logger.i(TAG, "Obtain an instance of the UiObject with resource id:" + resourceId);
        return obtainUiObjectWithUiSelector(new UiSelector().resourceId(resourceId),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain an instance of the UiObject for a given resource id and a custom timeout.
     *
     * @param resourceId the resource id of the element to obtain
     * @param existsTimeout time to wait until ui object exists
     * @return the UiObject associated to the supplied resource id
     */
    public static UiObject obtainUiObjectWithResourceId(@NonNull final String resourceId, final long existsTimeout) {
        Logger.i(TAG, "Obtain an instance of the UiObject with resource id (custom timeout):" + resourceId);
        return obtainUiObjectWithUiSelector(new UiSelector().resourceId(resourceId),
                existsTimeout);
    }

    /**
     * Obtain an instance of an enabled UiObject for the resource Id.
     *
     * @param resourceId the resource Id of the element to obtain
     * @param enabled whether the object is enabled or not
     * @return the UiObject associated to the supplied resource id
     */
    @NonNull
    public static UiObject obtainUiObjectWithResourceIdAndEnabledFlag(@NonNull final String resourceId,
                                                                      final boolean enabled) {
        Logger.i(TAG, "Obtain an instance of an enabled UiObject with resource Id:" + resourceId + " and enabled value:" + enabled);
        return obtainUiObjectWithUiSelector(new UiSelector().resourceId(resourceId).enabled(enabled),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain an instance of an enabled UiObject for the given text.
     *
     * @param text the text of the element to obtain
     * @return the UiObject associated to the supplied resource id
     */
    @NonNull
    public static UiObject obtainEnabledUiObjectWithExactText(@NonNull final String text) {
        Logger.i(TAG, "Obtain an instance of an enabled UiObject with text:" + text);
        return obtainUiObjectWithUiSelector(new UiSelector().text(text).enabled(true),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain an instance of the UiObject for the given text.
     *
     * @param text the text of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithText(@NonNull final String text) {
        Logger.i(TAG, "Obtain an instance of the UiObject with text:" + text);
        return obtainUiObjectWithUiSelector(new UiSelector().textContains(text),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain an instance of the UiObject for the given text.
     *
     * @param description the description of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithDescription(@NonNull final String description) {
        Logger.i(TAG, "Obtain an instance of the UiObject with description:" + description);
        return obtainUiObjectWithUiSelector(new UiSelector().description(description),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain an instance of the UiObject for the given text.
     *
     * @param description the content description of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithClassAndDescription(@NonNull final Class clazz,
                                                                 @NonNull final String description) {
        Logger.i(TAG, "Obtain an instance of the UiObject with description:" + description + " and class name:" + clazz);
        return obtainUiObjectWithUiSelector(new UiSelector().className(clazz).descriptionContains(description),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain an instance of the UiObject for a given resource id.
     *
     * @param resourceId the resource id of the element to obtain
     * @return the UiObject associated to the supplied resource id
     */
    public static UiObject obtainUiObjectWithResourceIdAndText(@NonNull final String resourceId,
                                                               @NonNull final String text) {
        Logger.i(TAG, "Obtain an instance of an UiObject with resource id:" + resourceId + " and with text:" + text);
        return obtainUiObjectWithUiSelector(new UiSelector().resourceId(resourceId).textContains(text),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain an instance of the UiObject for the given text and class name.
     *
     * @param text      the text of the element to obtain
     * @param className the class name of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithTextAndClassType(@NonNull final String text,
                                                              @NonNull Class className) {
        Logger.i(TAG, "Obtain an instance of the UiObject with text:" + text + " and with class name:" + className);
        return obtainUiObjectWithUiSelector(new UiSelector().className(className).textContains(text),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain a child element inside a scrollable view by specifying resource id and text.
     *
     * @param scrollableResourceId the resource id of the parent scroll view
     * @param childText            the text on the child view
     * @return the UiObject associated to the desired child element
     */
    public static UiObject obtainChildInScrollable(@NonNull final String scrollableResourceId,
                                                   @NonNull final String childText) {
        Logger.i(TAG, "Obtain a child element inside a scrollable view with resource id:" + scrollableResourceId + " and with text:" + childText);
        final UiSelector scrollSelector = new UiSelector().resourceId(scrollableResourceId);
        return obtainChildInScrollable(childText, scrollSelector);
    }

    /**
     * Obtain a child element inside a scrollable view by specifying class and text.
     *
     * @param clazz     the class of the parent scroll view
     * @param childText the text on the child view
     * @return the UiObject associated to the desired child element
     */
    public static UiObject obtainChildInScrollable(@NonNull final Class clazz,
                                                   @NonNull final String childText) {
        Logger.i(TAG, "Obtain a child element inside a scrollable view with class name:" + clazz + " and with text:" + childText);
        final UiSelector scrollSelector = new UiSelector().className(clazz);
        return obtainChildInScrollable(childText, scrollSelector);
    }

    private static UiObject obtainChildInScrollable(@NonNull final String childText,
                                                    @NonNull final UiSelector scrollSelector) {
        Logger.i(TAG, "Obtain a child element inside a scrollable view with text:" + childText + " and with scrollSelector value:" + scrollSelector);
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
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Obtain a child element inside a scrollable view by specifying text.
     *
     * @param childText the text on the child view
     * @return the UiObject associated to the desired child element
     */
    public static UiObject obtainChildInScrollable(@NonNull final String childText) {
        Logger.i(TAG, "Obtain a child element inside a scrollable view with text:" + childText);
        final UiSelector scrollSelector = new UiSelector().className(ScrollView.class);

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
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Fills the supplied text into the input element associated to the supplied resource id.
     *
     * @param resourceId the resource id of the input element
     * @param inputText  the text to enter
     */
    public static void handleInput(@NonNull final String resourceId,
                                   @NonNull final String inputText) {
        Logger.i(TAG, "Handling input for resource id: " + resourceId);
        final UiObject inputField = obtainUiObjectWithResourceId(resourceId);

        try {
            inputField.setText(inputText);
            closeKeyboardIfNeeded();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Clicks the button element associated to the supplied resource id.
     *
     * @param resourceId the resource id of the button to click
     */
    public static void handleButtonClick(@NonNull final String resourceId) {
        Logger.i(TAG, "Clicks the button element associated to the resource id:" + resourceId);
        final UiObject button = obtainUiObjectWithResourceId(resourceId);

        try {
            button.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Clicks the button element associated to the supplied resource id.
     * Do not throw an exception if the button is not found.
     *
     * @param resourceId the resource id of the button to click
     */
    public static void handleButtonClickSafely(@NonNull final String resourceId) {
        Logger.i(TAG, "Clicks the button element associated to the resource id:" + resourceId);
        final UiObject button = obtainUiObjectWithResourceId(resourceId);

        try {
            button.click();
        } catch (final UiObjectNotFoundException e) {
            Logger.i(TAG, "Button " + resourceId + " was not found: " + e.getMessage());
        }
    }

    /**
     * Clicks the button element associated to the supplied resource id with a longer timeout.
     * Add parameter to give a custom timeout.
     * @param resourceId the resource id of the button to click
     * @param existsTimeout how long to wait for ui element
     */
    public static void handleButtonClick(@NonNull final String resourceId, final long existsTimeout) {
        Logger.i(TAG, "Clicks the button element associated to the resource id (custom timeout):" + resourceId);
        final UiObject button = obtainUiObjectWithResourceId(resourceId, existsTimeout);

        try {
            button.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Clicks the button element associated to the supplied text.
     *
     * @param text the text on the button to click
     */
    public static void handleButtonClickForObjectWithText(@NonNull final String text) {
        final UiObject button = obtainUiObjectWithText(text);

        try {
            button.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Clicks the button element associated to the supplied text.
     * Do not throw an exception if the button is not found.
     *
     * @param text the text on the button to click
     */
    public static void handleButtonClickForObjectWithTextSafely(@NonNull final String text) {
        final UiObject button = obtainUiObjectWithText(text);

        try {
            button.click();
        } catch (final UiObjectNotFoundException e) {
            Logger.i(TAG, "Button with text \"" + text + "\" was not found: " + e.getMessage());
        }
    }

    /**
     * Presses the device back button on the Android device.
     */
    public static void pressBack() {
        Logger.i(TAG, "Presses the device back button on the Android device..");
        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        device.pressBack();
    }

    /**
     * Presses the device enter button on the Android device.
     */
    public static void pressEnter() {
        Logger.i(TAG, "Presses the device enter button on the Android device..");
        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        device.pressEnter();
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

    /**
     * Obtain an instance of the UiObject for the given text.
     *
     * @param text the text of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithExactText(@NonNull final String text) {
        Logger.i(TAG, "Obtain an instance of the UiObject for the given text:" + text);
        return obtainUiObjectWithUiSelector(new UiSelector().text(text),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    /**
     * Obtain an instance of the UiObject for the given class and index.
     *
     * @param clazz the class of the element to obtain
     * @param index the index of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithClassAndIndex(@NonNull final Class clazz, final int index) {
        Logger.i(TAG, "Obtain an instance of the UiObject for the class name:" + clazz + " and index value:" + index);
        return obtainUiObjectWithUiSelector(new UiSelector().className(clazz).index(index),
                FIND_UI_ELEMENT_TIMEOUT);
    }

    public static UiObject obtainUiObjectWithUiSelector(@NonNull final UiSelector selector, final long existsTimeout) {
        Logger.i(TAG, "Obtain an instance of the UiObject with a given selector");
        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final UiObject uiObject = device.findObject(selector);

        uiObject.waitForExists(existsTimeout);
        return uiObject;
    }
}
