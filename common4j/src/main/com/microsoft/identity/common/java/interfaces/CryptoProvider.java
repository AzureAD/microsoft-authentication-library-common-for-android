package com.microsoft.identity.common.java.interfaces;

import com.microsoft.identity.common.java.crypto.CryptoSuite;

import java.security.NoSuchAlgorithmException;

public interface CryptoProvider {
    CryptoSuite getCryptoSuite(String suiteName) throws NoSuchAlgorithmException;
}
