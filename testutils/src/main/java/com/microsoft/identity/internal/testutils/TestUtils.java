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
package com.microsoft.identity.internal.testutils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.java.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.util.ResultFuture;

import org.junit.Assert;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

public class TestUtils {

    private static final Gson gson = new Gson();
    private static final ExecutorService testPool = Executors.newFixedThreadPool(10);

    private static String getCacheKeyForAccessToken(Map<String, ?> cacheValues) {
        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isAccessToken(cacheKey)) {
                return cacheKey;
            }
        }

        return null;
    }

    public static boolean isAccessToken(@NonNull final String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.AccessToken;
    }

    public static boolean isRefreshToken(@NonNull final String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.RefreshToken;
    }

    public static IMultiTypeNameValueStorage getSharedPreferences(final String sharedPrefName) {
        final IPlatformComponents components = AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext());

        return components.getStorageSupplier().getFileStore(sharedPrefName);
    }

    /**
     * Return a SharedPreferences instance that works with stores containing encrypted values.
     *
     * @param sharedPrefName the name of the shared preferences file.
     * @return A SharedPreferences that decrypts and encrypts the values.
     */
    public static IMultiTypeNameValueStorage getEncryptedSharedPreferences(final String sharedPrefName) {
        final IPlatformComponents components = AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext());
        final IMultiTypeNameValueStorage barePreferences = components.getStorageSupplier()
                .getFileStore(sharedPrefName);
        return barePreferences;
    }

    public static void clearCache(final String sharedPrefName) {
        IMultiTypeNameValueStorage sharedPreferences = getSharedPreferences(sharedPrefName);
        sharedPreferences.clear();
    }

    public static void removeAccessTokenFromCache(final String sharedPrefName) {
        IMultiTypeNameValueStorage sharedPreferences = getSharedPreferences(sharedPrefName);
        final Map<String, ?> cacheValues = sharedPreferences.getAll();
        final String keyToRemove = getCacheKeyForAccessToken(cacheValues);
        if (keyToRemove != null) {
            sharedPreferences.remove(keyToRemove);
        }
    }

    public static Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }
    /**
     * Given a test function, performs it in a shared thread pool and return an expected exception.
     * This will Assert and throw an exception if the function operates properly (a result is returned).
     * This is a blocking operation.
     *
     * @param request the request object.
     * @param operation the test function.
     * @param timeoutInSeconds how long this method will wait for the exception before
     *                         it throws a {@link RuntimeException}.
     * @return an exception object.
     */
    public static <RequestType, ResultType> Exception testAndExpectException(
            final RequestType request,
            final BiFunction<RequestType, ResultFuture<ResultType>, Void> operation,
            final int timeoutInSeconds) throws Exception {
        try {
            Assert.assertNull("expected exception to be thrown",
                    performOperationInThreadPool(request, operation, timeoutInSeconds));
            throw new RuntimeException("expected exception to be thrown");
        } catch (final TimeoutException e) {
            Assert.fail("Time out.");
            throw e;
        } catch (final Exception e) {
            return e;
        }
    }

    /**
     * Given a test function, performs it in a shared thread pool and return result.
     * This will Assert and throw an exception if it runs into any exception.
     * This is a blocking operation.
     *
     * @param request the request object.
     * @param operation the test function.
     * @param timeoutInSeconds how long this method will wait for the result before
     *                         it throws a {@link RuntimeException}.
     * @return a result object.
     */
    public static <RequestType, ResultType> ResultType testAndExpectResult(
            final RequestType request,
            final BiFunction<RequestType, ResultFuture<ResultType>, Void> operation,
            final int timeoutInSeconds)
            throws Exception {
        try {
            final ResultType result =
                    performOperationInThreadPool(request, operation, timeoutInSeconds);
            Assert.assertNotNull("returned result should not be null", result);
            return result;
        } catch (final Exception e) {
            Assert.fail("expected result to be returned");
            throw e;
        }
    }

    /**
     * Given a test function, performs it in a shared thread pool and return result.
     * This is a blocking operation.
     *
     * @param request the request object.
     * @param operation the test function.
     * @param timeoutInSeconds how long this method will wait for the result before
     *                         it throws a {@link RuntimeException}.
     * @return a result object.
     */
    public static <RequestType,ResultType> ResultType performOperationInThreadPool(
            final RequestType request,
            final BiFunction<RequestType, ResultFuture<ResultType>, Void> operation,
            final int timeoutInSeconds) throws Exception {
        final ResultFuture<ResultType> resultFuture = new ResultFuture<>();
        testPool.submit(() -> {
            operation.apply(request, resultFuture);
        });

        try {
            return resultFuture.get(timeoutInSeconds, TimeUnit.MINUTES);
        } catch (final TimeoutException e) {
            Assert.fail("Time out.");
            throw e;
        } catch (ExecutionException e){
            // Unwrap here, so that we don't have to unwrap it
            // in multiple places down the line.
            throw (Exception) Objects.requireNonNull(e.getCause());
        }
    }
}
