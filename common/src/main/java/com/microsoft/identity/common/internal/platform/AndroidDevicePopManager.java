//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.platform;

import static com.microsoft.identity.common.internal.util.AndroidKeyStoreUtil.applyKeyStoreLocaleWorkarounds;
import static com.microsoft.identity.common.java.WarningType.NewApi;
import static com.microsoft.identity.common.java.util.ported.DateUtilities.LOCALE_CHANGE_LOCK;
import static com.microsoft.identity.common.java.util.ported.DateUtilities.isLocaleCalendarNonGregorian;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;

import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.java.crypto.IKeyStoreKeyManager;
import com.microsoft.identity.common.java.crypto.SecureHardwareState;
import com.microsoft.identity.common.java.platform.AbstractDevicePopManager;
import com.microsoft.identity.common.logging.Logger;
import com.nimbusds.jose.crypto.impl.RSAKeyUtils;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.security.auth.x500.X500Principal;

import lombok.NonNull;

public class AndroidDevicePopManager extends AbstractDevicePopManager {

    private static final String TAG = AndroidDevicePopManager.class.getSimpleName();

    /**
     * The name of the KeyStore to use.
     */
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    /**
     * Error message from underlying KeyStore that StrongBox HAL is unavailable.
     */
    public static final String STRONG_BOX_UNAVAILABLE_EXCEPTION = "StrongBoxUnavailableException";

    /**
     * Seeing this on android 14, we think it's being caused by the new IAR requirement
     * <a href="https://android.googlesource.com/platform/compatibility/cdd/+/e2fee2f/9_security-model/9_11_keys-and-credentials.md">...</a>
     */
    public static final String NEGATIVE_THOUSAND_INTERNAL_ERROR = "internal Keystore code: -1000";

    /**
     * Error message from underlying KeyStore that an attestation certificate could not be
     * generated, typically due to lack of API support via {@link KeyGenParameterSpec.Builder#setAttestationChallenge(byte[])}.
     */
    public static final String FAILED_TO_GENERATE_ATTESTATION_CERTIFICATE_CHAIN = "Failed to generate attestation certificate chain";

    /**
     * The NIST advised min keySize for RSA pairs.
     */
    private static final int RSA_KEY_SIZE = 2048;

    private final Context mContext;

    public AndroidDevicePopManager(@NonNull final Context context) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        this(context, DEFAULT_KEYSTORE_ENTRY_ALIAS);
    }

    public AndroidDevicePopManager(@NonNull final Context context, @NonNull final String alias) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        super(createKeyStoreKeyManager(alias));
        mContext = context;
    }

    private static IKeyStoreKeyManager<KeyStore.PrivateKeyEntry> createKeyStoreKeyManager(@NonNull final String alias) throws CertificateException, NoSuchAlgorithmException, IOException, KeyStoreException {
        final KeyStore instance = KeyStore.getInstance(ANDROID_KEYSTORE);
        instance.load(null);
        return AndroidDeviceKeyManager.<KeyStore.PrivateKeyEntry>builder()
                .keyAlias(alias)
                .keyStore(instance)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public KeyPair generateNewRsaKeyPair(int keySize) throws UnsupportedOperationException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        return generateNewRsaKeyPair(mContext, keySize);
    }

    @Override
    protected SecureHardwareState getSecureHardwareState(@NonNull KeyPair kp) {
        final String methodTag = TAG + ":getSecureHardwareState";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                final PrivateKey privateKey = kp.getPrivate();
                final KeyFactory factory = KeyFactory.getInstance(
                        privateKey.getAlgorithm(), ANDROID_KEYSTORE
                );
                final KeyInfo info = factory.getKeySpec(privateKey, KeyInfo.class);
                final boolean isInsideSecureHardware = info.isInsideSecureHardware();
                Logger.info(methodTag, "SecretKey is secure hardware backed? " + isInsideSecureHardware);
                return isInsideSecureHardware
                        ? SecureHardwareState.TRUE_UNATTESTED
                        : SecureHardwareState.FALSE;
            } catch (final NoSuchAlgorithmException | NoSuchProviderException |
                           InvalidKeySpecException e) {
                Logger.error(methodTag, "Failed to query secure hardware state.", e);
                return SecureHardwareState.UNKNOWN_QUERY_ERROR;
            }
        } else {
            Logger.info(methodTag, "Cannot query secure hardware state (API unavailable <23)");
        }

        return SecureHardwareState.UNKNOWN_DOWNLEVEL;
    }

    @Override
    protected void performCleanupIfMintShrFails(@NonNull final Exception e) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && e.getCause() instanceof KeyPermanentlyInvalidatedException) {
            Logger.warn(TAG, "Unable to access asymmetric key - clearing.");
            clearAsymmetricKey();
        }
    }

    /**
     * Generates a new RSA KeyPair of the specified lenth.
     *
     * @param context    The current application Context.
     * @param minKeySize The minimum keysize to use.
     * @return The newly generated RSA KeyPair.
     * @throws UnsupportedOperationException
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressLint(NewApi)
    private KeyPair generateNewRsaKeyPair(@androidx.annotation.NonNull final Context context,
                                          final int minKeySize)
            throws UnsupportedOperationException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        final int MAX_RETRIES = 4;

        for (int ii = 0; ii < MAX_RETRIES; ii++) {
            KeyPair kp = null;
            boolean tryStrongBox = true;
            boolean tryImport = true;
            boolean trySetAttestationChallenge = true;
            boolean generated = false;
            while (!generated) {
                try {
                    kp = generateNewKeyPair(context, tryStrongBox, tryImport, trySetAttestationChallenge);
                    generated = true;

                    // Log success (with flags used)
                    final String successMessage = String.format("Key pair generated successfully (StrongBox [%b], Import [%b], Attestation Challenge [%b])",
                            tryStrongBox, tryImport, trySetAttestationChallenge);
                    Logger.info(TAG, successMessage);
                } catch (final ProviderException e) {
                    // This mechanism is terrible.  But there are stern warnings that even attempting to
                    // mention these classes in a catch clause might cause failures. So we're going to look
                    // at the exception names.


                    if (tryStrongBox && isStrongBoxUnavailableException(e)) {
                        Logger.error(TAG, "StrongBox unavailable. Skipping StrongBox then retry.", e);
                        tryStrongBox = false;
                        continue;
                    } else if (tryImport && e.getClass().getSimpleName().equals("SecureKeyImportUnavailableException")) {
                        Logger.error(TAG, "Import unsupported. Skipping import flag then retry.", e);
                        tryImport = false;

                        if (tryStrongBox && null != e.getCause() && (isStrongBoxUnavailableException(e.getCause()) || isNegativeInternalError(e.getCause()))) {
                            // On some devices (notably, Huawei Mate 9 Pro), StrongBox errors are
                            // the cause of the surfaced SecureKeyImportUnavailableException.
                            tryStrongBox = false;
                        }

                        continue;
                    } else if (trySetAttestationChallenge && FAILED_TO_GENERATE_ATTESTATION_CERTIFICATE_CHAIN.equalsIgnoreCase(e.getMessage())) {
                        Logger.error(TAG, "Failed to generate attestation cert. Skipping attestation then retry.", e);
                        trySetAttestationChallenge = false;

                        continue;
                    } else if (tryStrongBox && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            && (Build.VERSION.RELEASE_OR_CODENAME.equals("UpsideDownCake") || Build.VERSION.RELEASE_OR_CODENAME.equals("14"))
                            && (null != e.getCause()) && isNegativeInternalError(e.getCause())) {
                        // Android 14 specific error where strong box is failing, most likely because of IAR requirement in android 14
                        // https://android.googlesource.com/platform/compatibility/cdd/+/e2fee2f/9_security-model/9_11_keys-and-credentials.md
                        // Had to check code name, as android 14 device in beta seems to still show 33 as SDK int
                        // TO-DO : https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2574078
                        Logger.error(TAG, "Android 14 Internal Key store error with StrongBox. Skipping strongbox then retry.", e);
                        tryStrongBox = false;
                        continue;
                    }

                    // We were unsuccessful, cleanup after ourselves and throw...
                    clearAsymmetricKey();
                    throw e;
                }
            }

            // Due to a bug in some versions of Android, keySizes may not be exactly as specified
            // To generate a 2048-bit key, two primes of length 1024 are multiplied -- this product
            // may be 2047 in length in some cases which causes Nimbus to throw an IllegalArgumentException.
            // To avoid this, check the keysize prior to returning the generated KeyPair.

            // Since this seems to be nondeterministic in nature, attempt this a maximum of 4 times.
            final int length = RSAKeyUtils.keyBitLength(kp.getPrivate());

            // If the key material is hidden (HSM or otherwise) the length is -1
            if (length >= minKeySize || length < 0) {
                // Log out secure hardware state -- we don't need the result here
                getSecureHardwareState(kp);

                return kp;
            }
        }

        // Clean up... we generated a cert, but it cannot be used.
        clearAsymmetricKey();

        throw new UnsupportedOperationException(
                "Failed to generate valid KeyPair. Attempted " + MAX_RETRIES + " times."
        );
    }

    private static boolean isStrongBoxUnavailableException(@androidx.annotation.NonNull final Throwable t) {
        final boolean isStrongBoxException = t.getClass().getSimpleName().equals(STRONG_BOX_UNAVAILABLE_EXCEPTION);

        if (isStrongBoxException) {
            Logger.error(TAG + ":isStrongBoxUnavailableException", "StrongBox not supported.", t);
        }

        return isStrongBoxException;
    }

    private static boolean isNegativeInternalError(@androidx.annotation.NonNull final Throwable t) {
        final boolean isNegativeInternalError = t.getMessage() != null && t.getMessage().contains(NEGATIVE_THOUSAND_INTERNAL_ERROR);

        if (isNegativeInternalError) {
            Logger.error(TAG, "StrongBox not supported. internal Keystore code: -1000", t);
        }

        return isNegativeInternalError;
    }

    /**
     * Generates a new {@link KeyPair}.
     *
     * @param context                    The application Context.
     * @param useStrongbox               True if StrongBox should be used, false otherwise.
     * @param trySetAttestationChallenge
     * @return The newly generated KeyPair.
     * @throws InvalidAlgorithmParameterException If the designated crypto algorithm is not
     *                                            supported for the designated parameters.
     * @throws NoSuchAlgorithmException           If the designated crypto algorithm is not supported.
     * @throws NoSuchProviderException            If the designated crypto provider cannot be found.
     * @throws StrongBoxUnavailableException      If StrongBox is unavailable.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private KeyPair generateNewKeyPair(@androidx.annotation.NonNull final Context context, final boolean useStrongbox,
                                       final boolean enableImport, final boolean trySetAttestationChallenge)
            throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, StrongBoxUnavailableException {
        synchronized (isLocaleCalendarNonGregorian(Locale.getDefault()) ? LOCALE_CHANGE_LOCK : new Object()) {
            // See: https://issuetracker.google.com/issues/37095309
            final Locale currentLocale = Locale.getDefault();
            applyKeyStoreLocaleWorkarounds(currentLocale);

            try {
                final KeyPairGenerator kpg = getInitializedRsaKeyPairGenerator(
                        context,
                        RSA_KEY_SIZE,
                        useStrongbox,
                        enableImport,
                        trySetAttestationChallenge
                );
                final KeyPair keyPair = kpg.generateKeyPair();

                return keyPair;
            } finally {
                // Reset our locale to the default
                Locale.setDefault(currentLocale);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private KeyPairGenerator getInitializedRsaKeyPairGenerator(@androidx.annotation.NonNull final Context context,
                                                               final int keySize,
                                                               final boolean useStrongbox,
                                                               final boolean enableImport,
                                                               final boolean trySetAttestationChallenge)
            throws InvalidAlgorithmParameterException, NoSuchProviderException, NoSuchAlgorithmException {
        // Create the KeyPairGenerator
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                AndroidDevicePopManager.KeyPairGeneratorAlgorithms.RSA,
                ANDROID_KEYSTORE
        );

        // Initialize it!
        initialize(context, keyPairGenerator, keySize, useStrongbox, enableImport, trySetAttestationChallenge);

        return keyPairGenerator;
    }

    /**
     * Initialize the provided {@link KeyPairGenerator}.
     *
     * @param context                    The current application Context.
     * @param keyPairGenerator           The KeyPairGenerator to initialize.
     * @param keySize                    The RSA keysize.
     * @param useStrongbox               True if StrongBox should be used, false otherwise. Please note that
     *                                   StrongBox may not be supported on all devices.
     * @param enableImport               True if imports to the underlying KeyStore are allowed.
     * @param trySetAttestationChallenge True if we should attempt to generate an attestation challenge cert.
     * @throws InvalidAlgorithmParameterException
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void initialize(@androidx.annotation.NonNull final Context context,
                            @androidx.annotation.NonNull final KeyPairGenerator keyPairGenerator,
                            final int keySize,
                            final boolean useStrongbox,
                            final boolean enableImport,
                            final boolean trySetAttestationChallenge) throws InvalidAlgorithmParameterException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initializePre23(context, keyPairGenerator, keySize);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            initialize23(keyPairGenerator, keySize, useStrongbox, trySetAttestationChallenge);
        } else {
            initialize28(keyPairGenerator, keySize, useStrongbox, enableImport, trySetAttestationChallenge);
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initialize23(@androidx.annotation.NonNull final KeyPairGenerator keyPairGenerator,
                              final int keySize,
                              final boolean useStrongbox,
                              final boolean trySetAttestationChallenge) throws InvalidAlgorithmParameterException {
        KeyGenParameterSpec.Builder builder;
        builder = new KeyGenParameterSpec.Builder(
                mKeyManager.getKeyAlias(),
                KeyProperties.PURPOSE_SIGN
                        | KeyProperties.PURPOSE_VERIFY
                        | KeyProperties.PURPOSE_ENCRYPT
                        | KeyProperties.PURPOSE_DECRYPT
        )
                .setKeySize(keySize)
                .setSignaturePaddings(
                        KeyProperties.SIGNATURE_PADDING_RSA_PKCS1
                )
                .setDigests(
                        KeyProperties.DIGEST_NONE,
                        KeyProperties.DIGEST_SHA1,
                        KeyProperties.DIGEST_SHA256
                ).setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_RSA_OAEP,
                        KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
                );

        if (trySetAttestationChallenge && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder = setAttestationChallenge(builder);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongbox) {
            Logger.verbose(
                    TAG,
                    "Attempting to apply StrongBox isolation."
            );
            builder = applyHardwareIsolation(builder);
        }

        final AlgorithmParameterSpec spec = builder.build();
        keyPairGenerator.initialize(spec);
    }

    /**
     * Key attestation should ideally happen with the support of a separate server that you trust.
     * In that use case an attestationChallenge should be generated by that server.  The challenge will
     * be included as extension data in the attestation certificate associated with the generated key pair
     * And that attestation certificate can be validated server side
     *
     * Currently we don't need to support attestation so the challenge should be set to null
     *
     * Refer to: <a href="https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder#setAttestationChallenge(byte[])">setAttestationChallenge</a>
     * Refer to: <a href="https://developer.android.com/training/articles/security-key-attestation">Verifying hardware-backed key pairs with Key Attestation</a>
     */
    @SuppressLint(NewApi)
    @RequiresApi(Build.VERSION_CODES.N)
    @androidx.annotation.NonNull
    private KeyGenParameterSpec.Builder setAttestationChallenge(
            @androidx.annotation.NonNull final KeyGenParameterSpec.Builder builder) {
        return builder.setAttestationChallenge(null);
    }

    /**
     * Applies hardware backed security to the supplied {@link KeyGenParameterSpec.Builder}.
     *
     * @param builder The builder.
     * @return A reference to the supplied builder instance.
     */
    @SuppressLint(NewApi)
    @RequiresApi(Build.VERSION_CODES.P)
    @androidx.annotation.NonNull
    private static KeyGenParameterSpec.Builder applyHardwareIsolation(
            @androidx.annotation.NonNull final KeyGenParameterSpec.Builder builder) {
        return builder.setIsStrongBoxBacked(true);
    }

    @SuppressLint("InlinedApi")
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void initialize28(@androidx.annotation.NonNull final KeyPairGenerator keyPairGenerator,
                              final int keySize,
                              final boolean useStrongbox,
                              final boolean enableImport,
                              final boolean trySetAttestationChallenge) throws InvalidAlgorithmParameterException {
        int purposes = KeyProperties.PURPOSE_SIGN
                | KeyProperties.PURPOSE_VERIFY
                | KeyProperties.PURPOSE_ENCRYPT
                | KeyProperties.PURPOSE_DECRYPT;
        if (enableImport && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            purposes |= KeyProperties.PURPOSE_WRAP_KEY;
        }
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                mKeyManager.getKeyAlias(), purposes)
                .setKeySize(keySize)
                .setSignaturePaddings(
                        KeyProperties.SIGNATURE_PADDING_RSA_PKCS1
                )
                .setDigests(
                        KeyProperties.DIGEST_NONE,
                        KeyProperties.DIGEST_SHA1,
                        KeyProperties.DIGEST_SHA256
                ).setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_RSA_OAEP,
                        KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
                );

        if (trySetAttestationChallenge && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder = setAttestationChallenge(builder);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongbox) {
            Logger.verbose(
                    TAG,
                    "Attempting to apply StrongBox isolation."
            );
            builder = applyHardwareIsolation(builder);
        }

        final AlgorithmParameterSpec spec = builder.build();
        keyPairGenerator.initialize(spec);
    }


    @SuppressLint(NewApi)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @SuppressWarnings("deprecation")
    private void initializePre23(@androidx.annotation.NonNull final Context context,
                                 @androidx.annotation.NonNull final KeyPairGenerator keyPairGenerator,
                                 final int keySize) throws InvalidAlgorithmParameterException {
        final Calendar calendar = Calendar.getInstance();
        final Date start = getNow(calendar);
        calendar.add(Calendar.YEAR, AbstractDevicePopManager.CertificateProperties.CERTIFICATE_VALIDITY_YEARS);
        final Date end = calendar.getTime();

        final android.security.KeyPairGeneratorSpec.Builder specBuilder = new android.security.KeyPairGeneratorSpec.Builder(context)
                .setAlias(mKeyManager.getKeyAlias())
                .setStartDate(start)
                .setEndDate(end)
                .setSerialNumber(AndroidDevicePopManager.CertificateProperties.SERIAL_NUMBER)
                .setSubject(new X500Principal(AndroidDevicePopManager.CertificateProperties.COMMON_NAME));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            specBuilder.setAlgorithmParameterSpec(
                    new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4)
            );
        }

        final android.security.KeyPairGeneratorSpec spec = specBuilder.build();
        keyPairGenerator.initialize(spec);
    }
}
