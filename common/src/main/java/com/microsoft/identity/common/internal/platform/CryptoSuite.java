package com.microsoft.identity.common.internal.platform;

import java.security.Key;
import java.security.KeyStore;

/**
 * Interface for cryptoSuite definitions.  Designed to span the Cipher enum in use in DevicePopManager
 * to allow for inclusion of symmetric cipher definitions and include the name of a MAC algorithm.
 */
public interface CryptoSuite {
    /**
     * @return the name of the cipher used for this cryto suite.  Should be suitable for use in Cipher.getInstance();
     */
    String cipherName();

    /**
     * @return the name of the MAC used for this cryto suite.  Should be suitable for use in Mac.getInstance();
     */
    String macName();

    /**
     * @return true if this suite uses an asymmetric key.
     */
    boolean isAsymmetric();

    /**
     * @return the class of entry that is used by the a KeyStore to store this credential.
     */
    Class<? extends KeyStore.Entry> keyClass();

    /**
     * @return the key size for this instance.
     */
    int keySize();
}
