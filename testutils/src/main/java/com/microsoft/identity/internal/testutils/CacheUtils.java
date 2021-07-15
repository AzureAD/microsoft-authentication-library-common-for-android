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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;

import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.java.dto.AccessTokenRecord;
import com.microsoft.identity.common.java.dto.Credential;
import com.microsoft.identity.common.java.dto.CredentialType;
import com.microsoft.identity.common.java.dto.IdTokenRecord;
import com.microsoft.identity.common.java.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.Map;
import java.util.Random;


/**
 * This class provide utility to modify the tokens stored in the cache by providing predicate and Function
 * generic interface. This provide caller flexibility to write its own editing functions to modify the tokens
 * stored in the cache.
 */
public class CacheUtils {

    private static final CacheKeyValueDelegate CACHE_KEY_VALUE_DELEGATE = new CacheKeyValueDelegate();

    /**
     * Functional interface for editing signature of the token.
     */
    public static final Function<String, String> TOKEN_SIGNATURE_EDITOR = new Function<String, String>() {
        @Override
        public String apply(String s) {
            return randomizeCharacterInTokenSignature(s);
        }
    };

    /**
     * Generic functional interface to give caller flexibility of code to evaluate on the given
     * arguments.
     *
     * @param <T> input arguments to evaluate.
     */
    public interface Predicate<T> {
        boolean test(T t);
    }

    /**
     * This method will edit all the token specified by the predicate using the  editor
     * in the shared preference.
     *
     * @param sharedPrefName Name of the shared preference where token has been stored.
     * @param predicate      Generic functional interface representing function that returns true
     *                       or false depending on token type.
     * @param editor         Functional interface to have any number of token editing method.
     */
    public void editAllTokenInCache(@NonNull final String sharedPrefName, @NonNull final Predicate<String> predicate,
                                    @NonNull Function<String, Class<? extends Credential>> classFunction, @NonNull final Function<String, String> editor,
                                             final boolean encrypted) {
        final SharedPreferencesFileManager sharedPref = encrypted ? TestUtils.getEncryptedSharedPreferences(sharedPrefName) :
                TestUtils.getSharedPreferences(sharedPrefName);
        final Map<String, ?> cacheEntries = sharedPref.getAll();

        //get all the key from the cache entry, verify and edit it.
        for (final Map.Entry<String, ?> cacheEntry : cacheEntries.entrySet()) {
            final String keyToEdit = cacheEntry.getKey();
            if (predicate.test(keyToEdit)) {
                final String cacheValue = (String) cacheEntries.get(keyToEdit);
                final Class<? extends Credential> credClass = classFunction.apply(keyToEdit);
                if (credClass == null) { continue; }
                final Credential credential = CACHE_KEY_VALUE_DELEGATE.fromCacheValue(cacheValue, credClass);
                if (credential == null) { Logger.warn("CacheUtils:editAllTokenInCache", "Value did not deserialize"); continue; }
                credential.setSecret(editor.apply(credential.getSecret()));
                sharedPref.putString(keyToEdit, CACHE_KEY_VALUE_DELEGATE.generateCacheValue(credential));
            }
        }
    }


    public static Function<String, Class<? extends Credential>> CLASS_DETERMNING_FUNCTION =
            new Function<String, Class<? extends Credential>>() {
                @Override
                public Class<? extends Credential> apply(String k) {
                    final CredentialType credentialTypeForCredentialCacheKey = SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(k);
                    if (credentialTypeForCredentialCacheKey == null) {
                        return null;
                    }
                    switch(credentialTypeForCredentialCacheKey) {
                        case AccessToken:
                            return AccessTokenRecord.class;
                        case IdToken:
                            return IdTokenRecord.class;
                        case V1IdToken:
                            return null;
                        case AccessToken_With_AuthScheme:
                            return null;
                        case RefreshToken:
                            return RefreshTokenRecord.class;
                        case Certificate:
                            return null;
                        case Cookie:
                            return null;
                        case Password:
                            return null;
                        default:
                            return null;
                    }
                }
            };

    /**
     * utility function to edit Access Token.
     *
     * @param sharedPrefName Name of the shared preference.
     * @param editor         Functional interface for token editing method.
     */
    public void editAllAccessTokenInCache(@NonNull final String sharedPrefName, @NonNull final Function<String, String> editor, boolean encrypted) {
        final Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean test(String cacheKey) {
                return TestUtils.isAccessToken(cacheKey);
            }
        };

        editAllTokenInCache(sharedPrefName, predicate, new Function<String, Class<? extends Credential>>(){ public Class<? extends Credential> apply(String k) { return AccessTokenRecord.class; } }, editor, encrypted);
    }

    /**
     * utility function to edit Refresh Token.
     *
     * @param sharedPrefName Name of the shared preference.
     * @param editor         Functional interface for token editing method.
     */
    public void editAllRefreshTokenInCache(@NonNull final String sharedPrefName, @NonNull final Function<String, String> editor, boolean encrypted) {
        final Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean test(String cacheKey) {
                return TestUtils.isRefreshToken(cacheKey);
            }
        };

        editAllTokenInCache(sharedPrefName, predicate, new Function<String, Class<? extends Credential>>(){ public Class<? extends Credential> apply(String k) { return RefreshTokenRecord.class; } }, editor, encrypted);
    }

    /**
     * Split the string into 3 segment separated by period "." and set the
     * random character at the random position in the 3rd segment.
     *
     * @param string string to be edited.
     * @return edited string.
     */
    private static String randomizeCharacterInTokenSignature(String string) {
        final String[] segments = string.split("\\.");
        if (segments.length != 3) {
            throw new AssertionError("not JWT");
        }

        // segment[2] = signature of token which is the 3rd part of the string.
        final String signature = segments[2];
        final StringBuilder signatureBuilder = new StringBuilder(signature);
        final Random random = new Random();
        final int index = random.nextInt(signatureBuilder.length());

        // get random character from the base64 string.
        final String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        int position = random.nextInt(base64Chars.length());
        if (signatureBuilder.charAt(index) == base64Chars.charAt(position)) {
            position += random.nextInt(base64Chars.length() - position) + 1;
        }

        signatureBuilder.setCharAt(signatureBuilder.charAt(index), base64Chars.charAt(position));
        segments[2] = signatureBuilder.toString();
        return TextUtils.join(".", segments);
    }

    public void clear(String sharedPrefName, boolean encrypted) {
        final SharedPreferencesFileManager sharedPref = encrypted ? TestUtils.getEncryptedSharedPreferences(sharedPrefName) :
                TestUtils.getSharedPreferences(sharedPrefName);
        sharedPref.clear();
    }
}
