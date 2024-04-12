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
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.IControllerFactory;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.NonNull;

@RunWith(RobolectricTestRunner.class)
public class LoadAccountCommandTest {
    @Test
    public void testNoControllers() throws Exception {
        final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                CommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
                Mockito.mock(IControllerFactory.class),
                Mockito.mock(CommandCallback.class),
                "22"
        );

        List<ICacheRecord> result = loadAccountCommand.execute();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testSingleControllerSucceeds() throws Exception {
        BaseController controller = Mockito.mock(BaseController.class);
        List<ICacheRecord> cacheRecords = new ArrayList<>();
        cacheRecords.add(Mockito.mock(ICacheRecord.class));
        Mockito.when(controller.getAccounts(any(CommandParameters.class))).thenReturn(cacheRecords);

        final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                CommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
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

        List<ICacheRecord> result = loadAccountCommand.execute();
        Assert.assertEquals(cacheRecords, result);
    }

    @Test
    public void testMultipleControllersSucceed() throws Exception {
        BaseController controller1 = Mockito.mock(BaseController.class);
        BaseController controller2 = Mockito.mock(BaseController.class);
        List<ICacheRecord> cacheRecords1 = new ArrayList<>();
        cacheRecords1.add(Mockito.mock(ICacheRecord.class));
        List<ICacheRecord> cacheRecords2 = new ArrayList<>();
        cacheRecords2.add(Mockito.mock(ICacheRecord.class));
        Mockito.when(controller1.getAccounts(any(CommandParameters.class))).thenReturn(cacheRecords1);
        Mockito.when(controller2.getAccounts(any(CommandParameters.class))).thenReturn(cacheRecords2);

        final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                CommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
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

        List<ICacheRecord> result = loadAccountCommand.execute();
        List<ICacheRecord> expected = new ArrayList<>();
        expected.addAll(cacheRecords1);
        expected.addAll(cacheRecords2);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testFirstControllerFailsSecondControllerSucceeds() throws Exception {
        BaseController controller1 = Mockito.mock(BaseController.class);
        BaseController controller2 = Mockito.mock(BaseController.class);
        List<ICacheRecord> cacheRecords2 = new ArrayList<>();
        cacheRecords2.add(Mockito.mock(ICacheRecord.class));
        Mockito.when(controller1.getAccounts(any(CommandParameters.class))).thenReturn(Collections.emptyList());
        Mockito.when(controller2.getAccounts(any(CommandParameters.class))).thenReturn(cacheRecords2);

        final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                CommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
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

        List<ICacheRecord> result = loadAccountCommand.execute();
        Assert.assertEquals(cacheRecords2, result);
    }

    @Test
    public void testFirstControllerSucceedsSecondControllerFails() throws Exception {
        BaseController controller1 = Mockito.mock(BaseController.class);
        BaseController controller2 = Mockito.mock(BaseController.class);
        List<ICacheRecord> cacheRecords1 = new ArrayList<>();
        cacheRecords1.add(Mockito.mock(ICacheRecord.class));
        Mockito.when(controller1.getAccounts(any(CommandParameters.class))).thenReturn(cacheRecords1);
        Mockito.when(controller2.getAccounts(any(CommandParameters.class))).thenReturn(Collections.emptyList());

        final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                CommandParameters.builder().platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext())).build(),
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

        List<ICacheRecord> result = loadAccountCommand.execute();
        Assert.assertEquals(cacheRecords1, result);
    }
}
