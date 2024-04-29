package com.microsoft.identity.common.java.nativeauth.util

interface ILoggable {
    /**
     * This method produces a String that may contain PII (PII = Personally identifiable information).
     * The value of containsPii() will indicate whether the value actually contains PII.
     */
    fun toUnsanitizedString(): String

    /**
     * This method indicates whether the implementing class contains data fields that are considered
     * PII (Personally identifiable information). If this method returns true, then the value of
     * toSafeString(true) will return a String that contains PII.
     */
    fun containsPii(): Boolean = toString() != toUnsanitizedString()

    /**
     * While we can't enforce this method to be overwritten, the intention is that every class that
     * implements this interface, also implements this method. This method will return a PII-safe
     * String, i.e. any PII is not included.
     */
    override fun toString(): String
}
