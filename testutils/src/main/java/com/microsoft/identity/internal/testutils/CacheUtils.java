package com.microsoft.identity.internal.testutils;

import android.content.Context;
import android.content.SharedPreferences;

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

    private boolean isAccessToken(@NonNull String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.AccessToken;
    }

    private boolean isRefreshToken(@NonNull String cacheKey) {
        return SharedPreferencesAccountCredentialCache.getCredentialTypeForCredentialCacheKey(cacheKey) == CredentialType.RefreshToken;
    }

    private void editAllTokenInCache(String SharedPrefName, Predicate<String> predicate, Function<String, String> editor) {
        SharedPreferences sharedPref = getSharedPreferences(SharedPrefName);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        Map<String, ?> cacheEntries = sharedPref.getAll();

        //get all the key from the cache entry, verify and corrupt it.
        for (Map.Entry<String, ?> cacheEntry : cacheEntries.entrySet()) {
            String keyToCorrupt = cacheEntry.getKey();
            if (predicate.test(keyToCorrupt)) {
                if (keyToCorrupt != null) {
                    Object cacheValue = cacheEntries.get(keyToCorrupt);
                    if (cacheValue instanceof String) {
                        Credential credential = CACHE_KEY_VALUE_DELEGATE.fromCacheValue((String) cacheValue, Credential.class);
                        credential.setSecret(editor.apply(credential.getSecret()));
                        prefEditor.putString(keyToCorrupt, CACHE_KEY_VALUE_DELEGATE.generateCacheValue(credential));
                        prefEditor.apply();
                    }

                }
            }
        }
    }

    public void editAllAccessTokenInCache(@NonNull String SharedPrefName, Function<String, String> editor) {
        Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean test(String cacheKey) {
                return isAccessToken(cacheKey);
            }
        };

        editAllTokenInCache(SharedPrefName, predicate, editor);
    }

    public void editAllRefreshTokenInCache(@NonNull String SharedPrefName, Function<String, String> editor) {
        Predicate<String> predicate = new Predicate<String>() {
            @Override
            public boolean test(String cacheKey) {
                return isRefreshToken(cacheKey);
            }
        };

        editAllTokenInCache(SharedPrefName, predicate, editor);
    }

    public static SharedPreferences getSharedPreferences(final String sharedPrefName) {
        final Context context = ApplicationProvider.getApplicationContext();
        return context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
    }

    /*split the string into 3 segments separated by period "." and
        set the random character at the random position in the string.*/
    private static String editTokenSignature(String string) {
        String[] segments = string.split(".");
        String signature = segments[segments.length - 1];

        StringBuilder sb = new StringBuilder(signature);
        Random rnd = new Random();
        char charAtRndPosition = sb.charAt(rnd.nextInt());
        char rndChar = (char) ('a' + rnd.nextInt(26));

        while (charAtRndPosition == rndChar) {
            charAtRndPosition = sb.charAt(rnd.nextInt());
            rndChar = (char) ('a' + rnd.nextInt(26));
        }

        sb.setCharAt(charAtRndPosition, rndChar);
        return sb.toString();
    }
}
