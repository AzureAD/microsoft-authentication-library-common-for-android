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
package com.microsoft.identity.common.java.providers.oauth2;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.util.Base64;
import com.microsoft.identity.common.java.util.StringUtil;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The client creates a code challenge derived from the code
 * verifier by using one of the following transformations.
 * <p>
 * Sophisticated attack scenarios allow the attacker to
 * observe requests (in addition to responses) to the
 * authorization endpoint.  The attacker is, however, not able to
 * act as a man in the middle. To mitigate this,
 * "code_challenge_method" value must be set either to "S256" or
 * a value defined by a cryptographically secure
 * "code_challenge_method" extension. In this implementation "S256" is used.
 * <p>
 * Example for the S256 code_challenge_method
 *
 * @see <a href="https://tools.ietf.org/html/rfc7636#page-17">RFC-7636</a>
 */

@Getter
@Accessors(prefix = "m")
public final class PkceChallenge implements Serializable {
    private static final int CODE_VERIFIER_BYTE_SIZE = 32;
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String ISO_8859_1 = "ISO_8859_1";
    private static final String CHALLENGE_SHA256 = "S256";
    private static final long serialVersionUID = 8549806628675994235L;

    /**
     * A cryptographically random string that is used to correlate the
     * authorization request to the token request.
     * <p>
     * code-verifier = 43*128unreserved
     * where...
     * unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
     * ALPHA = %x41-5A / %x61-7A
     * DIGIT = %x30-39
     */
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    private final transient String mCodeVerifier;

    /**
     * A challenge derived from the code verifier that is sent in the
     * authorization request, to be verified against later.
     */
    @SerializedName("code_challenge")
    private final String mCodeChallenge;

    @SerializedName("code_challenge_method")
    private final String mCodeChallengeMethod;

    private PkceChallenge(final String codeVerifier, final String codeChallenge) {
        mCodeVerifier = codeVerifier;
        mCodeChallenge = codeChallenge;
        mCodeChallengeMethod = CHALLENGE_SHA256;
    }

    /**
     * Creates a new instance of {@link PkceChallenge}.
     *
     * @return the newly created Challenge
     */
    public static PkceChallenge newPkceChallenge() {
        // Generate the code_verifier as a high-entropy cryptographic random String
        final String codeVerifier = generateCodeVerifier(null);

        // Create a code_challenge derived from the code_verifier
        final String codeChallenge = generateCodeVerifierChallenge(codeVerifier);

        return new PkceChallenge(codeVerifier, codeChallenge);
    }

    static String generateCodeVerifier(byte[] verifierBytes) {
        if (verifierBytes == null){
            verifierBytes = new byte[CODE_VERIFIER_BYTE_SIZE];
            new SecureRandom().nextBytes(verifierBytes);
        }
        return Base64.encodeUrlSafeString(verifierBytes);
    }

    static String generateCodeVerifierChallenge(final String verifier) {
        try {
            MessageDigest digester = MessageDigest.getInstance(DIGEST_ALGORITHM);
            digester.update(verifier.getBytes(ISO_8859_1));
            byte[] digestBytes = digester.digest();
            return Base64.encodeUrlSafeString(digestBytes);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate the code verifier challenge", e);
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(
                    "Every implementation of the Java platform is required to support ISO-8859-1."
                            + "Consult the release documentation for your implementation.", e);
        }
    }
}