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
package com.microsoft.identity.common.internal

import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.internal.util.PackageUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.cert.X509Certificate

/**
 * Unit Tests for [BrokerValidator].
 */
@RunWith(RobolectricTestRunner::class)
class BrokerValidatorTest {

    // Extracted from debugger.
    private val mockLtwCertificate = byteArrayOf(48, -126, 3, 121, 48, -126, 2, 97, -96, 3, 2, 1, 2, 2, 4, 96, 91, 18, -41, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 48, 108, 49, 16, 48, 14, 6, 3, 85, 4, 6, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 8, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 7, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 10, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 11, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 3, 19, 7, 85, 110, 107, 110, 111, 119, 110, 48, 32, 23, 13, 50, 51, 48, 54, 48, 55, 49, 57, 49, 53, 52, 54, 90, 24, 15, 52, 55, 54, 49, 48, 53, 48, 52, 49, 57, 49, 53, 52, 54, 90, 48, 108, 49, 16, 48, 14, 6, 3, 85, 4, 6, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 8, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 7, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 10, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 11, 19, 7, 85, 110, 107, 110, 111, 119, 110, 49, 16, 48, 14, 6, 3, 85, 4, 3, 19, 7, 85, 110, 107, 110, 111, 119, 110, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0, -114, -42, 71, 18, -67, 15, 49, 106, 4, 58, 71, 99, 19, 86, -13, -108, 121, -109, -39, -46, -46, -33, 99, 28, 28, -91, 46, -63, 25, -112, -85, -71, 126, -28, -86, 10, 93, 113, 16, -78, 17, 107, 61, -23, -58, -98, -99, 39, -46, 110, 66, -123, -27, -25, -97, 91, -110, -71, 112, 39, -39, -11, 19, -37, -64, 103, -97, -118, 29, -81, 13, -91, -31, -100, -58, -26, 92, 127, -75, -69, 51, -17, -103, -20, 26, -16, -88, 90, 73, 5, -120, 18, 56, 66, -60, 120, -9, -115, -17, 62, 87, -33, 1, 77, -61, -58, 98, -90, -45, 11, 73, 71, -5, 98, 61, -101, 61, -111, 53, -79, -72, -58, -15, -51, -45, 38, 124, 78, 66, 106, -105, 115, 106, -51, -71, -118, 93, -42, -125, -87, 17, -116, -55, -30, 122, -111, -113, 61, -8, -1, 105, 102, -110, -9, 85, 8, 107, 60, 44, 87, -121, 127, 81, 43, 15, 44, -26, -80, 30, -18, 4, 17, 88, 48, -86, -83, 79, -110, 43, -49, 101, 122, -23, -36, 26, 48, 91, -50, 104, 65, 74, -84, 2, 30, -101, 111, -53, -113, -87, -114, -21, -79, -15, -44, 68, 61, 12, 125, 34, 75, -3, 89, -82, -2, 123, 29, -2, -108, 16, -96, 117, -38, -37, 124, 2, 104, 43, 108, 27, 98, 91, 103, 29, 81, -90, 55, -26, 61, -17, -76, -110, 62, 59, 116, 36, 26, -127, -94, 35, -43, 43, 73, -117, 10, -113, 113, 2, 3, 1, 0, 1, -93, 33, 48, 31, 48, 29, 6, 3, 85, 29, 14, 4, 22, 4, 20, -68, -38, -7, 32, 64, -64, 83, -41, 125, -107, 26, 48, 34, 73, 41, -92, -34, -47, -122, 105, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 3, -126, 1, 1, 0, 121, 87, 71, 55, 70, -119, 75, 49, -70, 0, -115, -60, -45, -67, -41, 15, 24, -99, 88, -87, -49, -57, -65, -100, 23, 102, -42, -118, 9, 48, 6, 109, -38, -123, -76, -63, 50, -78, -48, -70, -83, 24, -117, -43, -29, -43, -4, 127, 63, 117, -128, 67, -52, -62, -31, 35, 113, -16, 30, 23, 44, -93, 55, -34, -16, 44, 25, 13, 41, -79, -121, -8, 14, -46, 21, 2, -66, -16, -76, -98, -63, 127, -14, -110, -66, 19, 110, -26, 117, -1, -60, -62, 28, -102, 119, -84, 9, -11, 127, 87, 62, -96, -94, -20, 92, -18, 106, -55, -84, 42, -76, -31, 112, 104, -46, 65, -101, -68, -19, -12, -74, 112, -76, -28, -60, 118, -49, -91, -34, 45, 123, 108, 107, 4, -61, 74, 80, 72, 113, 53, 82, 106, 83, 57, -74, 41, 7, 51, -32, -105, 56, -70, 43, 76, 113, -91, 81, -122, 51, 2, -106, -15, -35, -45, -38, 108, -76, 24, 30, -122, 72, -76, -43, -124, -2, 28, 126, -58, -8, -6, 71, 56, 103, 9, 64, -57, 58, -101, 104, -76, -11, -128, -86, 62, -35, -117, 109, 56, 28, 26, 50, -125, -16, 69, -46, 12, 66, 101, 40, 41, 11, -38, 104, 91, 91, -49, -13, 86, -100, 55, -10, -113, 57, -117, -62, -63, -101, 75, -53, 5, -40, 81, -74, 32, 97, -44, -128, 95, -2, 63, -36, 85, -73, 22, 31, -59, 91, 71, 85, 25, 120, -8, 26, 75, 110, -38)

    @Test
    fun testValidationSucceed(){
        val validator = BrokerValidator(
            allowedBrokerApps = setOf(BrokerData.debugMockLtw),
            getSigningCertificateForApp = {
                getMockBrokerRawCert()
            },
            validateSigningCertificate = BrokerValidator.Companion::validateSigningCertificate
        )

        Assert.assertTrue(validator.isValidBrokerPackage(BrokerData.debugMockLtw.packageName))
        Assert.assertFalse(validator.isValidBrokerPackage(BrokerData.prodCompanyPortal.packageName))
    }

    @Test
    fun testValidationFailed_NotInAllowedBrokerAppList(){
        val validator = BrokerValidator(
            allowedBrokerApps = setOf(
                BrokerData.debugMockAuthApp,
                BrokerData.debugMockCp),
            getSigningCertificateForApp = {
                getMockBrokerRawCert()
            },
            validateSigningCertificate = BrokerValidator.Companion::validateSigningCertificate
        )

        Assert.assertFalse(validator.isValidBrokerPackage(BrokerData.debugMockLtw.packageName))
    }

    @Test
    fun testValidationFailed_CannotGetSigningCertificate(){
        val validator = BrokerValidator(
            allowedBrokerApps = setOf(BrokerData.debugMockLtw),
            getSigningCertificateForApp = {
                throw RuntimeException("Fail to get cert for some reason")
            },
            validateSigningCertificate = BrokerValidator.Companion::validateSigningCertificate
        )

        Assert.assertFalse(validator.isValidBrokerPackage(BrokerData.debugMockLtw.packageName))
    }

    @Test
    fun testSigningCertificationValidationFailed(){
        val validator = BrokerValidator(
            allowedBrokerApps = setOf(BrokerData.debugMockLtw),
            getSigningCertificateForApp = {
                getMockBrokerRawCert()
            },
            validateSigningCertificate = { _, _ ->
               throw RuntimeException("Fail to validate for some reason")
            }
        )

        Assert.assertFalse(validator.isValidBrokerPackage(BrokerData.debugMockLtw.packageName))
    }

    private fun getMockBrokerRawCert(): List<X509Certificate>{
        return listOf(PackageUtils.createCertificateFromByteArray(mockLtwCertificate))
    }
}