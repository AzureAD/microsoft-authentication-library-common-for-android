package com.microsoft.identity.client.ui.automation.utils;

import android.view.accessibility.AccessibilityWindowInfo;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.TIMEOUT;
import static org.junit.Assert.fail;

public class UiAutomatorUtils {

    /**
     * Obtain an instance of the UiObject for a given resource id
     *
     * @param resourceId the resource id of the element to obtain
     * @return the UiObject associated to the supplied resource id
     */
    public static UiObject obtainUiObjectWithResourceId(final String resourceId) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject uiObject = mDevice.findObject(new UiSelector()
                .resourceId(resourceId));

        uiObject.waitForExists(TIMEOUT);
        return uiObject;
    }

    /**
     * Obtain an instance of the UiObject for the given text
     *
     * @param text the text of the element to obtain
     * @return the UiObject associated to the supplied text
     */
    public static UiObject obtainUiObjectWithText(final String text) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject uiObject = mDevice.findObject(new UiSelector()
                .textContains(text));

        uiObject.waitForExists(TIMEOUT);
        return uiObject;
    }

    /**
     * Obtain a child element inside a scrollable view by specifying resource id and text
     *
     * @param scrollableResourceId the resource id of the parent scroll view
     * @param childText            the text on the child view
     * @return the UiObject associated to the desired child element
     */
    public static UiObject obtainChildInScrollable(final String scrollableResourceId, final String childText) {
        final UiSelector scrollSelector = new UiSelector().resourceId(scrollableResourceId);

        final UiScrollable recyclerView = new UiScrollable(scrollSelector);

        final UiSelector childSelector = new UiSelector()
                .textContains(childText);

        try {
            final UiObject child = recyclerView.getChildByText(
                    childSelector,
                    childText
            );

            child.waitForExists(TIMEOUT);
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
    public static void handleInput(final String resourceId, final String inputText) {
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
    public static void handleButtonClick(final String resourceId) {
        final UiObject button = obtainUiObjectWithResourceId(resourceId);

        try {
            button.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
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
