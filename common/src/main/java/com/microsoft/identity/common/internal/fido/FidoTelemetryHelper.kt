package com.microsoft.identity.common.internal.fido

import com.microsoft.identity.common.java.opentelemetry.IFidoTelemetryHelper

class FidoTelemetryHelper : IFidoTelemetryHelper {
    override fun setFidoChallenge(challengeName: String) {
        TODO("Not yet implemented")
    }

    override fun setFidoChallengeHandler(challengeHandlerName: String) {
        TODO("Not yet implemented")
    }

    override fun setResultSuccess() {
        TODO("Not yet implemented")
    }

    override fun setResultFailure(message: String) {
        TODO("Not yet implemented")
    }

    override fun setResultFailure(exception: Exception) {
        TODO("Not yet implemented")
    }
}