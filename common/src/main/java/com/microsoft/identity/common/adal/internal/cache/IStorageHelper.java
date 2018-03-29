package com.microsoft.identity.common.adal.internal.cache;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

interface IStorageHelper {
    /**
     * Encrypt text with current key based on API level.
     *
     * @param clearText Clear text to encrypt.
     * @return Encrypted blob.
     * @throws GeneralSecurityException for key related exceptions.
     * @throws IOException              For general IO related exceptions.
     */
    String encrypt(String clearText)
            throws GeneralSecurityException, IOException;

    /**
     * Decrypt encrypted blob with either user provided key or key persisted in AndroidKeyStore.
     *
     * @param encryptedBlob The blob to decrypt
     * @return Decrypted clear text.
     * @throws GeneralSecurityException for key related exceptions.
     * @throws IOException              For general IO related exceptions.
     */
    String decrypt(String encryptedBlob)
            throws GeneralSecurityException, IOException;

    /**
     * Get Secret Key based on API level to use in encryption. Decryption key
     * depends on version# since user can migrate to new Android.OS
     *
     * @return SecretKey Get Secret Key based on API level to use in encryption.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    SecretKey loadSecretKeyForEncryption() throws IOException,
            GeneralSecurityException;

    /**
     * Get Secret Key based on API level to use in encryption. Decryption key
     * depends on version# since user can migrate to new Android.OS
     *
     * @param defaultBlobVersion the blobVersion to use by default
     * @return SecretKey Get Secret Key based on API level to use in encryption.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    SecretKey loadSecretKeyForEncryption(String defaultBlobVersion) throws IOException,
            GeneralSecurityException;
}
