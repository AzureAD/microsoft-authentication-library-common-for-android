package com.microsoft.identity.common.internal.cache;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * Convenience class for accessing {@link SharedPreferences}.
 */
public class SharedPreferencesFileManager implements ISharedPreferencesFileManager {

    private final String mSharedPreferencesFileName;
    private final SharedPreferences mSharedPreferences;
    private final IStorageHelper mStorageHelper;

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
        mStorageHelper = null;
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
        mStorageHelper = null;
    }

    /**
     * Constructs an instance of SharedPreferencesFileManager.
     * The default operating mode is {@link Context#MODE_PRIVATE}
     *
     * @param context       Interface to global information about an application environment.
     * @param name          The desired {@link android.content.SharedPreferences} file. It will be created
     *                      if it does not exist.
     * @param storageHelper The {@link IStorageHelper} to handle encryption/decryption of values.
     */
    public SharedPreferencesFileManager(
            final Context context,
            final String name,
            final IStorageHelper storageHelper) {
        mSharedPreferencesFileName = name;
        mSharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        mStorageHelper = storageHelper;
    }

    /**
     * Constructs an instance of SharedPreferencesFileManager.
     *
     * @param context       Interface to global information about an application enviroment.
     * @param name          The desired {@link SharedPreferences} file. It will be created
     *                      if it does not exist.
     * @param operatingMode Operating mode {@link Context#getSharedPreferences(String, int)}.
     * @param storageHelper The {@link IStorageHelper} to handle encryption/decryption of values.
     */
    public SharedPreferencesFileManager(
            final Context context,
            final String name,
            final int operatingMode,
            final IStorageHelper storageHelper) {
        mSharedPreferencesFileName = name;
        mSharedPreferences = context.getSharedPreferences(name, operatingMode);
        mStorageHelper = storageHelper;
    }

    // Suppressing because cache integrity is a greater concern than perf
    @SuppressLint("ApplySharedPref")
    @Override
    public final void putString(
            final String key,
            final String value) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();

        if (null == mStorageHelper) {
            editor.putString(key, value);
        } else {
            editor.putString(key, encrypt(value));
        }

        editor.commit();
    }

    @Override
    public final String getString(final String key) {
        String restoredValue = mSharedPreferences.getString(key, null);

        if (null != mStorageHelper && !StringExtensions.isNullOrBlank(restoredValue)) {
            restoredValue = decrypt(restoredValue);
        }

        return restoredValue;
    }

    @Override
    public final String getSharedPreferencesFileName() {
        return mSharedPreferencesFileName;
    }

    @Override
    public final Map<String, String> getAll() {
        final Map<String, String> entries = (Map<String, String>) mSharedPreferences.getAll();

        if (null != mStorageHelper) {
            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                entry.setValue(decrypt(entry.getValue()));
            }
        }

        return entries;
    }

    @Override
    public final boolean contains(final String key) {
        return mSharedPreferences.contains(key);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public final void clear() {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void remove(final String key) {
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(key);
        editor.commit();
    }

    private String encrypt(final String clearText) {
        return encryptDecryptInternal(clearText, true);

    }

    private String decrypt(final String encryptedBlob) {
        return encryptDecryptInternal(encryptedBlob, false);
    }

    private String encryptDecryptInternal(final String inputText, final boolean encrypt) {
        String result;
        try {
            result = encrypt ? mStorageHelper.encrypt(inputText) : mStorageHelper.decrypt(inputText);
        } catch (GeneralSecurityException | IOException e) {
            // TODO log an error? Throw a RuntimeException?
            result = null;
        }
        return result;
    }

}
