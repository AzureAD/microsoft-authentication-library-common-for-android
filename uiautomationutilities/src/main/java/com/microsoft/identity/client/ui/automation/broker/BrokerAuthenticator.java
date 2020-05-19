package com.microsoft.identity.client.ui.automation.broker;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.interaction.MicrosoftPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import lombok.Getter;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.TIMEOUT;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.getResourceId;

@Getter
public class BrokerAuthenticator extends App implements ITestBroker {

    public final static String AUTHENTICATOR_APP_PACKAGE_NAME = "com.azure.authenticator";
    public final static String AUTHENTICATOR_APP_NAME = "Microsoft Authenticator";

    public BrokerAuthenticator() {
        super(AUTHENTICATOR_APP_PACKAGE_NAME, AUTHENTICATOR_APP_NAME);
    }

    @Override
    public void performDeviceRegistration(String username, String password) {
        performDeviceRegistrationHelper(
                username,
                password,
                "com.azure.authenticator:id/manage_device_registration_email_input",
                "com.azure.authenticator:id/manage_device_registration_register_button"
        );
    }

    @Override
    public void performSharedDeviceRegistration(String username, String password) {
        performDeviceRegistrationHelper(
                username,
                password,
                "com.azure.authenticator:id/shared_device_registration_email_input",
                "com.azure.authenticator:id/shared_device_registration_button"
        );

        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiSelector sharedDeviceConfirmationSelector = new UiSelector()
                .descriptionContains("Shared Device Mode")
                .className("android.widget.ImageView");

        //confirm that we are in Shared Device Mode inside Authenticator
        UiObject sharedDeviceConfirmation = mDevice.findObject(sharedDeviceConfirmationSelector);
        sharedDeviceConfirmation.waitForExists(TIMEOUT);
        Assert.assertTrue(sharedDeviceConfirmation.exists());
    }

    private void performDeviceRegistrationHelper(final String username,
                                                 final String password,
                                                 final String emailInputResourceId,
                                                 final String registerBtnResourceId) {
        launch(); // launch Authenticator app
        handleFirstRun(); // handle first run experience

        // click the 3 dot menu icon in top right
        UiAutomatorUtils.handleButtonClick("com.azure.authenticator:id/menu_overflow");

        try {
            // select Settings from drop down
            final UiObject settings = UiAutomatorUtils.obtainUiObjectWithText("Settings");
            settings.click();

            // scroll down the recycler view to find device registration btn
            final UiObject deviceRegistration = UiAutomatorUtils.obtainChildInScrollable(
                    "com.azure.authenticator:id/recycler_view",
                    "Device registration"
            );

            assert deviceRegistration != null;

            // click the device registration button
            deviceRegistration.click();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        // enter email
        UiAutomatorUtils.handleInput(
                emailInputResourceId,
                username
        );

        // click register
        UiAutomatorUtils.handleButtonClick(registerBtnResourceId);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .broker(this)
                .consentPageExpected(false)
                .expectingNonZeroAccountsInBroker(false)
                .expectingNonZeroAccountsInCookie(false)
                .sessionExpected(false)
                .loginHintProvided(true)
                .build();

        final MicrosoftPromptHandler microsoftPromptHandler = new MicrosoftPromptHandler(promptHandlerParameters);

        // handle AAD login page
        microsoftPromptHandler.handlePrompt(username, password);
    }

    @Override
    public void handleFirstRun() {
        final String skipButtonResourceId = CommonUtils.getResourceId(
                AUTHENTICATOR_APP_PACKAGE_NAME, "frx_slide_skip_button"
        );
        UiAutomatorUtils.handleButtonClick("android:id/button1");
        // the skip button is actually rendered 3 times in the swipe/slide view
        UiAutomatorUtils.handleButtonClick(skipButtonResourceId);
        UiAutomatorUtils.handleButtonClick(skipButtonResourceId);
        UiAutomatorUtils.handleButtonClick(skipButtonResourceId);
    }

    @Override
    public void handleAccountPicker(final String username) {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        // find the object associated to this username in account picker
        UiObject accountSelected = device.findObject(new UiSelector().resourceId(
                getResourceId(AUTHENTICATOR_APP_PACKAGE_NAME, "account_chooser_listView")
        ).childSelector(new UiSelector().textContains(
                username
        )));

        try {
            accountSelected.waitForExists(TIMEOUT);
            accountSelected.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }
}
