package com.microsoft.identity.common;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/**
 * Convenience class for accessing {@link SharedPreferences}.
 */
public class SharedPreferencesFileManager {

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

    /**
     * Saves a Token (as a {@link String} to the {@link SharedPreferences} file.
     *
     * @param key   The name (key) of the Token to save.
     * @param value The Token's value (as a {@link String}).
     */
    public final void putString(
            final String key,
            final String value) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Retrieves a Token from the {@link SharedPreferences} file.
     *
     * @param key The name (key) of the Token.
     * @return The Token's value or null if no value could be found.
     */
    public final String getString(final String key) {
        return mSharedPreferences.getString(key, null);
    }

    /**
     * Returns the name of {@link SharedPreferences} file in use.
     *
     * @return The name of the file.
     */
    public final String getSharedPreferencesFileName() {
        return mSharedPreferencesFileName;
    }

    /**
     * Retuns all entries in the {@link SharedPreferences} file.
     *
     * @return A Map of all entries.
     */
    public final Map<String, ?> getAll() {
        return mSharedPreferences.getAll();
    }

    /**
     * Tests if the {@link SharedPreferences} file contains an entry for the supplied key.
     *
     * @param key The key to consult.
     * @return True, if the key has an associate entry.
     */
    public final boolean contains(final String key) {
        return mSharedPreferences.contains(key);
    }

    /**
     * Clears the contents of the {@link SharedPreferences} file.
     */
    public final void clear() {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Removes any associated entry for the supplied key.
     *
     * @param key The key whose value should be cleared.
     */
    public void remove(final String key) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

}
