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
package com.microsoft.identity.common.java.constants

/**
 * Constants for CBOR (Concise Binary Object Representation) parsing.
 *
 * CBOR is a binary data serialization format defined by RFC 7049 / RFC 8949.
 * It is used, for example, to encode the `attestationObject` in WebAuthn
 * registration responses.
 */
object CborConstants {

    /**
     * Bitmask used to convert a signed Kotlin/Java [Byte] to its unsigned integer representation
     * (0–255) when parsing raw binary data.
     *
     * Because [Byte] is signed (-128..127), calling [Byte.toInt] on values ≥ 0x80 produces a
     * negative number due to sign extension (e.g. `0x80.toByte().toInt() == -128`, not 128).
     * ANDing with [BYTE_UNSIGNED_MASK] clears the upper 24 bits and yields the correct unsigned value.
     */
    const val BYTE_UNSIGNED_MASK = 0xFF

    /**
     * Bitmask to extract the major type from a CBOR initial byte.
     *
     * In CBOR encoding, the upper 3 bits of the initial byte encode the major type.
     * Shifting the initial byte right by 5 and ANDing with this mask yields a value 0–7
     * identifying the type (e.g. 0 = unsigned int, 2 = byte string, 3 = text string).
     */
    const val MAJOR_TYPE_MASK = 0x07

    /**
     * Mask for the additional info field in the lower 5 bits of a CBOR initial byte.
     *
     * Values 0–23 encode the length directly; values 24, 25, and 26 signal that the
     * length follows in 1, 2, or 4 subsequent bytes respectively.
     */
    const val ADDITIONAL_INFO_MASK = 0x1F

    /**
     * CBOR major type value for a byte string (major type 2).
     *
     * Used to confirm a CBOR item is a byte string before reading its length.
     * After extracting the major type with [MAJOR_TYPE_MASK], compare against this value.
     */
    const val MAJOR_TYPE_BYTE_STRING = 2

    /**
     * CBOR additional info value indicating the length follows in 1 byte (uint8).
     */
    const val ADDITIONAL_INFO_ONE_BYTE_LENGTH = 24

    /**
     * CBOR additional info value indicating the length follows in 2 bytes (uint16, big-endian).
     */
    const val ADDITIONAL_INFO_TWO_BYTE_LENGTH = 25

    /**
     * CBOR additional info value indicating the length follows in 4 bytes (uint32, big-endian).
     */
    const val ADDITIONAL_INFO_FOUR_BYTE_LENGTH = 26
}

