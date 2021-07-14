package com.microsoft.identity.common.internal.util;

import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;

import lombok.NonNull;

/**
 * An adapter for SharedPreferences that allows it to appear as a nameValue store with String
 * value types.
 */
public class SharedPrefStringNameValueStorage extends AbstractSharedPrefNameValueStorage<String> {
    public SharedPrefStringNameValueStorage(ISharedPreferencesFileManager mManager) {
        super(mManager);
    }

    @Override
    public String get(@NonNull String name) { return mManager.getString(name); }

    @Override
    public void put(@NonNull String name, String value) {
        mManager.putString(name, value);
    }
}