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
package com.microsoft.identity.common.internal.fido

import com.microsoft.identity.common.internal.fido.FidoChallengeField.Companion.throwIfInvalidOptionalListParameter
import com.microsoft.identity.common.internal.fido.FidoChallengeField.Companion.throwIfInvalidProtocolVersion
import com.microsoft.identity.common.internal.fido.FidoChallengeField.Companion.throwIfInvalidRequiredParameter
import com.microsoft.identity.common.internal.fido.FidoChallengeField.Companion.throwIfInvalidSubmitUrl
import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert
import org.junit.Test

class FidoChallengeFieldTest {
    //Challenge parameters
    val challengeStr = "T1xCsnxM2DNL2KdK5CLa6fMhD7OBqho6syzInk_n-Uo"
    val version = "1.0"
    val submitUrl = "https://submiturl"
    val context = "contextValue flowToken"
    val authority = "urn:http-auth:PassKey"

    //Challenge auth parameters
    val allowCredentialsTwoUsers = listOf("user1","user2")
    val keyTypesString = "passkey"

    @Test
    fun testThrowIfInvalidRequiredParameter_ExpectedFieldAndValue() {
        Assert.assertEquals(
            challengeStr,
            throwIfInvalidRequiredParameter(
                FidoRequestField.CHALLENGE,
                challengeStr
            )
        )
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidRequiredParameter_MissingField() {
        throwIfInvalidRequiredParameter(FidoRequestField.CHALLENGE, null)
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidRequiredParameter_EmptyValue() {
        throwIfInvalidRequiredParameter(FidoRequestField.CHALLENGE, "")
    }

    @Test
    fun testThrowIfInvalidSubmitUrl_ExpectedFieldAndValue() {
        throwIfInvalidSubmitUrl(FidoRequestField.SUBMIT_URL, submitUrl)
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidSubmitUrl_MissingField() {
        throwIfInvalidSubmitUrl(FidoRequestField.SUBMIT_URL, null)
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidSubmitUrl_EmptyValue() {
        throwIfInvalidSubmitUrl(FidoRequestField.SUBMIT_URL, "")
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidSubmitUrl_MalformedUrl() {
        throwIfInvalidSubmitUrl(FidoRequestField.SUBMIT_URL,"url")
    }

    @Test
    fun testThrowIfInvalidProtocolVersion_ExpectedFieldAndValue() {
        throwIfInvalidProtocolVersion(FidoRequestField.VERSION, version)
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidProtocolVersion_MissingField() {
        throwIfInvalidProtocolVersion(FidoRequestField.VERSION,null)
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidProtocolVersion_EmptyValue() {
        throwIfInvalidProtocolVersion(FidoRequestField.VERSION,"")
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidProtocolVersion_UnsupportedVersion() {
        throwIfInvalidProtocolVersion(FidoRequestField.VERSION,"2.0")
    }

    @Test
    fun testThrowIfInvalidOptionalListParameter_ExpectedFieldAndValue() {
        Assert.assertEquals(
            allowCredentialsTwoUsers,
            throwIfInvalidOptionalListParameter(
                FidoRequestField.ALLOWED_CREDENTIALS,
                allowCredentialsTwoUsers
            )
        )
    }

    @Test
    fun testThrowIfInvalidOptionalListParameter_MissingField() {
        Assert.assertNull(
            throwIfInvalidOptionalListParameter(
                FidoRequestField.ALLOWED_CREDENTIALS,
                null
            )
        )
    }

    @Test(expected = ClientException::class)
    fun testThrowIfInvalidOptionalListParameter_EmptyValue() {
        throwIfInvalidOptionalListParameter(FidoRequestField.ALLOWED_CREDENTIALS, emptyList())
    }
}
