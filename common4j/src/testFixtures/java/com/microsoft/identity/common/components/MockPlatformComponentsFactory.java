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
package com.microsoft.identity.common.components;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.ICommand;
import com.microsoft.identity.common.java.crypto.CryptoSuite;
import com.microsoft.identity.common.java.crypto.IDevicePopManager;
import com.microsoft.identity.common.java.crypto.IKeyAccessor;
import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPopManagerSupplier;
import com.microsoft.identity.common.java.interfaces.PlatformComponents;
import com.microsoft.identity.common.java.net.DefaultHttpClientWrapper;
import com.microsoft.identity.common.java.providers.oauth2.IStateGenerator;
import com.microsoft.identity.common.java.strategies.IAuthorizationStrategyFactory;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.java.util.IBroadcaster;
import com.microsoft.identity.common.java.util.IClockSkewManager;
import com.microsoft.identity.common.java.util.IPlatformUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * This class provides an implementation of IPlatformComponents where all attributes are mocked.
 * This is mainly created for testing purposes, included in this package so that it is useful across
 * different packaging.
 */
public class MockPlatformComponentsFactory {

    /**
     * Returns a builder with non-functional implementations of almost every different
     * component.
     */
    @SuppressWarnings(WarningType.rawtype_warning)
    public static PlatformComponents.PlatformComponentsBuilder getNonFunctionalBuilder(){
        final PlatformComponents.PlatformComponentsBuilder builder = PlatformComponents.builder();
        builder.clockSkewManager(NONFUNCTIONAL_CLOCK_SKEW_MANAGER)
                .broadcaster(NONFUNCTIONAL_BROADCASTER)
                .popManagerLoader(NONFUNCTIONAL_POP_MANAGER_LOADER)
                .storageSupplier(new InMemoryStorageSupplier())
                .authorizationStrategyFactory(NON_FUNCTIONAL_AUTH_STRATEGY_FACTORY)
                .stateGenerator(NON_FUNCTIONAL_STATE_GENERATOR)
                .platformUtil(NON_FUNCTIONAL_PLATFORM_UTIL)
                .httpClientWrapper(new DefaultHttpClientWrapper());
        return builder;
    }

    private static final IClockSkewManager NONFUNCTIONAL_CLOCK_SKEW_MANAGER = new IClockSkewManager() {
        @Override
        public void onTimestampReceived(long referenceTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSkewMillis() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date toClientTime(long referenceTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date toReferenceTime(long clientTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getCurrentClientTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date getAdjustedReferenceTime() {
            throw new UnsupportedOperationException();
        }
    };

    public static final IBroadcaster NONFUNCTIONAL_BROADCASTER = (broadcastId, propertyBag) -> {
        // Do nothing.
    };

    public static final IPopManagerSupplier NONFUNCTIONAL_POP_MANAGER_LOADER = new IPopManagerSupplier() {
        @Override
        @NonNull
        public IDevicePopManager getDevicePopManager(@Nullable final String alias) throws ClientException {
            throw new UnsupportedOperationException();
        }
    };

    @SuppressWarnings(WarningType.rawtype_warning)
    public static final IAuthorizationStrategyFactory NON_FUNCTIONAL_AUTH_STRATEGY_FACTORY = parameters -> {
        throw new UnsupportedOperationException();
    };

    public static final IStateGenerator NON_FUNCTIONAL_STATE_GENERATOR = new IStateGenerator() {
        @Override
        @NonNull
        public String generate() {
            throw new UnsupportedOperationException();
        }
    };

    public static final IPlatformUtil NON_FUNCTIONAL_PLATFORM_UTIL = new IPlatformUtil() {
        @Override
        public List<BrowserDescriptor> getBrowserSafeListForBroker() {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public String getInstalledCompanyPortalVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void throwIfNetworkNotAvailable(boolean performPowerOptimizationCheck) throws ClientException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeCookiesFromWebView() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValidCallingApp(@NonNull String redirectUri, @NonNull String packageName) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public String getEnrollmentId(@NonNull String userId, @NonNull String packageName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onReturnCommandResult(@NonNull ICommand<?> command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getNanosecondTime() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void postCommandResult(@NonNull Runnable runnable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public KeyManagerFactory getSslContextKeyManagerFactory() {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public String getPackageNameFromUid(int uid) {
            return null;
        }

        @Nullable
        @Override
        public List<Map.Entry<String, String>> updateWithAndGetPlatformSpecificExtraQueryParameters(@Nullable List<Map.Entry<String, String>> originalList) {
            return originalList;
        }
    };
}
