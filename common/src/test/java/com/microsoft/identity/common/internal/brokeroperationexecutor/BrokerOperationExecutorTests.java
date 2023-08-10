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
package com.microsoft.identity.common.internal.brokeroperationexecutor;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.activebrokerdiscovery.InMemoryActiveBrokerCache;
import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdaterUtil;
import com.microsoft.identity.common.internal.cache.IActiveBrokerCache;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.UserCancelException;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.controllers.BrokerOperationExecutor;
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.CONNECTION_ERROR;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.BOUND_SERVICE;

import lombok.SneakyThrows;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.N})
public class BrokerOperationExecutorTests {

    final String SERVICE_EXCEPTION_BUNDLE_KEY = "service_exception_key";
    final String SUCCESS_BUNDLE_KEY = "success_key";
    final String USER_CANCEL_BUNDLE_KEY = "user_cancel_key";

    final String SERVICE_EXCEPTION_BUNDLE_ERROR_CODE = "service_exception";
    final String CORRUPTED_BUNDLE_ERROR_CODE = "corrupted_bundle";
    final String NULL_BUNDLE_ERROR_CODE = "null_bundle";

    final IIpcStrategy.Type MOCK_TYPE = BOUND_SERVICE;

    // No strategy is provided. executor should fail.
    @Test
    public void testZeroStrategy() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();

        expectBindFailureException(strategyList);
    }

    // Providing 1 strategy and it returns a valid result.
    @Test
    public void testOneStrategyWithValidResult() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithValidResult());

        expectValidResult(strategyList);
    }

    // Providing 1 strategy and it returns a corrupted result.
    @Test
    public void testOneStrategyWithCorruptedResult() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithCorruptedResult());

        expectCorruptedBundleException(strategyList);
    }

    // Providing 1 strategy and it returns a service exception result.
    @Test
    public void testOneStrategyWithServiceExceptionResult() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithServiceExceptionResult());

        expectServiceException(strategyList);
    }

    // Providing 1 strategy and it returns a user cancelled result.
    @Test
    public void testOneStrategyWithUserCancelledResult() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithUserCanceledResult());

        expectUserCancelledException(strategyList);
    }

    // Providing 1 strategy and it fails with BrokerCommunicationException inside IIpcStrategy.
    @Test
    public void testOneStrategyWithBrokerCommunicationException() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithBrokerCommunicationException());

        expectBindFailureException(strategyList);
    }

    // Providing 2 strategies and the first one fails with BrokerCommunicationException inside IIpcStrategy.
    @Test
    public void testTwoStrategiesWithTheFirstOneThrowingBrokerCommunicationException() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithBrokerCommunicationException());
        strategyList.add(getStrategyWithValidResult());

        expectValidResult(strategyList);
    }

    // Providing 2 strategies and the first one returns a corrupted result.
    // NOTE: This should never happen in real life. broker should return the same values regardless of communication strategy.
    @Test
    public void testTwoStrategiesWithTheFirstOneReturningCorruptedResult() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithCorruptedResult());
        strategyList.add(getStrategyWithValidResult());

        expectCorruptedBundleException(strategyList);
    }

    // Providing 2 strategies and the last one failed with BrokerCommunicationException inside IIpcStrategy.
    @Test
    public void testTwoStrategiesWithTheLastOneThrowingBrokerCommunicationException() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithValidResult());
        strategyList.add(getStrategyWithBrokerCommunicationException());

        expectValidResult(strategyList);
    }

    // Providing 2 strategies and the last one returns a corrupted result.
    // NOTE: This should never happen in real life. broker should return the same values regardless of communication strategy.
    @Test
    public void testTwoStrategiesWithTheLastOneThrowingCorruptedResult() {
        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithValidResult());
        strategyList.add(getStrategyWithCorruptedResult());

        expectValidResult(strategyList);
    }

    // For the new broker election mechanism...
    // If the new active broker is returned with the result,
    // The cache must be updated.
    @SneakyThrows
    @Test
    public void testActiveBrokerCacheUpdatedFromResultBundle(){
        final BrokerData newActiveBroker = new BrokerData(
                "com.microsoft.newActiveBroker",
                "SOME_SIG_HASH");

        final ActiveBrokerCacheUpdaterUtil mUtil = new ActiveBrokerCacheUpdaterUtil(
                (brokerData) -> brokerData.equals(newActiveBroker)
        );

        final IIpcStrategy strategy = new IIpcStrategy() {
            @Override
            public @NonNull Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                final Bundle result = new Bundle();
                result.putBoolean(SUCCESS_BUNDLE_KEY, true);
                mUtil.appendActiveBrokerToResultBundle(result, newActiveBroker);
                return result;
            }

            @Override
            public Type getType() {
                return MOCK_TYPE;
            }
        };

        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(strategy);

        final IActiveBrokerCache cache = new InMemoryActiveBrokerCache();
        final BrokerOperationExecutor executor = new BrokerOperationExecutor(strategyList, cache, mUtil);

        Assert.assertTrue(executor.execute(getMockParameter(), getBrokerOperation()));
        Assert.assertEquals(newActiveBroker, cache.getCachedActiveBroker());
    }

    // For the new broker election mechanism...
    // If the new active broker is NOT returned with the result,
    // The cache must remain unchanged.
    @SneakyThrows
    @Test
    public void testActiveBrokerCacheNotUpdatedFromResultBundle(){
        final BrokerData currentActiveBroker = new BrokerData(
                "com.microsoft.currentActiveBroker",
                "SOME_SIG_HASH");

        final List<IIpcStrategy> strategyList = new ArrayList<>();
        strategyList.add(getStrategyWithValidResult());

        final IActiveBrokerCache cache = new InMemoryActiveBrokerCache();
        cache.setCachedActiveBroker(currentActiveBroker);

        expectValidResult(strategyList, cache);
        Assert.assertEquals(currentActiveBroker, cache.getCachedActiveBroker());
    }

    private void expectValidResult(final List<IIpcStrategy> strategyList) {
        expectValidResult(strategyList, new InMemoryActiveBrokerCache());
    }

    private void expectValidResult(final List<IIpcStrategy> strategyList,
                                   final IActiveBrokerCache cache) {
        try {
            final BrokerOperationExecutor executor = new BrokerOperationExecutor(
                    strategyList,
                    cache,
                    new ActiveBrokerCacheUpdaterUtil((brokerData) -> true));
            Assert.assertTrue(executor.execute(getMockParameter(), getBrokerOperation()));
        } catch (final BaseException e) {
            Assert.fail("Unexpected exception.");
        }
    }

    private void expectBindFailureException(final List<IIpcStrategy> strategyList) {
        try {
            final BrokerOperationExecutor executor = new BrokerOperationExecutor(
                    strategyList,
                    new InMemoryActiveBrokerCache(),
                    new ActiveBrokerCacheUpdaterUtil((brokerData) -> true));
            executor.execute(getMockParameter(), getBrokerOperation());
            Assert.fail("Failure is expected.");
        } catch (final BaseException e) {
            Assert.assertTrue(e instanceof ClientException);
            Assert.assertEquals(e.getErrorCode(), ErrorStrings.BROKER_BIND_SERVICE_FAILED);
            Assert.assertEquals(e.getSuppressedException().size(), strategyList.size());
        }
    }

    private void expectCorruptedBundleException(final List<IIpcStrategy> strategyList) {
        try {
            final BrokerOperationExecutor executor = new BrokerOperationExecutor(
                    strategyList,
                    new InMemoryActiveBrokerCache(),
                    new ActiveBrokerCacheUpdaterUtil((brokerData) -> true));
            executor.execute(getMockParameter(), getBrokerOperation());
            Assert.fail("Failure is expected.");
        } catch (final BaseException e) {
            Assert.assertTrue(e instanceof ClientException);
            Assert.assertEquals(e.getErrorCode(), CORRUPTED_BUNDLE_ERROR_CODE);
        }
    }

    private void expectServiceException(final List<IIpcStrategy> strategyList) {
        try {
            final BrokerOperationExecutor executor = new BrokerOperationExecutor(
                    strategyList,
                    new InMemoryActiveBrokerCache(),
                    new ActiveBrokerCacheUpdaterUtil((brokerData) -> true));
            executor.execute(getMockParameter(), getBrokerOperation());
            Assert.fail("Failure is expected.");
        } catch (final BaseException e) {
            Assert.assertTrue(e instanceof ServiceException);
            Assert.assertEquals(e.getErrorCode(), SERVICE_EXCEPTION_BUNDLE_ERROR_CODE);
        }
    }

    private void expectUserCancelledException(final List<IIpcStrategy> strategyList) {
        try {
            final BrokerOperationExecutor executor = new BrokerOperationExecutor(
                    strategyList,
                    new InMemoryActiveBrokerCache(),
                    new ActiveBrokerCacheUpdaterUtil((brokerData) -> true));
            executor.execute(getMockParameter(), getBrokerOperation());
            Assert.fail("Failure is expected.");
        } catch (final BaseException e) {
            Assert.assertTrue(e instanceof UserCancelException);
        }
    }

    private CommandParameters getMockParameter() {
        return CommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .build();
    }

    private IIpcStrategy getStrategyWithValidResult() {
        return new IIpcStrategy() {
            @Override
            public @NonNull Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                final Bundle result = new Bundle();
                result.putBoolean(SUCCESS_BUNDLE_KEY, true);
                return result;
            }

            @Override
            public Type getType() {
                return MOCK_TYPE;
            }
        };
    }

    // Gets a bundle back, the bundle contains a valid result.
    private IIpcStrategy getStrategyWithCorruptedResult() {
        return new IIpcStrategy() {
            @Override
            public @NonNull Bundle communicateToBroker(final @NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                return new Bundle();
            }

            @Override
            public Type getType() {
                return MOCK_TYPE;
            }
        };
    }

    // Gets a bundle back, the bundle contains a service exception result.
    private IIpcStrategy getStrategyWithServiceExceptionResult() {
        return new IIpcStrategy() {
            @Override
            public @NonNull Bundle communicateToBroker(final @NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                final Bundle result = new Bundle();
                result.putBoolean(SERVICE_EXCEPTION_BUNDLE_KEY, true);
                return result;
            }

            @Override
            public Type getType() {
                return MOCK_TYPE;
            }
        };
    }

    // Gets a bundle back, the bundle contains a user cancelled result.
    private IIpcStrategy getStrategyWithUserCanceledResult() {
        return new IIpcStrategy() {
            @Override
            public @NonNull Bundle communicateToBroker(final @NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                final Bundle result = new Bundle();
                result.putBoolean(USER_CANCEL_BUNDLE_KEY, true);
                return result;
            }

            @Override
            public Type getType() {
                return MOCK_TYPE;
            }
        };
    }

    // Fails to get the bundle. (Failure to communicate).
    private IIpcStrategy getStrategyWithBrokerCommunicationException() {
        return new IIpcStrategy() {
            @Override
            public @Nullable Bundle communicateToBroker(final @NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                throw new BrokerCommunicationException(CONNECTION_ERROR, BOUND_SERVICE, "Some connection error", null);
            }

            @Override
            public Type getType() {
                return MOCK_TYPE;
            }
        };
    }

    // This will throw if the result is corrupted..
    private BrokerOperationExecutor.BrokerOperation<Boolean> getBrokerOperation() {
        return new BrokerOperationExecutor.BrokerOperation<Boolean>() {
            @Override
            public void performPrerequisites(@NonNull IIpcStrategy strategy) throws BaseException {
            }

            @Override
            public @NonNull BrokerOperationBundle getBundle() {
                return new BrokerOperationBundle(
                        BrokerOperationBundle.Operation.BROKER_API_HELLO,
                        "MOCK_TARGET_APP",
                        new Bundle());
            }

            @Override
            public @NonNull Boolean extractResultBundle(@Nullable Bundle resultBundle) throws BaseException {
                if (resultBundle == null)
                    throw new ClientException(NULL_BUNDLE_ERROR_CODE);
                else if (resultBundle.containsKey(SUCCESS_BUNDLE_KEY))
                    return resultBundle.getBoolean(SUCCESS_BUNDLE_KEY);
                else if (resultBundle.containsKey(SERVICE_EXCEPTION_BUNDLE_KEY))
                    throw new ServiceException(SERVICE_EXCEPTION_BUNDLE_ERROR_CODE, null, null);
                else if (resultBundle.containsKey(USER_CANCEL_BUNDLE_KEY))
                    throw new UserCancelException();
                else
                    throw new ClientException(CORRUPTED_BUNDLE_ERROR_CODE);

            }

            @Override
            public @NonNull String getMethodName() {
                return "";
            }

            @Override
            public @Nullable String getTelemetryApiId() {
                return null;
            }

            @Override
            public void putValueInSuccessEvent(final @NonNull ApiEndEvent event, final @NonNull Boolean result) {
            }
        };
    }
}
