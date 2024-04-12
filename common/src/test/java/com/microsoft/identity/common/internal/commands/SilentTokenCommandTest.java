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
package com.microsoft.identity.common.internal.commands;

import static org.mockito.ArgumentMatchers.any;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.internal.controllers.LocalMSALController;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.SilentTokenCommand;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.CommandDispatcher;
import com.microsoft.identity.common.java.controllers.CommandResult;
import com.microsoft.identity.common.java.controllers.IControllerFactory;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.FinalizableResultFuture;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.SneakyThrows;

@RunWith(RobolectricTestRunner.class)
public class SilentTokenCommandTest {
    private BrokerMsalController brokerController;
    private LocalMSALController msalController;
    private final List<BaseController> controllerList = new ArrayList<>();
    private IControllerFactory controllerFactory;
    @Before
    public void setup() {
        brokerController = Mockito.mock(BrokerMsalController.class);
        msalController = Mockito.mock(LocalMSALController.class);
        controllerList.add(brokerController);
        controllerList.add(msalController);
        controllerFactory = Mockito.mock(IControllerFactory.class);
        Mockito.when(controllerFactory.getAllControllers()).thenReturn(controllerList);
    }

    @After
    public void tearDown() {
        brokerController = null;
        msalController = null;
        controllerList.clear();
        controllerFactory = null;
    }

    @Test
    public void testEmptyControllersList() {
        Mockito.when(controllerFactory.getAllControllers()).thenReturn(new ArrayList<>());

        final SilentTokenCommand silentTokenCommand = new SilentTokenCommand(
                SilentTokenCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                controllerFactory,
                Mockito.mock(CommandCallback.class),
                "22"
        );

        try {
            silentTokenCommand.execute();
        } catch (Throwable t) {
            Assert.assertEquals("No controllers available", t.getMessage());
        }
    }

    @Test
    public void testSingleControllers_Success() throws Exception {
        IControllerFactory singleControllerFactory = Mockito.mock(IControllerFactory.class);
        Mockito.when(singleControllerFactory.getAllControllers()).thenReturn(Arrays.asList(msalController));
        AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
        LocalAuthenticationResult localAuthenticationResult = Mockito.mock(LocalAuthenticationResult.class);
        acquireTokenResult.setLocalAuthenticationResult(localAuthenticationResult);
        Mockito.when(msalController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenReturn( acquireTokenResult);


        final SilentTokenCommand silentTokenCommand = new SilentTokenCommand(
                SilentTokenCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                singleControllerFactory,
                Mockito.mock(CommandCallback.class),
                "22"
        );

        AcquireTokenResult result = silentTokenCommand.execute();
        Assert.assertNotNull(result);
        Assert.assertEquals(true, ((AcquireTokenResult)result).getSucceeded());
    }

    @Test
    public void testSingleControllers_Failure_invalid_grant() throws Exception {
        testSingleController_Failure("invalid_grant", new UiRequiredException("invalid_grant", "msg"));
    }
    @Test
    public void testSingleControllers_Failure_no_tokens_found() throws Exception {
        testSingleController_Failure("no_tokens_found",new ClientException("no_tokens_found"));
    }
    @Test
    public void testSingleControllers_Failure_no_account_found() throws Exception {
        testSingleController_Failure("no_account_found",new ClientException("no_account_found"));
    }
    @Test
    public void testSingleControllers_Failure_other() throws Exception {
        testSingleController_Failure("error_code",new BaseException("error_code"));
    }
    public void testSingleController_Failure(String error_code, Exception ex) throws Exception {
        IControllerFactory singleControllerFactory = Mockito.mock(IControllerFactory.class);
        Mockito.when(singleControllerFactory.getAllControllers()).thenReturn(Arrays.asList(msalController));
        Mockito.when(msalController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenThrow(ex);

        final SilentTokenCommand silentTokenCommand = new SilentTokenCommand(
                SilentTokenCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                singleControllerFactory,
                Mockito.mock(CommandCallback.class),
                "22"
        );

        FinalizableResultFuture<CommandResult> future = CommandDispatcher.submitSilentReturningFuture(silentTokenCommand);
        CommandResult result = future.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(error_code, ((BaseException)result.getResult()).getErrorCode());
    }

    @Test
    public void testMultipleControllers_FirstControllerSuccess_SecondControllerFails_returnsSuccess() throws Exception {
        Mockito.when(msalController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenThrow(new BaseException("error_code"));
        AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
        LocalAuthenticationResult localAuthenticationResult = Mockito.mock(LocalAuthenticationResult.class);
        acquireTokenResult.setLocalAuthenticationResult(localAuthenticationResult);
        Mockito.when(brokerController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenReturn( acquireTokenResult);
        final SilentTokenCommand silentTokenCommand = new SilentTokenCommand(
                SilentTokenCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                controllerFactory,
                Mockito.mock(CommandCallback.class),
                "22"
        );
        AcquireTokenResult result = silentTokenCommand.execute();
        Assert.assertNotNull(result);
        Assert.assertEquals(true, ((AcquireTokenResult)result).getSucceeded());
    }
    @Test
    public void testMultipleControllers_FirstControllerFails_InvalidGrant_SecondControllerSuccess_ReturnsSuccess() throws Exception {
        testMultipleControllers_FirstControllerFailsWithFallbackError_SecondControllerSuccess_ReturnsSuccess(new UiRequiredException("invalid_grant", "error message"));
    }
    @Test
    public void testMultipleControllers_FirstControllerFails_NoTokens_SecondControllerSuccess_ReturnsSuccess() throws Exception {
        testMultipleControllers_FirstControllerFailsWithFallbackError_SecondControllerSuccess_ReturnsSuccess(new ClientException("no_tokens_found"));
    }
    @Test
    public void testMultipleControllers_FirstControllerFails_NoAccount_SecondControllerSuccess_ReturnsSuccess() throws Exception {
        testMultipleControllers_FirstControllerFailsWithFallbackError_SecondControllerSuccess_ReturnsSuccess(new ClientException("no_account_found"));
    }
    public void testMultipleControllers_FirstControllerFailsWithFallbackError_SecondControllerSuccess_ReturnsSuccess(BaseException brokerException) throws Exception {
        AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
        LocalAuthenticationResult localAuthenticationResult = Mockito.mock(LocalAuthenticationResult.class);
        acquireTokenResult.setLocalAuthenticationResult(localAuthenticationResult);
        Mockito.when(msalController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenReturn( acquireTokenResult);
        Mockito.when(brokerController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenThrow(brokerException);


        final SilentTokenCommand silentTokenCommand = new SilentTokenCommand(
                SilentTokenCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                controllerFactory,
                Mockito.mock(CommandCallback.class),
                "22"
        );

        AcquireTokenResult result = silentTokenCommand.execute();
        Assert.assertNotNull(result);
        Assert.assertEquals(true, ((AcquireTokenResult)result).getSucceeded());
    }
    @Test
    public void testMultipleControllers_FirstControllerFailsNonFallbackError_SecondControllerSuccess_returnsFirstControllerError() throws Exception {
        Mockito.when(brokerController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenThrow(new BaseException("error_code"));
        AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
        LocalAuthenticationResult localAuthenticationResult = Mockito.mock(LocalAuthenticationResult.class);
        acquireTokenResult.setLocalAuthenticationResult(localAuthenticationResult);
        Mockito.when(msalController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenReturn( acquireTokenResult);

        final SilentTokenCommand silentTokenCommand = new SilentTokenCommand(
                SilentTokenCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                controllerFactory,
                Mockito.mock(CommandCallback.class),
                "22"
        );
        try {
            silentTokenCommand.execute();
        } catch (Throwable t) {
            Assert.assertNotNull(t instanceof BaseException);
            Assert.assertEquals("error_code", ((BaseException)t).getErrorCode());
        }
    }
    @Test
    public void testMultipleControllers_FirstControllerFails_InvalidGrant_SecondControllerFails_ReturnsFirstControllerError() {
        testMultipleControllers_FirstControllerFails_SecondControllerFail_ReturnsFirstControllerError(OAuth2ErrorCode.INVALID_GRANT, new UiRequiredException(OAuth2ErrorCode.INVALID_GRANT, "broker"),  new ClientException("msal", "msal"));
    }
    @Test
    public void testMultipleControllers_FirstControllerFails_NoTokens_SecondControllerFails_ReturnsFirstControllerError() {
        testMultipleControllers_FirstControllerFails_SecondControllerFail_ReturnsFirstControllerError(ErrorStrings.NO_TOKENS_FOUND, new UiRequiredException(ErrorStrings.NO_TOKENS_FOUND, "broker"),  new BaseException("msal", "msal"));
    }
    @Test
    public void testMultipleControllers_FirstControllerFails_NoAccount_SecondControllerFails_ReturnsFirstControllerError() {
        testMultipleControllers_FirstControllerFails_SecondControllerFail_ReturnsFirstControllerError(ErrorStrings.NO_ACCOUNT_FOUND, new UiRequiredException(ErrorStrings.NO_ACCOUNT_FOUND, "broker"),  new UiRequiredException("msal", "msal"));
    }

    @Test
    public void testMultipleControllers_FirstControllerFailsNonFallbackError_SecondControllerFails_ReturnsFirstControllerError() {
        testMultipleControllers_FirstControllerFails_SecondControllerFail_ReturnsFirstControllerError("broker_error", new BaseException("broker_error", "broker"),  new Exception("msal"));
    }

    @SneakyThrows
    public void testMultipleControllers_FirstControllerFails_SecondControllerFail_ReturnsFirstControllerError(String errorCode, BaseException brokerException, Exception msalException) {
        Mockito.when(brokerController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenThrow( brokerException);
        Mockito.when(msalController.acquireTokenSilent(any(SilentTokenCommandParameters.class))).thenThrow(msalException);

        final SilentTokenCommand silentTokenCommand = new SilentTokenCommand(
                SilentTokenCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                controllerFactory,
                Mockito.mock(CommandCallback.class),
                "22"
        );

        FinalizableResultFuture<CommandResult> future = CommandDispatcher.submitSilentReturningFuture(silentTokenCommand);
        CommandResult result = future.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(errorCode, ((BaseException)result.getResult()).getErrorCode());
        Assert.assertEquals("broker", ((BaseException)result.getResult()).getMessage());
    }

}
