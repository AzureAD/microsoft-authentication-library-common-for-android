package com.microsoft.identity.common.internal.cache;

import android.content.SharedPreferences;

import java.util.Map;


public interface ISharedPreferencesFileManager {
    /**
     * Saves a Token (as a {@link String} to the {@link SharedPreferences} file.
     *
     * @param key   The name (key) of the Token to save.
     * @param value The Token's value (as a {@link String}).
     */
    void putString(String key, String value);

    /**
     * Retrieves a Token from the {@link SharedPreferences} file.
     *
     * @param key The name (key) of the Token.
     * @return The Token's value or null if no value could be found.
     */
    String getString(String key);

    /**
     * Returns the name of {@link SharedPreferences} file in use.
     *
     * @return The name of the file.
     */
    String getSharedPreferencesFileName();

    /**
     * Returns all entries in the {@link SharedPreferences} file.
     * <p>
     * Note that you must not modify the collection returned by this method, or alter any of its
     * contents. The consistency of your stored data is not guaranteed if you do.
     *
     * @return A Map of all entries.
     */
    Map<String, ?> getAll();

    /**
     * Tests if the {@link SharedPreferences} file contains an entry for the supplied key.
     *
     * @param key The key to consult.
     * @return True, if the key has an associate entry.
     */
    boolean contains(String key);

    /**
     * Clears the contents of the {@link SharedPreferences} file.
     */
    void clear();

    /**
     * Removes any associated entry for the supplied key.
     *
     * @param key The key whose value should be cleared.
     */
    void remove(final String key);
}
