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
package com.microsoft.identity.common.java.crypto;

import com.microsoft.identity.common.java.exception.ClientException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.util.Date;

/**
 * Mediate access to a specific instance of a piece of material stored in a KeyStore.  This
 * interface provides generic access to hardware storage for a given key entry, provides the
 * metadata that can be retrieved from the underlying storage mechanism of whatever variety.
 *
 * @param <K> the type of KeyStore entry being managed.
 */
public interface IKeyStoreKeyManager<K extends KeyStore.Entry> {
    /**
     * @return true if the key that is being managed exists.
     */
    boolean exists();

    /**
     * Given a particular key thumbprint, determine whether it matches the one specified
     * produced by this key.  The thumprint in question is usually a well-known value encrypted
     * with the key, and may include certain parameters with which the cipher is initialized.
     *
     * @param thumbprint A key thumprint.
     * @return True if this keys thumprint maches the one provided.
     */
    boolean hasThumbprint(byte[] thumbprint);

    /**
     * @return the alias of the key.  Storage mechanisms can use this alias to provide access
     * to other users.
     */
    String getKeyAlias();

    /**
     * @return the date on which the key was created.
     * @throws ClientException
     */
    Date getCreationDate() throws ClientException;

    /**
     * Remove this key from the storage mechanism containing it.  After this method is called,
     * other functionality on this object may fail.
     *
     * @return true if the removal was successful.
     */
    boolean clear();

    /**
     * Retrieve a reference to the material stored in this location.
     *
     * @return the keyStore entry for this key or null if it cannot be located.
     * @throws UnrecoverableEntryException if the key cannot be read from the storage mechanism.
     * @throws NoSuchAlgorithmException    if the algorithm required by the key cannot be supported.
     * @throws KeyStoreException           if the underlying KeyStore has not been initialized/
     */
    K getEntry() throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException;

    /**
     * Import a key.
     *
     * @param jwk       the jwk to import.
     * @param algorithm the algortihm in use.
     * @throws ClientException if something goes wrong.
     */
    void importKey(byte[] jwk, String algorithm) throws ClientException;

    /**
     * Store an asymmetric key into the key store.
     *
     * @param privateKey the private key of the asymmetric key
     * @param certChain  the cert chain of the asymmetric key
     * @throws KeyStoreException if something goes wrong with key store
     * @throws ClientException   if something goes wrong while storing asymmetric key
     */
    void storeAsymmetricKey(PrivateKey privateKey, Certificate[] certChain) throws KeyStoreException, ClientException;

    /**
     * @return a byte array key thumpbrint.
     */
    byte[] getThumbprint() throws ClientException;

    /**
     * @return a certificate chain associated with this key, or null if no chain is tied to it.
     */
    Certificate[] getCertificateChain() throws ClientException;

    /**
     * Gets the {@link SecureHardwareState} of this key.
     *
     * @return The SecureHardwareState.
     * @throws ClientException If the underlying key material cannot be inspected.
     */
    SecureHardwareState getSecureHardwareState() throws ClientException;
}
