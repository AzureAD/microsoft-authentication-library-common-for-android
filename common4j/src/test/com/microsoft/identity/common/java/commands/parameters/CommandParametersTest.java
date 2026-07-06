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
package com.microsoft.identity.common.java.commands.parameters;

import static org.mockito.Mockito.mock;

import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link CommandParameters#getApplicationIdentifier()} and its inverse
 * {@link CommandParameters#getPackageNameFromApplicationIdentifier(String)}.
 * <p>
 * These tests deliberately lock the {@code <callerPackageName>/<callerSignature>} wire format so
 * that any change to {@link CommandParameters#APPLICATION_IDENTIFIER_FORMAT} (or to the way the
 * package is parsed back out) is caught here — several callers (e.g. the broker browser-redirect
 * Intune exemption) depend on being able to recover the caller package from the identifier.
 */
public class CommandParametersTest {

    private static final String PACKAGE_NAME = "com.microsoft.intune";

    /**
     * A real base64 SHA-512 signing signature. Note it contains a '/', which is exactly why the
     * package parser must split on the first separator only.
     */
    private static final String SIGNATURE_WITH_SLASH =
            "jPpMoaNvcxSLMX4yG4C3Gf86rtTqh33SqpuRKg4WOP+MnnpA52zZgvKLW76U4Cqqf68iaBk9W7k/jhciiSAtgQ==";

    private static CommandParameters commandParametersWith(final String packageName,
                                                           final String signature) {
        return CommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .callerPackageName(packageName)
                .callerSignature(signature)
                .build();
    }

    @Test
    public void getApplicationIdentifier_isPackageNameSlashSignature() {
        final CommandParameters params = commandParametersWith(PACKAGE_NAME, SIGNATURE_WITH_SLASH);
        // Locks the exact wire format. If APPLICATION_IDENTIFIER_FORMAT changes, this breaks.
        Assert.assertEquals(PACKAGE_NAME + "/" + SIGNATURE_WITH_SLASH, params.getApplicationIdentifier());
    }

    @Test
    public void getPackageName_roundTripsWithGetApplicationIdentifier() {
        final CommandParameters params = commandParametersWith(PACKAGE_NAME, SIGNATURE_WITH_SLASH);
        // Guarantees the parser stays in sync with the producer even if the format changes: whatever
        // getApplicationIdentifier() emits, getPackageNameFromApplicationIdentifier() must recover
        // the caller package from it.
        Assert.assertEquals(
                PACKAGE_NAME,
                CommandParameters.getPackageNameFromApplicationIdentifier(params.getApplicationIdentifier()));
    }

    @Test
    public void getPackageName_whenSignatureContainsSlash_returnsFullPackageOnly() {
        final String applicationIdentifier = PACKAGE_NAME + "/" + SIGNATURE_WITH_SLASH;
        Assert.assertEquals(
                PACKAGE_NAME,
                CommandParameters.getPackageNameFromApplicationIdentifier(applicationIdentifier));
    }

    @Test
    public void getPackageName_whenNoSeparator_returnsWholeString() {
        Assert.assertEquals(
                PACKAGE_NAME,
                CommandParameters.getPackageNameFromApplicationIdentifier(PACKAGE_NAME));
    }

    @Test
    public void getPackageName_whenEmptySignature_returnsPackage() {
        // String.format("%s/%s", pkg, "") -> "pkg/"
        Assert.assertEquals(
                PACKAGE_NAME,
                CommandParameters.getPackageNameFromApplicationIdentifier(PACKAGE_NAME + "/"));
    }

    @Test
    public void getPackageName_whenNull_returnsNull() {
        Assert.assertNull(CommandParameters.getPackageNameFromApplicationIdentifier(null));
    }
}
