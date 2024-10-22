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
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.commands.BaseCommand;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.IControllerFactory;
import com.microsoft.identity.common.java.result.GenerateShrResult;
import com.microsoft.identity.common.java.util.ClockSkewManager;
import com.microsoft.identity.common.java.util.UrlUtil;
import com.microsoft.identity.common.java.util.ported.InMemoryStorage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;

@RunWith(RobolectricTestRunner.class)
public class GenerateShrCommandTest {

    private static final CommandCallback EMPTY_CALLBACK = new CommandCallback() {
        @Override public void onCancel() {  }
        @Override public void onError(Object error) {  }
        @Override public void onTaskCompleted(Object o) {  }
    };

    private static final IControllerFactory emptyControllerFactory = new IControllerFactory() {

        @NonNull
        @Override
        public List<BaseController> getAllControllers() {
            return Collections.<BaseController>emptyList();
        }

        @NonNull
        @Override
        public BaseController getDefaultController() {
            throw new IllegalStateException("Thins shouldn't be invoked in this flow.");
        }
    };

    public static final GenerateShrCommandParameters PARAMS_ONE = GenerateShrCommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .homeAccountId("One")
            .popParameters(PopAuthenticationSchemeInternal.builder()
                    .clientClaims("claims")
                    .httpMethod("GET")
                    .url(UrlUtil.makeUrlSilent("https://url"))
                    .nonce("one")
                    .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                    .build())
            .build();
    public static final GenerateShrCommand COMMAND_ONE = GenerateShrCommand.builder()
            .parameters(PARAMS_ONE)
            .callback(EMPTY_CALLBACK)
            .publicApiId("ID")
            .controllerFactory(emptyControllerFactory)
            .build();
    public static final GenerateShrCommandParameters PARAMS_ONE_CLONE = GenerateShrCommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .homeAccountId("One")
            .popParameters(PopAuthenticationSchemeInternal.builder()
                    .clientClaims("claims")
                    .httpMethod("GET")
                    .url(UrlUtil.makeUrlSilent("https://url"))
                    .nonce("one")
                    .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                    .build())
            .build();
    public static final GenerateShrCommand COMMAND_ONE_CLONE = GenerateShrCommand.builder()
            .parameters(PARAMS_ONE_CLONE)
            .callback(EMPTY_CALLBACK)
            .publicApiId("ID")
            .controllerFactory(emptyControllerFactory)
            .build();
    public static final GenerateShrCommandParameters PARAMS_TWO = GenerateShrCommandParameters.builder()
            .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
            .homeAccountId("One")
            .popParameters(PopAuthenticationSchemeInternal.builder()
                    .clientClaims("claims")
                    .httpMethod("GET")
                    .url(UrlUtil.makeUrlSilent("https://url"))
                    .nonce("two")
                    .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                    .build())
            .build();
    public static final GenerateShrCommand COMMAND_TWO = GenerateShrCommand.builder()
            .parameters(PARAMS_TWO)
            .callback(EMPTY_CALLBACK)
            .publicApiId("ID")
            .controllerFactory(emptyControllerFactory)
            .build();

    @Test
    public void testMappability() throws Exception {
        GenerateShrCommandParameters paramsOne = PARAMS_ONE;
        GenerateShrCommand commandOne = GenerateShrCommand.builder()
                .parameters(paramsOne)
                .callback(EMPTY_CALLBACK)
                .publicApiId("ID")
                .controllerFactory(emptyControllerFactory)
                .build();

        GenerateShrCommandParameters paramsTwo = GenerateShrCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .homeAccountId("One")
                .popParameters(PopAuthenticationSchemeInternal.builder()
                        .clientClaims("claims")
                        .httpMethod("GET")
                        .url(UrlUtil.makeUrlSilent("https://url"))
                        .nonce("two")
                        .clockSkewManager(new ClockSkewManager(new InMemoryStorage<Long>()))
                        .build())
                .build();
        GenerateShrCommand commandTwo = GenerateShrCommand.builder()
                .parameters(paramsTwo)
                .callback(EMPTY_CALLBACK)
                .publicApiId("ID")
                .controllerFactory(emptyControllerFactory)
                .build();
        Map<BaseCommand, Boolean> map = new HashMap<>();
        map.put(commandOne, true);
        Assert.assertEquals(1, map.size());
        map.put(commandTwo, true);
        Assert.assertEquals(2, map.size());
    }

    @Test
    public void testHashCode_equals() throws Exception {
        Assert.assertEquals(COMMAND_ONE.hashCode(), COMMAND_ONE_CLONE.hashCode());
    }

    @Test
    public void testHashCode_notEquals() throws Exception {
        Assert.assertNotEquals(COMMAND_ONE.hashCode(), COMMAND_TWO.hashCode());
    }

    @Test
    public void testEquals_equals() throws Exception {
        Assert.assertEquals(COMMAND_ONE, COMMAND_ONE_CLONE);
    }
    @Test
    public void testEquals_notEqualNull() throws Exception {
        Assert.assertNotEquals(COMMAND_ONE, null);
    }
    @Test
    public void testEquals_equalsSame() throws Exception {
        Assert.assertEquals(COMMAND_ONE, COMMAND_ONE);
    }
    @Test
    public void testEquals_notEqualDifferenceInNonce() {
        Assert.assertNotEquals(COMMAND_ONE, COMMAND_TWO);
    }

    @Test
    public void testNoControllers() {
        final GenerateShrCommand generateShrCommand = new GenerateShrCommand(
                GenerateShrCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                emptyControllerFactory,
                Mockito.mock(CommandCallback.class),
                "ID"
        );

        try {
            generateShrCommand.execute();
        } catch (Throwable t) {
            Assert.assertNotNull(t instanceof Exception);
            Assert.assertEquals("No controllers available", t.getMessage());
        }
    }

    @Test
    public void testSingleControllerSucceeds() throws Exception {
        BaseController controller = Mockito.mock(BaseController.class);
        GenerateShrResult generateShrResult = new GenerateShrResult();
        Mockito.when(controller.generateSignedHttpRequest(any(GenerateShrCommandParameters.class))).thenReturn(generateShrResult);

        final GenerateShrCommand generateShrCommand = new GenerateShrCommand(
                GenerateShrCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                new IControllerFactory() {
                    @NonNull
                    @Override
                    public List<BaseController> getAllControllers() {
                        return Collections.singletonList(controller);
                    }

                    @NonNull
                    @Override
                    public BaseController getDefaultController() {
                        return controller;
                    }
                },
                Mockito.mock(CommandCallback.class),
                "ID"
        );

        GenerateShrResult result = generateShrCommand.execute();
        Assert.assertNotNull(result);
    }

    @Test
    public void testFirstControllerFails_no_account_found_SecondControllerSucceeds() throws Exception {
        BaseController controller1 = Mockito.mock(BaseController.class);
        BaseController controller2 = Mockito.mock(BaseController.class);
        GenerateShrResult generateShrFailResult = new GenerateShrResult();
        generateShrFailResult.setErrorCode("no_account_found");
        GenerateShrResult generateShrSuccessResult = new GenerateShrResult();
        generateShrSuccessResult.setShr("shr");
        Mockito.when(controller1.generateSignedHttpRequest(any(GenerateShrCommandParameters.class))).thenReturn(generateShrFailResult);
        Mockito.when(controller2.generateSignedHttpRequest(any(GenerateShrCommandParameters.class))).thenReturn(generateShrSuccessResult);

        final GenerateShrCommand generateShrCommand = new GenerateShrCommand(
                GenerateShrCommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                new IControllerFactory() {
                    @NonNull
                    @Override
                    public List<BaseController> getAllControllers() {
                        return Arrays.asList(controller1, controller2);
                    }

                    @NonNull
                    @Override
                    public BaseController getDefaultController() {
                        return controller1;
                    }
                },
                Mockito.mock(CommandCallback.class),
                "ID"
        );

        GenerateShrResult result = generateShrCommand.execute();
        Assert.assertNotNull(result);
        Assert.assertEquals("shr", result.getShr());
    }
}
