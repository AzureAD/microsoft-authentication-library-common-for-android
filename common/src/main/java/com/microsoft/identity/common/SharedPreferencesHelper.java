package com.microsoft.identity.common;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Convenience class for accessing {@link SharedPreferences}.
 */
public class SharedPreferencesHelper {

    private final SharedPreferences mSharedPreferences;

    /**
     * Constructs an instance of SharedPreferencesHelper.
     * The default operating mode is {@link Context#MODE_PRIVATE}
     *
     * @param context Interface to global information about an application environment.
     * @param name    The desired {@link android.content.SharedPreferences} file. It will be created
     *                if it does not exist.
     */
    public SharedPreferencesHelper(
            final Context context,
            final String name) {
        mSharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * Constructs an instance of SharedPreferencesHelper.
     *
     * @param context       Interface to global information about an application enviroment.
     * @param name          The desired {@link SharedPreferences} file. It will be created
     *                      if it does not exist.
     * @param operatingMode Operating mode {@link Context#getSharedPreferences(String, int)}.
     */
    public SharedPreferencesHelper(
            final Context context,
            final String name,
            final int operatingMode) {
        mSharedPreferences = context.getSharedPreferences(name, operatingMode);
    }

    /**
     * Saves a Token (as a {@link String} to the {@link SharedPreferences} file.
     *
     * @param key   The name (key) of the Token to save.
     * @param value The Token's value (as a {@link String}).
     * @return True, if the value was successfully written.
     */
    public final boolean saveToken(
            final String key,
            final String value) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    /**
     * Asynchronously saves a Token (as a {@link String} to the {@link SharedPreferences} file.
     *
     * @param key   The name (key) of the Token to save.
     * @param value The Token's value (as a {@link String}).
     */
    public final void saveTokenAsync(
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
    public final String getToken(final String key) {
        return mSharedPreferences.getString(key, null);
    }

    /**
     * Gets the underlying {@link SharedPreferences} instance.
     *
     * @return The SharedPreference instance used by this SharedPreferencesHelper.
     */
    public final SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

}
