//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.internal.testutils.nativeauth.api

import com.google.gson.reflect.TypeToken
import com.microsoft.identity.common.nativeauth.ApiConstants
import com.microsoft.identity.internal.test.labapi.ApiClient
import com.microsoft.identity.internal.test.labapi.ApiException
import com.microsoft.identity.internal.test.labapi.Pair
import com.microsoft.identity.internal.testutils.nativeauth.api.models.EmailContent
import com.microsoft.identity.internal.testutils.nativeauth.api.models.InboxContent
import com.squareup.okhttp.Call
import java.lang.IllegalStateException

/**
 *
 */
class TemporaryEmailService {

    private val api = TemporaryEmailApi()

    fun generateRandomEmailAddress(): String {
        val generatedUsers = api.generateRandomEmailAddress()
        if (generatedUsers.isEmpty()) {
            throw ApiException("No generated users returned from TemporaryEmailApi")
        }
        val user = generatedUsers[0]
        if (user.isNullOrBlank()) {
            throw ApiException("No valid user returned from TemporaryEmailApi")
        }
        return user
    }

    /**
     * Retrieve mailbox content, and OTP from latest email.
     * To cater for email send and arrival delays, this is retried several times.
     */
    fun retrieveCodeFromInbox(emailAddress: String): String {
        var retrievedMailbox = api.retrieveMailbox(emailAddress)

        var mailBoxEmpty = retrievedMailbox.isEmpty()
        var count = 0
        while (mailBoxEmpty && count < 3) {
            // Wait 3 seconds
            Thread.sleep(3000)

            // Attempt to retrieve mailbox again.
            retrievedMailbox = api.retrieveMailbox(emailAddress)
            mailBoxEmpty = retrievedMailbox.isEmpty()

            // Max 3 retries
            count++
        }
        // After the retries we still weren't able to retrieve the inbox, so fail and restart the test.
        if (mailBoxEmpty) {
            throw IllegalStateException("Unable to fetch inbox for user $emailAddress")
        }

        // TODO in case of multiple emails, sort by latest first
        val emailId = retrievedMailbox[0].id
        val emailContent = api.retrieveEmail(emailAddress, emailId)
        val otpValue = retrieveOtpFromEmailBody(emailContent.textBody)

        return otpValue
    }

    private fun retrieveOtpFromEmailBody(emailBody: String): String {
        val otpRegex = "Account verification code:\n(?<otc>[0-9]*)\n".toRegex()
        val match = otpRegex.find(emailBody)
        val otp = match!!.groups["otc"]!!.value
        return otp
    }

    class TemporaryEmailApi {
        companion object {
            const val ACTION_GENERATE_RANDOM_MAILBOX = "genRandomMailbox"
            const val ACTION_GET_MESSAGES = "getMessages"
            const val ACTION_READ_MESSAGE = "readMessage"
        }

        private val apiClient = ApiClient(ApiConstants.TemporaryMailService.BASE_URL)

        fun generateRandomEmailAddress(): List<String> {
            val queryParam = ArrayList(createActionQueryParam(ACTION_GENERATE_RANDOM_MAILBOX))

            val apiCall = createCall(queryParam)

            val localVarReturnType = TypeToken.getParameterized(
                List::class.java,
                String::class.java
            ).type

            val apiResponse = apiClient.execute<List<String>>(apiCall, localVarReturnType)

            return apiResponse.data
        }

        fun retrieveMailbox(userEmail: String): List<InboxContent> {
            val userLogin = userEmail.split("@")[0]
            val userDomain = userEmail.split("@")[1]

            val queryParam = mutableListOf<Pair>()
            queryParam.addAll(
                createActionQueryParam(ACTION_GET_MESSAGES)
            )
            queryParam.addAll(
                createLoginQueryParam(userLogin)
            )
            queryParam.addAll(
                createDomainQueryParam(userDomain)
            )

            val apiCall = createCall(queryParam)

            val localVarReturnType = TypeToken.getParameterized(
                List::class.java,
                InboxContent::class.java
            ).type


            val apiResponse = apiClient.execute<List<InboxContent>>(apiCall, localVarReturnType)

            return apiResponse.data
        }

        fun retrieveEmail(userEmail: String, emailId: String): EmailContent {
            val userLogin = userEmail.split("@")[0]
            val userDomain = userEmail.split("@")[1]

            val queryParam = mutableListOf<Pair>()
            queryParam.addAll(
                createActionQueryParam(ACTION_READ_MESSAGE)
            )
            queryParam.addAll(
                createLoginQueryParam(userLogin)
            )
            queryParam.addAll(
                createDomainQueryParam(userDomain)
            )
            queryParam.addAll(
                createIdQueryParam(emailId)
            )

            val apiCall = createCall(queryParam)

            val localVarReturnType = TypeToken.get(EmailContent::class.java).type

            val apiResponse = apiClient.execute<EmailContent>(apiCall, localVarReturnType)

            return apiResponse.data
        }

        private fun createActionQueryParam(value: String): List<Pair> {
            return apiClient.parameterToPair("action", value)
        }

        private fun createLoginQueryParam(value: String): List<Pair> {
            return apiClient.parameterToPair("login", value)
        }

        private fun createDomainQueryParam(value: String): List<Pair> {
            return apiClient.parameterToPair("domain", value)
        }

        private fun createIdQueryParam(value: String): List<Pair> {
            return apiClient.parameterToPair("id", value)
        }

        private fun createCall(queryParam: List<Pair>): Call {
            return apiClient.buildCall(
                "",
                "GET",
                queryParam,
                null,
                null,
                mapOf(),
                null,
                arrayOf<String>(),
                null
            )
        }
    }
}

