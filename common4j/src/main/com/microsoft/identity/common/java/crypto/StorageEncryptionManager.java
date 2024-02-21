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
package com.microsoft.identity.common.java.crypto;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;
import static com.microsoft.identity.common.java.crypto.key.KeyUtil.HMAC_ALGORITHM;
import static com.microsoft.identity.common.java.exception.ClientException.BAD_PADDING;
import static com.microsoft.identity.common.java.exception.ClientException.DATA_MALFORMED;
import static com.microsoft.identity.common.java.exception.ClientException.HMAC_MISMATCH;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_ALG_PARAMETER;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_BLOCK_SIZE;
import static com.microsoft.identity.common.java.exception.ClientException.INVALID_KEY;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_ALGORITHM;
import static com.microsoft.identity.common.java.exception.ClientException.NO_SUCH_PADDING;
import static com.microsoft.identity.common.java.exception.ClientException.UNEXPECTED_HMAC_LENGTH;
import static com.microsoft.identity.common.java.exception.ClientException.UNKNOWN_CRYPTO_ERROR;

import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.crypto.key.AbstractSecretKeyLoader;
import com.microsoft.identity.common.java.crypto.key.KeyUtil;
import com.microsoft.identity.common.java.crypto.key.PredefinedKeyLoader;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.logging.Logger;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import cz.msebera.android.httpclient.extras.Base64;
import lombok.NonNull;

/**
 * Encrypts/Decrypts data (to be stored into storage).
 * <p>
 * Encrypted Data             = [getEncodeVersionLengthPrefix()][ENCODE_VERSION][Base64EncodedEncryptedData]
 * Base64EncodedEncryptedData = Base64.encodedURLUnsafe([KeyIdentifier][encryptedData][iv][MACDigest])
 */
public abstract class StorageEncryptionManager implements IKeyAccessor {
    private static final String TAG = StorageEncryptionManager.class.getSimpleName() + "#";

    /**
     * IV Key length for AES (128 bit blocks)
     */
    public static final int IV_LENGTH = 16;

    /**
     * 256 bits output for signing message.
     */
    public static final int MAC_DIGEST_LENGTH = 32;

    /**
     * To keep track of encoding version and related flags.
     */
    private static final String ENCODE_VERSION = "E1";

    /**
     * Length of key identifiers that are appended before the encrypted data.
     * See: PredefinedKeyLoader.USER_PROVIDED_KEY_IDENTIFIER,
     *      AndroidWrappedKeyLoader.WRAPPED_KEY_KEY_IDENTIFIER,
     *      KeyringKeyLoader.KEY_IDENTIFIER
     */
    public static final int KEY_IDENTIFIER_LENGTH = 4;

    /**
     * IV generator.
     */
    private final IVGenerator mGenerator;

    public StorageEncryptionManager() {
        mGenerator = new IVGenerator() {
            final SecureRandom mRandom = new SecureRandom();

            @Override
            public byte[] generate() {
                final byte[] iv = new byte[IV_LENGTH];
                mRandom.nextBytes(iv);
                return iv;
            }
        };
    }

    /**
     * Exposed for test cases only.
     */
    /* package */ StorageEncryptionManager(@NonNull final IVGenerator generator) {
        mGenerator = generator;
    }

    @Override
    public byte[] encrypt(final byte[] plaintext)
            throws ClientException {
        final String methodName = ":encrypt";

        final String errCode;
        final Throwable exception;

        // load key for encryption if not loaded
        final AbstractSecretKeyLoader keyLoader = getKeyLoaderForEncryption();
        if (keyLoader == null) {
            // Developer error. Throw.
            throw new IllegalStateException("Cannot find a matching Keyloader.");
        }

        try {
            final SecretKey encryptionKey = keyLoader.getKey();
            final SecretKey encryptionHMACKey = KeyUtil.getHMacKey(encryptionKey);
            final byte[] keyIdentifier = keyLoader.getKeyTypeIdentifier().getBytes(ENCODING_UTF8);

            // IV: Initialization vector that is needed to start CBC
            final byte[] iv = mGenerator.generate();
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Set to encrypt mode
            final Cipher cipher = Cipher.getInstance(keyLoader.getCipherAlgorithm());
            final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, ivSpec);

            final byte[] encrypted = cipher.doFinal(plaintext);

            // Calculate digest from keyIdentifier+encryptedData+IV.
            mac.init(encryptionHMACKey);
            mac.update(keyIdentifier);
            mac.update(encrypted);
            mac.update(iv);
            final byte[] macDigest = mac.doFinal();

            // Init array to store keyIdentifier, encrypted data, iv, macdigest
            final byte[] blobVerAndEncryptedDataAndIVAndMacDigest = new byte[keyIdentifier.length
                    + encrypted.length + iv.length + macDigest.length];
            System.arraycopy(keyIdentifier, 0, blobVerAndEncryptedDataAndIVAndMacDigest, 0,
                    keyIdentifier.length);
            System.arraycopy(encrypted, 0, blobVerAndEncryptedDataAndIVAndMacDigest,
                    keyIdentifier.length, encrypted.length);
            System.arraycopy(iv, 0, blobVerAndEncryptedDataAndIVAndMacDigest, keyIdentifier.length
                    + encrypted.length, iv.length);
            System.arraycopy(macDigest, 0, blobVerAndEncryptedDataAndIVAndMacDigest, keyIdentifier.length
                    + encrypted.length + iv.length, macDigest.length);

            Logger.verbose(TAG + methodName, "Finished encryption");
            return prefixWithEncodeVersion(blobVerAndEncryptedDataAndIVAndMacDigest);
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (final IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        } catch (final BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        } catch (final InvalidAlgorithmParameterException e) {
            errCode = INVALID_ALG_PARAMETER;
            exception = e;
        } catch (final ClientException e) {
            throw e;
        } catch (final Throwable e) {
            errCode = UNKNOWN_CRYPTO_ERROR;
            exception = e;
        }

        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    @Override
    public byte[] decrypt(final byte[] cipherText) throws ClientException {
        final String methodTag = TAG + ":decrypt";

        final byte[] dataBytes;
        try {
            dataBytes = stripEncodeVersionFromCipherText(cipherText);
        } catch (final ClientException e){
            Logger.verbose(methodTag,
                    "Failed to strip encode version from cipherText, string might not be encrypted. Exception: ", e.getMessage());
            return cipherText;
        }

        final List<AbstractSecretKeyLoader> keysForDecryption = getKeyLoaderForDecryption(cipherText);
        if (keysForDecryption.size() == 0) {
            // Developer error. Throw.
            throw new IllegalStateException("Cannot find a matching Keyloader.");
        }

        final List<Throwable> suppressedException = new ArrayList<>();
        for (final AbstractSecretKeyLoader keyLoader : keysForDecryption) {
            try {
                return decryptWithSecretKey(dataBytes, keyLoader);
            } catch (final Throwable e) {
                Logger.warn(methodTag, "Failed to decrypt with key:" + keyLoader.getAlias() +
                        " thumbprint : " + KeyUtil.getKeyThumbPrint(keyLoader));
                suppressedException.add(e);
            }
        }

        // How should we throw an error?
        if (suppressedException.size() == 1){
            throw ExceptionAdapter.clientExceptionFromException(suppressedException.get(0));
        } else {
            final ClientException exceptionToThrowIfAllFails = new ClientException(ErrorStrings.DECRYPTION_FAILED,
                    "Tried all decryption keys and decryption still fails.");
            exceptionToThrowIfAllFails.getSuppressedException().addAll(suppressedException);
            throw exceptionToThrowIfAllFails;
        }
    }

    @Override
    public byte[] sign(byte[] text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean verify(byte[] text, byte[] signature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getThumbprint() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Certificate[] getCertificateChain() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SecureHardwareState getSecureHardwareState() {
        return SecureHardwareState.FALSE;
    }

    @Override
    public IKeyAccessor generateDerivedKey(byte[] label, byte[] ctx, CryptoSuite suite) {
        throw new UnsupportedOperationException();
    }

    /**
     * Decrypted the given encrypted blob with a key from {@link AbstractSecretKeyLoader}
     *
     * @param encryptedBlobWithoutEncodeVersion the encrypted blob with the format of
     *                                          [KeyIdentifier][encryptedData][iv][MACDigest].
     * @param keyLoader                         a {@link AbstractSecretKeyLoader} to load the decryption key from.
     */
    private byte[] decryptWithSecretKey(final byte[] encryptedBlobWithoutEncodeVersion,
                                        @NonNull final AbstractSecretKeyLoader keyLoader)
            throws ClientException {
        final String errCode;
        final Throwable exception;
        try {
            final SecretKey secretKey = keyLoader.getKey();
            final SecretKey hmacKey = KeyUtil.getHMacKey(secretKey);

            // byte input array: [keyVersion][encryptedData][IV][macDigest]
            final int ivIndex = encryptedBlobWithoutEncodeVersion.length - IV_LENGTH - MAC_DIGEST_LENGTH;
            final int macDigestIndex = encryptedBlobWithoutEncodeVersion.length - MAC_DIGEST_LENGTH;
            final int encryptedDataIndex = keyLoader.getKeyTypeIdentifier().getBytes(ENCODING_UTF8).length;
            final int encryptedDataLength = ivIndex - encryptedDataIndex;

            // Calculate digest again and compare to the appended value
            // incoming message: version+encryptedData+IV+Digest
            // Digest of EncryptedData+IV excluding the digest itself.
            final Cipher cipher = Cipher.getInstance(keyLoader.getCipherAlgorithm());
            final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            mac.update(encryptedBlobWithoutEncodeVersion, 0, macDigestIndex);
            final byte[] macDigest = mac.doFinal();

            // Compare digest of input message and calculated digest
            assertHMac(encryptedBlobWithoutEncodeVersion,
                    macDigestIndex,
                    encryptedBlobWithoutEncodeVersion.length,
                    macDigest);

            // Get IV related bytes from the end and set to decrypt mode with that IV.
            // It is using same cipher for different version since version# change
            // will mean upgrade to AndroidKeyStore and new Key.
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new IvParameterSpec(encryptedBlobWithoutEncodeVersion, ivIndex, IV_LENGTH)
            );

            // Decrypt data bytes from 0 to ivindex - offset the KeyIdentifier.
            // We only need to decrypt the actual content.
            return cipher.doFinal(
                    encryptedBlobWithoutEncodeVersion,
                    encryptedDataIndex,
                    encryptedDataLength
            );
        } catch (final NoSuchAlgorithmException e) {
            errCode = NO_SUCH_ALGORITHM;
            exception = e;
        } catch (final NoSuchPaddingException e) {
            errCode = NO_SUCH_PADDING;
            exception = e;
        } catch (final IllegalBlockSizeException e) {
            errCode = INVALID_BLOCK_SIZE;
            exception = e;
        } catch (final BadPaddingException e) {
            errCode = BAD_PADDING;
            exception = e;
        } catch (final InvalidKeyException e) {
            errCode = INVALID_KEY;
            exception = e;
        } catch (final InvalidAlgorithmParameterException e) {
            errCode = INVALID_ALG_PARAMETER;
            exception = e;
        } catch (final IllegalArgumentException e) {
            errCode = DATA_MALFORMED;
            exception = e;
        } catch (final ClientException e) {
            throw e;
        } catch (final Throwable e) {
            errCode = UNKNOWN_CRYPTO_ERROR;
            exception = e;
        }

        throw new ClientException(errCode, exception.getMessage(), exception);
    }

    /**
     * Returns Key identifier which was used for cipherText encryption.
     *
     * @param cipherText    the cipherText to be verified against.
     */
    protected static String getKeyIdentifierFromCipherText(final byte[] cipherText) {
        final String methodName = ":getKeyIdentifierFromCipherText";

        try {
            return new String(
                    stripEncodeVersionFromCipherText(cipherText),
                    0,
                    KEY_IDENTIFIER_LENGTH,
                    ENCODING_UTF8
            );
        } catch (final Exception e) {
            Logger.verbose(TAG + methodName, e.getMessage());
            return "EXCEPTION OCCURRED GETTING KEY IDENTIFIER";
        }
    }

    /**
     * Converts {@link StorageEncryptionManager#ENCODE_VERSION} string's length into a char -
     * to be prefixed in the encrypted string.
     */
    private char getEncodeVersionLengthPrefix() {
        return (char) ('a' + ENCODE_VERSION.length());
    }

    /**
     * Extracts {@link StorageEncryptionManager#ENCODE_VERSION} string from ciphertext.
     */
    private static int getEncodeVersionLengthFromCipherText(@NonNull final String cipherText) {
        return cipherText.charAt(0) - 'a';
    }

    /**
     * Base64 (URL unsafe) encode the encrypted data,
     * and prefix it with{@link StorageEncryptionManager#ENCODE_VERSION}.
     */
    private byte[] prefixWithEncodeVersion(final byte[] encryptedData) {
        final String encryptedText = Base64.encodeToString(encryptedData, Base64.NO_WRAP);
        final String result = getEncodeVersionLengthPrefix() + ENCODE_VERSION + encryptedText;
        return result.getBytes(ENCODING_UTF8);
    }

    /**
     * Remove {@link StorageEncryptionManager#ENCODE_VERSION} string from the given cipherText.
     * The returned string will also be Base64 (URL unsafe) decoded.
     *
     * @throws ClientException if the cipherText doesn't have the expected format.
     */
    private static byte[] stripEncodeVersionFromCipherText(final byte[] cipherText)
            throws ClientException {
        if (cipherText.length < 1) {
            throw new IllegalArgumentException("Input blob is null or length < 1");
        }

        final String cipherString = new String(cipherText, ENCODING_UTF8);
        int encodeVersionLength = getEncodeVersionLengthFromCipherText(cipherString);
        validateEncodeVersion(cipherString, encodeVersionLength);

        final String encryptedData = cipherString.substring(1 + encodeVersionLength);
        return Base64.decode(encryptedData, Base64.DEFAULT);
    }

    /**
     * Validate that the encryptedBlob contains a matching encode version.
     *
     * @param cipherString       the encrypted string to be verified.
     * @param encodeVersionLength length of the encode version as defined in the encrypted blob.
     * @throws ClientException if the string wasn't encoded with {@link StorageEncryptionManager#ENCODE_VERSION}.
     */
    private static void validateEncodeVersion(@NonNull final String cipherString,
                                              final int encodeVersionLength) throws ClientException {
        if (encodeVersionLength <= 0) {
            throw new ClientException(
                    DATA_MALFORMED,
                    String.format(
                            "Encode version length: '%s' is not valid, it must be greater of equal to 0",
                            encodeVersionLength
                    )
            );
        }

        if (encodeVersionLength + 1 > cipherString.length()){
            throw new ClientException(
                    DATA_MALFORMED,
                    "Length of encode version string (plus the length character) is longer than " +
                            "the CipherString itself. The data is malformed.");
        }

        if (!cipherString.substring(1, 1 + encodeVersionLength).equals(ENCODE_VERSION)) {
            throw new ClientException(
                    DATA_MALFORMED,
                    String.format(
                            "Unsupported encode version received. Encode version supported is: '%s'",
                            ENCODE_VERSION
                    )
            );
        }
    }

    /**
     * Compare digest of input message and calculated MAC digest.
     *
     * @param start    Start index of the digest in the encrypted blob array.
     * @param end      End index of the digest in the encrypted blob array.
     * @param expected The expected mac digest.
     * @throws ClientException Client if there is any mismatch.
     */
    private void assertHMac(final byte[] encryptedBlob,
                            final int start,
                            final int end,
                            final byte[] expected)
            throws ClientException {
        if (expected.length != (end - start)) { //NOPMD
            throw new ClientException(UNEXPECTED_HMAC_LENGTH);
        }

        byte result = 0;
        // It does not fail fast on the first not equal byte to protect against
        // timing attack.
        for (int i = start; i < end; i++) {
            result |= expected[i - start] ^ encryptedBlob[i];
        }

        if (result != 0) {
            throw new ClientException(HMAC_MISMATCH);
        }
    }

    /**
     * Get Secret Key to use in encryption/decryption.
     *
     * @return a SecretKey loader.
     */
    @NonNull
    public abstract AbstractSecretKeyLoader getKeyLoaderForEncryption() throws ClientException;

    /**
     * Identify the encrypted blob and return a list of potential candidate key loaders for decryption.
     *
     * @param cipherText The cipherText to decrypt.
     * @return a prioritized list of SecretKey (earlier keys is more likely to be the correct one).
     **/
    @NonNull
    abstract public List<AbstractSecretKeyLoader> getKeyLoaderForDecryption(final byte[] cipherText) throws ClientException;
}
