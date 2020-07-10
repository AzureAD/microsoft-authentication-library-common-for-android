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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;

import org.mockito.Mockito;

import java.util.Map;

public class TestUtils {

    public static final CacheKeyValueDelegate CACHE_KEY_VALUE_DELEGATE = new CacheKeyValueDelegate();
    public static final Predicate<String> IS_ACCESS_TOKEN = new Predicate<String>() {
        @Override
        public boolean test(String s) {
            return isAccessToken(s);
        }
    };
    public static final Predicate<String> IS_REFRESH_TOKEN = new Predicate<String>() {
        @Override
        public boolean test(String s) {
            return isRefreshToken(s);
        }
    };

    private interface Predicate<T> {
        boolean test(T t);
    }

    private static String getCacheKeyForAccessToken(Map<String, ?> cacheValues) {
        return getMachingKeyOrNull(cacheValues, IS_ACCESS_TOKEN);
    }

    private static String getCacheKeyForRefreshToken(Map<String, ?> cacheValues) {
        return getMachingKeyOrNull(cacheValues, IS_REFRESH_TOKEN);
    }

    private static String getMachingKeyOrNull(Map<String, ?> cacheValues, Predicate<String> predicate) {
        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (predicate.test(cacheKey)) {
                return cacheKey;
            }
        }
        return null;
    }

    private static boolean isAccessToken(@NonNull final String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.AccessToken;
    }

    private static boolean isRefreshToken(@NonNull final String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.RefreshToken;
    }

    public static SharedPreferences getSharedPreferences(final String sharedPrefName) {
        final Context context = ApplicationProvider.getApplicationContext();
        return context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
    }

    public static void clearCache(final String sharedPrefName) {
        SharedPreferences sharedPreferences = getSharedPreferences(sharedPrefName);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    public void editTokenInCache(final String sharedPrefName, Predicate<String> keyPredicate,
                                 Function<String, String> editor) {
        SharedPreferences sharedPreferences = getSharedPreferences(sharedPrefName);
        final Map<String, ?> cacheValues = sharedPreferences.getAll();
        final String keyToCorrupt = getMachingKeyOrNull(cacheValues, keyPredicate);
        if (keyToCorrupt != null) {
            Object value = cacheValues.get(keyToCorrupt);
            if (value instanceof String) {
                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                Credential c = CACHE_KEY_VALUE_DELEGATE.fromCacheValue((String) value, Credential.class);
                c.setSecret(editor.apply(c.getSecret()));
                prefEditor.putString(keyToCorrupt, CACHE_KEY_VALUE_DELEGATE.generateCacheValue(c));
                prefEditor.commit();
            }
        }

    }

    public void editAccessTokenInCache(final String sharedPrefName, Function<String, String> editor) {
        editTokenInCache(sharedPrefName, IS_ACCESS_TOKEN, editor);
    }

    public void editRefreshTokenInCache(final String sharedPrefName, Function<String, String> editor) {
        editTokenInCache(sharedPrefName, IS_REFRESH_TOKEN, editor);
    }

    public static void removeAccessTokenFromCache(final String sharedPrefName) {
        SharedPreferences sharedPreferences = getSharedPreferences(sharedPrefName);
        final Map<String, ?> cacheValues = sharedPreferences.getAll();
        final String keyToRemove = getCacheKeyForAccessToken(cacheValues);
        if (keyToRemove != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(keyToRemove);
            editor.commit();
        }
    }

    public static Activity getMockActivity(final Context context) {
        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        return mockedActivity;
    }

    public static Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }

}
