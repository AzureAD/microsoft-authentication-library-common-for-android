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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.logging.Logger;

import org.mockito.Mockito;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestUtils {

    private static final Gson gson = new Gson();

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

    public static SharedPreferences getSharedPreferences(final String sharedPrefName) {
        final Context context = ApplicationProvider.getApplicationContext();
        return context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
    }

    /**
     * Return a SharedPreferences instance that works with stores containing encrypted values.
     *
     * @param sharedPrefName the name of the shared preferences file.
     * @return A SharedPreferences that decrypts and encrypts the values.
     */
    public static SharedPreferences getEncryptedSharedPreferences(final String sharedPrefName) {
        final Context context = ApplicationProvider.getApplicationContext();
        final SharedPreferences barePreferences = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE);
        final StorageHelper storageHelper = new StorageHelper(context);
        if (storageHelper == null) {
            return barePreferences;
        }
        return new SharedPreferences() {

            private String decrypt(String s)  {
                try {
                    return s == null ? s : storageHelper.decrypt(s);
                } catch (GeneralSecurityException | IOException e) {
                    Logger.error("TestUtils:decrypt", "Error decryping value", e);
                    return null;
                }
            }

            private String encrypt(@Nullable String value)  {
                try {
                    return value == null ? value : storageHelper.encrypt(value);
                } catch (GeneralSecurityException | IOException e) {
                    Logger.error("TestUtils:encrypt", "Error encryping value", e);
                    return null;
                }
            }

            public Map<String, ?> getAll() {
                //TODO: fix this to work with more than just String values.
                final Map<String, ?> map = barePreferences.getAll();
                return new AbstractMap<String, Object>() {

                    @Override
                    public Set<Entry<String, Object>> entrySet() {
                        Set<Entry<String, Object>> newSet = new HashSet<>();
                        for(final Map.Entry<String, ?> e : map.entrySet()) {
                            newSet.add(new AbstractMap.SimpleEntry<String, Object>(e.getKey(), getString(e.getKey(), null)));
                        }
                        return newSet;
                    }
                };
            }

            public String getString(String key, @Nullable String defValue) {
                String s = barePreferences.getString(key, defValue);
                if (s == defValue) {
                    return s;
                }
                return decrypt(s);
            }

            @Override
            public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
                return barePreferences.getStringSet(key, defValues);
            }

            @Override
            public int getInt(String key, int defValue) {
                String s = barePreferences.getString(key, null);
                if (TextUtils.isEmpty(s)) {
                    return 0;
                }
                return Integer.valueOf(decrypt(s));
            }

            @Override
            public long getLong(String key, long defValue) {
                String s = barePreferences.getString(key, null);
                if (TextUtils.isEmpty(s)) {
                    return 0;
                }
                return Long.valueOf(decrypt(s));
            }

            @Override
            public float getFloat(String key, float defValue) {
                String s = barePreferences.getString(key, null);
                if (TextUtils.isEmpty(s)) {
                    return 0;
                }
                return Float.valueOf(decrypt(s));
            }

            @Override
            public boolean getBoolean(String key, boolean defValue) {
                String s = barePreferences.getString(key, null);
                if (TextUtils.isEmpty(s)) {
                    return false;
                }
                return Boolean.valueOf(decrypt(s));
            }

            @Override
            public boolean contains(String key) {
                return barePreferences.contains(key);
            }

            @Override
            public Editor edit() {
                final Editor bareEditor = barePreferences.edit();

                return new Editor() {

                    @Override
                    public Editor putString(String key, @Nullable String value) {
                        if (!TextUtils.isEmpty(value)) {
                            value = encrypt(value);
                        }
                        return bareEditor.putString(key, value);
                    }

                    @Override
                    public Editor putStringSet(String key, @Nullable Set<String> values) {
                        String value;
                        if (values == null || values.isEmpty()) {
                            value = "";
                        } else {
                            value = gson.toJson(values);
                        }
                        if (!TextUtils.isEmpty(value)) {
                            value = encrypt(value);
                        }
                        return bareEditor.putString(key, value);
                    }

                    @Override
                    public Editor putInt(String key, int value) {
                        return bareEditor.putString(key, encrypt(Integer.toString(value)));
                    }

                    @Override
                    public Editor putLong(String key, long value) {
                        return bareEditor.putString(key, encrypt(Long.toString(value)));
                    }

                    @Override
                    public Editor putFloat(String key, float value) {
                        return bareEditor.putString(key, decrypt(Float.toString(value)));
                    }

                    @Override
                    public Editor putBoolean(String key, boolean value) {
                        return bareEditor.putString(key, encrypt(Boolean.toString(value)));
                    }

                    @Override
                    public Editor remove(String key) {
                        return bareEditor.remove(key);
                    }

                    @Override
                    public Editor clear() {
                        return bareEditor.clear();
                    }

                    @Override
                    public boolean commit() {
                        return bareEditor.commit();
                    }

                    @Override
                    public void apply() {
                        bareEditor.apply();
                    }
                };
            }

            @Override
            public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
                barePreferences.registerOnSharedPreferenceChangeListener(listener);
            }

            @Override
            public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
                barePreferences.unregisterOnSharedPreferenceChangeListener(listener);
            }
        };
    }

    public static void clearCache(final String sharedPrefName) {
        SharedPreferences sharedPreferences = getSharedPreferences(sharedPrefName);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public static void removeAccessTokenFromCache(final String sharedPrefName) {
        SharedPreferences sharedPreferences = getSharedPreferences(sharedPrefName);
        final Map<String, ?> cacheValues = sharedPreferences.getAll();
        final String keyToRemove = getCacheKeyForAccessToken(cacheValues);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (keyToRemove != null) {
            editor.remove(keyToRemove);
            editor.apply();
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
