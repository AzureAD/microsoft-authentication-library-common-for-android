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
package com.microsoft.identity.common.adal.internal;

import static com.microsoft.identity.common.java.constants.SpotbugsWarning.ME_ENUM_FIELD_SETTER;

import android.os.Build;

import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.java.challengehandlers.IDeviceCertificate;
import com.microsoft.identity.common.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Settings to be used in AuthenticationContext.
 */
public enum AuthenticationSettings {
    /**
     * Singleton setting instance.
     */
    INSTANCE;

    private static final String TAG = AuthenticationSettings.class.getSimpleName();

    private static final int SECRET_RAW_KEY_LENGTH = 32;

    private static final int DEFAULT_EXPIRATION_BUFFER = 300;

    private static final int DEFAULT_READ_CONNECT_TIMEOUT = 30000;

    // This is used to accept two broker key. Today we have company portal and azure authenticator apps,
    // and each app is also going to send the other app's keys. They need to set package name and corresponding
    // keys in the map. used by broker.
    private final Map<String, byte[]> mBrokerSecretKeys = new HashMap<String, byte[]>(2);

    private AtomicReference<byte[]> mSecretKeyData = new AtomicReference<>();

    private String mBrokerPackageName = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME;

    private String mBrokerSignature = AuthenticationConstants.Broker.COMPANY_PORTAL_APP_RELEASE_SIGNATURE;

    private String mActivityPackageName;

    private boolean mEnableHardwareAcceleration = true;

    /**
     * SharedPreference package name to load this file from different context.
     */
    private String mSharedPrefPackageName;

    /**
     * set to be false in default.
     * if user want to use broker
     * the mUseBroker should be set explicitly by calling {@link #setUseBroker(boolean)}
     */
    private boolean mUseBroker = false;

    /**
     * Expiration buffer in seconds.
     */
    private int mExpirationBuffer = DEFAULT_EXPIRATION_BUFFER;

    private int mConnectTimeOut = DEFAULT_READ_CONNECT_TIMEOUT;

    private int mReadTimeOut = DEFAULT_READ_CONNECT_TIMEOUT;

    private boolean mIgnoreKeyLoaderNotFoundError = false;

    /**
     * Get bytes to derive secretKey to use in encrypt/decrypt.
     *
     * @return byte[] secret data
     */
    public byte[] getSecretKeyData() {
        return mSecretKeyData.get();
    }

    /**
     * Get an {@link ArrayList} of bytes to derive secret key to use in encryption/decryption. used by broker only.
     * {@link Map} contains two broker app secret key to do encryption/decryption, and it's keyed by broker package name.
     *
     * @return {@link Map} of byte[] secret key which is keyed by broker package name.
     */
    public Map<String, byte[]> getBrokerSecretKeys() {
        return Collections.unmodifiableMap(mBrokerSecretKeys);
    }

    /**
     * Set raw bytes to derive secretKey to use in encrypt/decrypt. KeySpec
     * algorithm is AES.
     * <p>
     * Please note: If a device with an existing installation of the ADAL/MSAL host-app is upgraded
     * from API 17 -> API 18+ then the previously-used secret key data must continue to be supplied
     * in order to not lose SSO state when reading cache entries written prior to upgrade.
     * <p>
     * For apps which may wish to transition away from this API long-term, they may do so
     * opportunistically by clearing SharedPreferences and recreating their AuthenticationContext
     * (or PublicClientApplication) after signing all users out or before requiring an interactive
     * authentication prompt from the current user. Be advised that changing the keystore/secret
     * keys will render all cache entries unreadable if they were written by another key.
     *
     * @param rawKey App related key to use in encrypt/decrypt
     */
    public void setSecretKey(byte[] rawKey) {
        final String methodTag = TAG + ":setSecretKey";
        if (rawKey == null || rawKey.length != SECRET_RAW_KEY_LENGTH) {
            throw new IllegalArgumentException("rawKey");
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Logger.warn(methodTag, "You're using setSecretKey in a version of android " +
                    "that supports keyStore functionality.  Consider not doing this, as it only exists " +
                    "for devices with an SDK lower than " + Build.VERSION_CODES.JELLY_BEAN_MR2);
        }
        mSecretKeyData.set(rawKey);
    }

    /**
     * set two raw bytes to derive secretKey to use in encrypt/decrypt. KeySpec
     * algorithm is AES. used by broker only.
     *
     * @param secretKeys App related keys to use in encrypt/decrypt. Should contain two secret keys.
     */
    public void setBrokerSecretKeys(final Map<String, byte[]> secretKeys) {
        if (secretKeys == null) {
            throw new IllegalArgumentException("The passed in secret key map is null.");
        }

        if (secretKeys.size() != 2) {
            throw new IllegalArgumentException("Expect two keys are passed in.");
        }

        for (Map.Entry<String, byte[]> entry : secretKeys.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length != SECRET_RAW_KEY_LENGTH) {
                throw new IllegalArgumentException("Passed in raw key is null or length is not as expected. ");
            }

            mBrokerSecretKeys.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Clear broker secret keys.
     * Introduced as a temporary workaround to make sure Broker code clears up Broker keys in common before it's used by ADAL/MSAL.
     */
    public void clearBrokerSecretKeys() {
        mBrokerSecretKeys.clear();
    }

    /**
     * Clears any secret keys set by legacy {@link #setSecretKey(byte[])} API.
     */
    public void clearLegacySecretKeyConfiguration() {
        Logger.info(
                TAG + ":clearLegacySecretKeyConfiguration",
                "Clearing legacy secret key configuration."
        );
        mBrokerSecretKeys.clear();
        mSecretKeyData.set(null);
    }

    /**
     * For test cases only.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public void clearSecretKeysForTestCases() {
        clearLegacySecretKeyConfiguration();
    }

    /**
     * Gets packagename for broker app that installed authenticator.
     *
     * @return packagename
     */
    public String getBrokerPackageName() {
        return mBrokerPackageName;
    }

    /**
     * Sets package name for broker app that installed authenticator.
     *
     * @param packageName package name related to broker
     */
    public void setBrokerPackageName(String packageName) {
        if (StringExtensions.isNullOrBlank(packageName)) {
            throw new IllegalArgumentException("packageName cannot be empty or null");
        }
        mBrokerPackageName = packageName;
    }

    /**
     * Gets broker signature for broker app that installed authenticator.
     *
     * @return signature
     */
    public String getBrokerSignature() {
        return mBrokerSignature;
    }

    /**
     * Sets broker app info for ADAL to use.
     *
     * @param brokerSignature Signature for broker
     */
    public void setBrokerSignature(String brokerSignature) {
        if (StringExtensions.isNullOrBlank(brokerSignature)) {
            throw new IllegalArgumentException("brokerSignature cannot be empty or null");
        }
        mBrokerSignature = brokerSignature;
    }

    /**
     * get package name to setup intent for AuthenticationActivity.
     *
     * @return Package name for activity
     */
    public String getActivityPackageName() {
        return mActivityPackageName;
    }

    /**
     * set package name to setup intent for AuthenticationActivity.
     *
     * @param activityPackageName activity to use from different package
     */
    public void setActivityPackageName(String activityPackageName) {
        if (StringExtensions.isNullOrBlank(activityPackageName)) {
            throw new IllegalArgumentException("activityPackageName cannot be empty or null");
        }
        mActivityPackageName = activityPackageName;
    }

    /**
     * @return true if broker is not used, false otherwise
     * @deprecated As of release 1.1.14, replaced by {@link #getUseBroker()}
     */
    @Deprecated
    public boolean getSkipBroker() {
        return !mUseBroker;
    }

    /**
     * @param skip true if broker has to be skipped, false otherwise
     * @deprecated As of release 1.1.14, replaced by {@link #setUseBroker(boolean)}
     */
    @Deprecated
    public void setSkipBroker(boolean skip) {
        mUseBroker = !skip;
    }

    /**
     * Get broker usage.
     *
     * @return true if broker is used.
     */
    public boolean getUseBroker() {
        return mUseBroker;
    }

    /**
     * Set flag to use or skip broker.
     * By default, the flag value is false and ADAL will not talk to broker.
     *
     * @param useBroker True to use broker
     */
    @SuppressFBWarnings(ME_ENUM_FIELD_SETTER)
    public void setUseBroker(boolean useBroker) {
        mUseBroker = useBroker;
    }

    /**
     * Sets package name to use {@link DefaultTokenCacheStore} with sharedUserId
     * apps.
     *
     * @param packageNameForSharedFile Package name of other app
     */
    @SuppressFBWarnings(ME_ENUM_FIELD_SETTER)
    public void setSharedPrefPackageName(String packageNameForSharedFile) {
        mSharedPrefPackageName = packageNameForSharedFile;
    }

    /**
     * Gets package name provided for shared preferences.
     *
     * @return package name provided for shared preferences
     */
    public String getSharedPrefPackageName() {
        return mSharedPrefPackageName;
    }

    /**
     * Gets expiration buffer.
     *
     * @return the amount of buffer that is provided to the expiration time.
     */
    public int getExpirationBuffer() {
        return mExpirationBuffer;
    }

    /**
     * When checking access token expiration, it will check if the time to
     * expiration is less than this value(in seconds). Example: Set to 300 to
     * give 5min buffer. Token with Expiry time of 12:04 will say expired when
     * actual time is 12:00 with 5min buffer.
     *
     * @param expirationBuffer the time buffer provided to expiration time.
     */
    @SuppressFBWarnings(ME_ENUM_FIELD_SETTER)
    public void setExpirationBuffer(int expirationBuffer) {
        mExpirationBuffer = expirationBuffer;
    }

    /**
     * Get the connect timeout.
     *
     * @return connect timeout
     */
    public int getConnectTimeOut() {
        return mConnectTimeOut;
    }

    /**
     * Sets the maximum time in milliseconds to wait while connecting.
     * Connecting to a server will fail with a SocketTimeoutException if the
     * timeout elapses before a connection is established. Default value is
     * 30000 milliseconds.
     *
     * @param timeOutMillis the non-negative connect timeout in milliseconds.
     */
    public void setConnectTimeOut(int timeOutMillis) {
        if (timeOutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeOutMillis");
        }
        mConnectTimeOut = timeOutMillis;
    }

    /**
     * Get the read timeout.
     *
     * @return read timeout
     */
    public int getReadTimeOut() {
        return mReadTimeOut;
    }

    /**
     * Sets the maximum time to wait for an input stream read to complete before
     * giving up. Reading will fail with a SocketTimeoutException if the timeout
     * elapses before data becomes available. The default value is 30000.
     *
     * @param timeOutMillis the read timeout in milliseconds. Non-negative
     */
    public void setReadTimeOut(int timeOutMillis) {
        if (timeOutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeOutMillis");
        }

        mReadTimeOut = timeOutMillis;
    }

    /**
     * Method to enable/disable WebView hardware acceleration used in
     * {@link AuthenticationActivity} and {@link AuthenticationDialog}.
     * By default hardware acceleration is enable in WebView.
     *
     * @param enable if true, WebView would be hardwareAccelerated else it
     *               would be disable.
     * @see #getDisableWebViewHardwareAcceleration()
     */
    @SuppressFBWarnings(ME_ENUM_FIELD_SETTER)
    public void setDisableWebViewHardwareAcceleration(boolean enable) {
        mEnableHardwareAcceleration = enable;
    }

    /**
     * Method to check whether WebView used in {@link AuthenticationActivity} and
     * {@link AuthenticationDialog} would be hardware accelerated or not.
     *
     * @return true if WebView is hardwareAccelerated otherwise false
     * @see #setDisableWebViewHardwareAcceleration(boolean)
     */
    public boolean getDisableWebViewHardwareAcceleration() {
        return mEnableHardwareAcceleration;
    }

    /**
     * Method to suppress errors where KeyLoader is not found to decrypt the cache content
     * @param shouldIgnore if true, ignores keyloader not found errors
     */
    @SuppressFBWarnings(ME_ENUM_FIELD_SETTER)
    public void setIgnoreKeyLoaderNotFoundError(boolean shouldIgnore) {
        mIgnoreKeyLoaderNotFoundError = shouldIgnore;
    }

    /**
     * Method to check whether to suppress errors where KeyLoader is not found to decrypt
     * the cache content.
     */
    public boolean shouldIgnoreKeyLoaderNotFoundError() {
        return mIgnoreKeyLoaderNotFoundError;
    }
}
