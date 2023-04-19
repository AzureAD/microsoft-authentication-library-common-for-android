package com.microsoft.identity.common.java.providers.nativeauth.interactors

import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthRequestProvider
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthResponseHandler
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprContinueRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprPollCompletionRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprStartRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprSubmitRequest
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.challenge.SsprChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.cont.SsprContinueResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.pollcompletion.SsprPollCompletionResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.start.SsprStartResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.submit.SsprSubmitResult
import com.microsoft.identity.common.internal.util.getEncodedRequest
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprContinueCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitCommandParameters
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.util.ObjectMapper

class SsprInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    //region /resetpassword/start
    fun performSsprStart(
        commandParameters: SsprStartCommandParameters
    ): SsprStartResult {
        val request = createSsprStartRequest(commandParameters)
        return performSsprStart(request)
    }

    private fun createSsprStartRequest(
        commandParameters: SsprStartCommandParameters
    ): SsprStartRequest {
        return nativeAuthRequestProvider.createSsprStartRequest(
            commandParameters = commandParameters
        )
    }

    private fun performSsprStart(request: SsprStartRequest): SsprStartResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSsprStartResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion

    //region /resetpassword/challenge
    fun performSsprChallenge(
        passwordResetToken: String
    ): SsprChallengeResult {
        val request = createSsprChallengeRequest(
            passwordResetToken = passwordResetToken
        )
        return performSsprChallenge(request)
    }

    private fun createSsprChallengeRequest(
        passwordResetToken: String
    ): SsprChallengeRequest {
        return nativeAuthRequestProvider.createSsprChallengeRequest(
            passwordResetToken = passwordResetToken
        )
    }

    private fun performSsprChallenge(request: SsprChallengeRequest): SsprChallengeResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSsprChallengeResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion

    //region /resetpassword/continue
    fun performSsprContinue(
        passwordResetToken: String,
        commandParameters: SsprContinueCommandParameters
    ): SsprContinueResult {
        val request = createSsprContinueRequest(
            passwordResetToken = passwordResetToken,
            commandParameters = commandParameters
        )
        return performSsprContinue(request)
    }

    private fun createSsprContinueRequest(
        passwordResetToken: String,
        commandParameters: SsprContinueCommandParameters
    ): SsprContinueRequest {
        return nativeAuthRequestProvider.createSsprContinueRequest(
            passwordResetToken = passwordResetToken,
            commandParameters = commandParameters
        )
    }

    private fun performSsprContinue(request: SsprContinueRequest): SsprContinueResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSsprContinueResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion

    //region /resetpassword/submit
    fun performSsprSubmit(
        passwordSubmitToken: String,
        commandParameters: SsprSubmitCommandParameters
    ): SsprSubmitResult {
        val request = createSsprSubmitRequest(
            passwordSubmitToken = passwordSubmitToken,
            commandParameters = commandParameters
        )
        return performSsprSubmit(request)
    }

    private fun createSsprSubmitRequest(
        passwordSubmitToken: String,
        commandParameters: SsprSubmitCommandParameters
    ): SsprSubmitRequest {
        return nativeAuthRequestProvider.createSsprSubmitRequest(
            passwordSubmitToken = passwordSubmitToken,
            commandParameters = commandParameters
        )
    }

    private fun performSsprSubmit(request: SsprSubmitRequest): SsprSubmitResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSsprSubmitResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion

    //region /resetpassword/poll_completion
    fun performSsprPollCompletion(
        passwordResetToken: String
    ): SsprPollCompletionResult {
        val request = createSsprPollCompletionRequest(
            passwordResetToken = passwordResetToken
        )
        return performSsprPollCompletion(request)
    }

    private fun createSsprPollCompletionRequest(
        passwordResetToken: String
    ): SsprPollCompletionRequest {
        return nativeAuthRequestProvider.createSsprPollCompletionRequest(
            passwordResetToken = passwordResetToken
        )
    }

    private fun performSsprPollCompletion(request: SsprPollCompletionRequest): SsprPollCompletionResult {
        val encodedRequest: String = request.parameters.getEncodedRequest()
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSsprPollCompletionResultFromHttpResponse(
            response = response
        )
        nativeAuthResponseHandler.validateApiResult(result)
        return result
    }
    //endregion
}
