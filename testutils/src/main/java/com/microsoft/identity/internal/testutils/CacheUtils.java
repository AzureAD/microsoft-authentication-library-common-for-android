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
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;

import java.util.Map;
import java.util.Random;

public class CacheUtils {

    private static final CacheKeyValueDelegate CACHE_KEY_VALUE_DELEGATE = new CacheKeyValueDelegate();

    public static final Function<String, String> TOKEN_SIGNATURE_EDITOR = new Function<String, String>() {
        @Override
        public String apply(String s) {
            return editTokenSignature(s);
        }
    };

    private interface Predicate<T> {
        boolean test(T t);
    }

    private static boolean isAccessToken(@NonNull final String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.AccessToken;
    }

    private static boolean isRefreshToken(@NonNull final String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.RefreshToken;
    }

    /**
     * This method will edit all the token specified by the predicate using the  editor
     * in the shared preference.
     *
     * @param SharedPrefName Name of the shared preference where token has been stored.
     * @param predicate      Generic functional interface representing function that returns true
     *                       or false depending on taken type.
     * @param editor         Functional interface to have any number of token editing method.
     */
    public void editAllTokenInCache(@NonNull final String SharedPrefName, Predicate<String> predicate, Function<String, String> editor) {
        SharedPreferences sharedPref = getSharedPreferences(SharedPrefName);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        Map<String, ?> cacheEntries = sharedPref.getAll();

        //get all the key from the cache entry, verify and edit it.
        for (Map.Entry<String, ?> cacheEntry : cacheEntries.entrySet()) {
            String keyToEdit = cacheEntry.getKey();
            if (predicate.test(keyToEdit)) {
                Object cacheValue = cacheEntries.get(keyToEdit);
                if (cacheValue instanceof String) {
                    Credential credential = CACHE_KEY_VALUE_DELEGATE.fromCacheValue((String) cacheValue, Credential.class);
                    credential.setSecret(editor.apply(credential.getSecret()));
                    prefEditor.putString(keyToEdit, CACHE_KEY_VALUE_DELEGATE.generateCacheValue(credential));
                    prefEditor.apply();
                }
            }
        }
    }

    /**
     * utility function to edit Access Token.
     *
     * @param SharedPrefName Name of the shared preference.
     * @param editor         Functional interface for token editing method.
     */
    public void editAllAccessTokenInCache(@NonNull final String SharedPrefName, Function<String, String> editor) {
        Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean test(String cacheKey) {
                return isAccessToken(cacheKey);
            }
        };

        editAllTokenInCache(SharedPrefName, predicate, editor);
    }

    /**
     * utility function to edit Refresh Token.
     *
     * @param SharedPrefName Name of the shared preference.
     * @param editor         Functional interface for token editing method.
     */
    public void editAllRefreshTokenInCache(@NonNull final String SharedPrefName, Function<String, String> editor) {
        Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean test(String cacheKey) {
                return isRefreshToken(cacheKey);
            }
        };

        editAllTokenInCache(SharedPrefName, predicate, editor);
    }

    private static SharedPreferences getSharedPreferences(final String sharedPrefName) {
        final Context context = ApplicationProvider.getApplicationContext();
        return context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
    }

    /**
     * Split the string into 3 segment separated by period "." and set the
     * random character at the random position in the 3rd segment.
     *
     * @param string string to be edited.
     * @return edited string.
     */
    private static String editTokenSignature(String string) {
        String[] segments = string.split(".");
        String signature = segments[segments.length - 1];

        StringBuilder sb = new StringBuilder(signature);
        Random rnd = new Random();
        int rndIndex = rnd.nextInt(sb.length());
        char charAtRndPosition = sb.charAt(rndIndex);
        char rndChar = (char) ('a' + rnd.nextInt(26));

        while (charAtRndPosition == rndChar) {
            rndIndex = rnd.nextInt(sb.length());
            charAtRndPosition = sb.charAt(rndIndex);
            rndChar = (char) ('a' + rnd.nextInt(26));
        }

        sb.setCharAt(rndIndex, rndChar);
        segments[segments.length - 1] = sb.toString();
        return TextUtils.join(".", segments);
    }
}
