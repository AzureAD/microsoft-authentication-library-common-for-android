package com.microsoft.identity.common.java.opentelemetry

/**
 * Assists FIDO-associated classes with telemetry-related tasks.
 */
interface IFidoTelemetryHelper {
    /**
     * Sets attribute indicating the type of FIDO challenge received from the server.
     * @param challengeName name of the FidoChallenge class.
     */
    fun setFidoChallenge(challengeName: String)

    /**
     * Sets attribute indicating the type of FIDO challenge handler handling the current FIDO operation.
     * @param challengeHandlerName name of the FidoChallengeHandler class.
     */
    fun setFidoChallengeHandler(challengeHandlerName: String)

    /**
     * Indicates on the Span that the FIDO operation was successful and then ends current Span.
     */
    fun setResultSuccess()

    /**
     * Indicates on the Span that the FIDO operation failed and then ends current Span.
     * This method should mainly be used for cases without an exception,
     * such as user cancellation.
     * @param message descriptive cause of failure message.
     */
    fun setResultFailure(message: String)

    /**
     * Indicates on the Span that the FIDO operation failed and then ends current Span.
     * @param exception exception thrown upon error.
     */
    fun setResultFailure(exception: Exception)
}