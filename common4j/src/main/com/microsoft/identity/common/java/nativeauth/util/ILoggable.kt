package com.microsoft.identity.common.java.nativeauth.util

interface ILoggable {
//    /**
//     * This method produces a PII-safe String (PII = Personally identifiable information).
//     * If mayContainPii is false, the String value that is returned will not contain PII. If
//     * mayContainPii is true, the String value that is returned that may contain PII (the value of
//     * containsPii() will determine if the String actually does contain PII).
//     *
//     * @param mayContainPii indicates whether the String that is returned is allowed to contain PII.
//     */
//    fun toSafeString(mayContainPii: Boolean): String

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
    fun containsPii(): Boolean

    /**
     * While we can't enforce this method to be overwritten, the intention is that every class that
     * implements this interface, also implements this method. This method will return a PII-safe
     * String, i.e. any PII is not included.
     */
    override fun toString(): String
}
