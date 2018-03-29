package com.microsoft.identity.common.internal.cache;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/**
 * Convenience class for accessing {@link SharedPreferences}.
 */
public class SharedPreferencesFileManager implements ISharedPreferencesFileManager {

    private final String mSharedPreferencesFileName;
    private final SharedPreferences mSharedPreferences;

    /**
     * Constructs an instance of SharedPreferencesFileManager.
     * The default operating mode is {@link Context#MODE_PRIVATE}
     *
     * @param context Interface to global information about an application environment.
     * @param name    The desired {@link android.content.SharedPreferences} file. It will be created
     *                if it does not exist.
     */
    public SharedPreferencesFileManager(
            final Context context,
            final String name) {
        mSharedPreferencesFileName = name;
        mSharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * Constructs an instance of SharedPreferencesFileManager.
     *
     * @param context       Interface to global information about an application enviroment.
     * @param name          The desired {@link SharedPreferences} file. It will be created
     *                      if it does not exist.
     * @param operatingMode Operating mode {@link Context#getSharedPreferences(String, int)}.
     */
    public SharedPreferencesFileManager(
            final Context context,
            final String name,
            final int operatingMode) {
        mSharedPreferencesFileName = name;
        mSharedPreferences = context.getSharedPreferences(name, operatingMode);
    }

    @Override
    public final void putString(
            final String key,
            final String value) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    @Override
    public final String getString(final String key) {
        return mSharedPreferences.getString(key, null);
    }

    @Override
    public final String getSharedPreferencesFileName() {
        return mSharedPreferencesFileName;
    }

    @Override
    public final Map<String, ?> getAll() {
        return mSharedPreferences.getAll();
    }

    @Override
    public final boolean contains(final String key) {
        return mSharedPreferences.contains(key);
    }

    @Override
    public final void clear() {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    @Override
    public void remove(final String key) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

}
