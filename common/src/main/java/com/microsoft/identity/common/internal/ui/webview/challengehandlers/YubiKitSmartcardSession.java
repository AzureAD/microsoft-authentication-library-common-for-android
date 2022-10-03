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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers;

import static com.yubico.yubikit.piv.Slot.AUTHENTICATION;
import static com.yubico.yubikit.piv.Slot.CARD_AUTH;
import static com.yubico.yubikit.piv.Slot.KEY_MANAGEMENT;
import static com.yubico.yubikit.piv.Slot.SIGNATURE;


import android.nfc.Tag;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.logging.Logger;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.piv.InvalidPinException;
import com.yubico.yubikit.piv.PivSession;
import com.yubico.yubikit.piv.Slot;
import com.yubico.yubikit.piv.jca.PivPrivateKey;
import com.yubico.yubikit.piv.jca.PivProvider;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of method abstractions requiring a PivSession to carry out smartcard certificate based authentication.
 */
public class YubiKitSmartcardSession implements ISmartcardSession {

    private static final String TAG = YubiKitSmartcardSession.class.getSimpleName();
    private final PivSession piv;
    private static final String YUBIKEY_PROVIDER = "YKPiv";
    //FILE_NOT_FOUND APDU Exception error code (27266)
    private static final short APDU_EXCEPTION_ERROR_CODE_FILE_NOT_FOUND = 0x6a82;

    /**
     * Creates instance of YubiKitSmartcardSession.
     * @param p PivSession created.
     */
    public YubiKitSmartcardSession(@NonNull final PivSession p) {
        piv = p;
    }

    /**
     * Gets a list of YubiKitCertDetails based off of certificates on the YubiKey.
     * @return a List of YubiKitCertDetails. If YubiKey doesn't contain PIV certificates, returns an empty List.
     * @throws ApduException in case of an error response from the YubiKey
     * @throws BadResponseException in case of incorrect YubiKey response
     * @throws IOException in case of connection error
     */
    @NonNull
    @Override
    public List<ICertDetails> getCertDetailsList() throws ApduException, BadResponseException, IOException {
        //Create ArrayList that contains cert details only pertinent to the cert picker.
        final List<ICertDetails> certList = new ArrayList<>();
        //We need to check all four PIV slots.
        //AUTHENTICATION (9A)
        getAndPutCertDetailsInList(AUTHENTICATION, piv, certList);
        //SIGNATURE (9C)
        getAndPutCertDetailsInList(SIGNATURE, piv, certList);
        //KEY_MANAGEMENT (9D)
        getAndPutCertDetailsInList(KEY_MANAGEMENT, piv, certList);
        //CARD_AUTH (9E)
        getAndPutCertDetailsInList(CARD_AUTH, piv, certList);
        return certList;
    }

    /**
     * Helper method that handles reading certificates off YubiKey, generating YubiKitCertDetails, and inserting into list.
     * @param slot A PIV slot from which to read the certificate.
     * @param piv A PivSession created from a SmartCardConnection that can interact with certificates located in the PIV slots on the YubiKey.
     * @param certList A List collecting the YubiKitCertDetails of the certificates found on the YubiKey.
     * @throws IOException          in case of connection error
     * @throws ApduException        in case of an error response from the YubiKey
     * @throws BadResponseException in case of incorrect YubiKey response
     */
    private void getAndPutCertDetailsInList(@NonNull final Slot slot,
                                            @NonNull final PivSession piv,
                                            @NonNull final List<ICertDetails> certList)
            throws IOException, ApduException, BadResponseException {
        final String methodTag = TAG + ":getAndPutCertDetailsInList";
        try {
            final X509Certificate cert =  piv.getCertificate(slot);
            //If there are no exceptions, add this cert to our certList.
            certList.add(new YubiKitCertDetails(cert, slot));
        } catch (final ApduException e) {
            //If sw represents a FILE_NOT_FOUND error, we should ignore
            // since this merely means the slot is empty.
            if (e.getSw() == APDU_EXCEPTION_ERROR_CODE_FILE_NOT_FOUND) {
                Logger.verbose(methodTag, slot + " slot is empty.");
            } else {
                throw e;
            }
        }
    }

    /**
     * Verifies PIN attempt provided by user.
     * @param pin Char array with pin.
     * @return true if PIN is verified; false otherwise.
     * @throws ApduException in case of an error response from the YubiKey
     * @throws IOException in case of connection error
     */
    @Override
    public boolean verifyPin(@NonNull final char[] pin) throws ApduException, IOException {
        final String methodTag = TAG + ":verifyPin";
        try {
            piv.verifyPin(pin);
            //If no InvalidPinException is thrown, PIN is validated.
            return true;
        } catch (InvalidPinException e) {
            Logger.info(methodTag, "Incorrect PIN entered.");
            return false;
        }
    }

    /**
     * Gets number of PIN attempts remaining for YubiKey.
     * @return number of PIN attempts remaining.
     * @throws ApduException in case of an error response from the YubiKey
     * @throws IOException in case of connection error
     */
    @Override
    public int getPinAttemptsRemaining() throws ApduException, IOException {
        return piv.getPinAttempts();
    }

    /**
     * Instantiates a PivPrivateKey to pass to a ClientCertRequest to proceed with certificate based authentication.
     * @param certDetails YubiKeyCertDetails of chosen certificate.
     * @param pin Char array containing verified PIN.
     * @return a PivPrivateKey.
     * @throws Exception for KeyStore, and if certDetails or PrivateKey are not of correct child type.
     */
    @NonNull
    @Override
    public PrivateKey getKeyForAuth(@NonNull final ICertDetails certDetails,
                                    @NonNull final char[] pin) throws Exception {
        final String methodTag = TAG + ":getKeyForAuth";
        if (!(certDetails instanceof YubiKitCertDetails)) {
            throw new Exception("certDetails is not of type YubiKitCertDetails.");
        }
        //Using KeyStore methods in order to generate PivPrivateKey.
        //Loading null is needed for initialization.
        final KeyStore keyStore = KeyStore.getInstance(YUBIKEY_PROVIDER, new PivProvider(piv));
        keyStore.load(null);
        final Key key = keyStore.getKey(((YubiKitCertDetails)certDetails).getSlot().getStringAlias(), pin);
        if (!(key instanceof PivPrivateKey)) {
            Logger.error(methodTag, "Private key retrieved from YKPiv keystore is not of type PivPrivateKey.", null);
            throw new Exception("Private key retrieved from YKPiv keystore is not of type PivPrivateKey.");
        }
        //PivPrivateKey implements PrivateKey. Note that the PIN is copied in pivPrivateKey.
        return (PivPrivateKey) key;
    }
}
