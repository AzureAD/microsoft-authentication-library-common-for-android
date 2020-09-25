// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.commands.CommandCallback;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class CommandDispatcherTest {

    private static final String TEST_RESULT_STR = "test_result_str";

    @Test
    public void testCanSubmitSilently() throws InterruptedException {
        final CountDownLatch testLatch = new CountDownLatch(1);

        final BaseCommand<String> testCommand = new TestCommand(new CommandCallback<String, Exception>() {
            @Override
            public void onCancel() {
                Assert.fail();
                testLatch.countDown();
            }

            @Override
            public void onError(Exception error) {
                Assert.fail();
                testLatch.countDown();
            }

            @Override
            public void onTaskCompleted(String s) {
                Assert.assertEquals(TEST_RESULT_STR, s);
                testLatch.countDown();
            }
        });
        CommandDispatcher.submitSilent(testCommand);
        testLatch.await();
    }

    static class TestCommand extends BaseCommand<String> {

        public TestCommand(@NonNull final CommandCallback callback) {
            super(getTestParameters(), getTestController(), callback, "test_id");
        }

        @Override
        public String execute() {
            return TEST_RESULT_STR;
        }

        @Override
        public boolean isEligibleForEstsTelemetry() {
            return false;
        }
    }

    private static BaseController getTestController() {
        return new BaseController() {
            @Override
            public AcquireTokenResult acquireToken(InteractiveTokenCommandParameters request) throws Exception {
                return null;
            }

            @Override
            public void completeAcquireToken(int requestCode, int resultCode, Intent data) {

            }

            @Override
            public AcquireTokenResult acquireTokenSilent(SilentTokenCommandParameters parameters) throws Exception {
                return null;
            }

            @Override
            public List<ICacheRecord> getAccounts(CommandParameters parameters) throws Exception {
                return null;
            }

            @Override
            public boolean removeAccount(RemoveAccountCommandParameters parameters) throws Exception {
                return false;
            }

            @Override
            public boolean getDeviceMode(CommandParameters parameters) throws Exception {
                return false;
            }

            @Override
            public List<ICacheRecord> getCurrentAccount(CommandParameters parameters) throws Exception {
                return null;
            }

            @Override
            public boolean removeCurrentAccount(RemoveAccountCommandParameters parameters) throws Exception {
                return false;
            }

            @Override
            public AuthorizationResult deviceCodeFlowAuthRequest(DeviceCodeFlowCommandParameters parameters) throws Exception {
                return null;
            }

            @Override
            public AcquireTokenResult acquireDeviceCodeFlowToken(AuthorizationResult authorizationResult, DeviceCodeFlowCommandParameters parameters) throws Exception {
                return null;
            }
        };
    }

    private static CommandParameters getTestParameters() {
        return CommandParameters.builder().build();
    }
}
