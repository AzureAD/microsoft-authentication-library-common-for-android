package com.microsoft.identity.common.nativeauth.internal.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASubmitChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Tests for [CommandUtil].
 */
@RunWith(RobolectricTestRunner::class)
class CommandUtilTest {

    private lateinit var platformComponents: IPlatformComponents
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        platformComponents = AndroidPlatformComponentsFactory.createFromContext(
            context
        )
    }

    @Test
    fun testCreateSignInSubmitPasswordCommandParameters_containsCorrectInfo() {
        val correlationId = UUID.randomUUID().toString()
        val continuationToken = "continuation"
        val signInStartParams = SignInStartCommandParameters.builder()
            .password("test".toCharArray())
            .claimsRequestJson("claimsRequestJson")
            .clientId("clientId")
            .challengeType(arrayListOf("OOB"))
            .redirectUri("redirectUri")
            .username("username")
            .platformComponents(platformComponents)
            .build()
        val submitPasswordParams = CommandUtil.createSignInSubmitPasswordCommandParameters(signInStartParams, correlationId, continuationToken)

        assert(submitPasswordParams.getPassword().contentEquals(signInStartParams.getPassword()))
        assert(submitPasswordParams.getContinuationToken().contentEquals(continuationToken))
        assert(submitPasswordParams.correlationId?.contentEquals(correlationId) == true)
        assert(submitPasswordParams.getClaimsRequestJson().contentEquals(signInStartParams.getClaimsRequestJson()))
        assert(submitPasswordParams.clientId?.contentEquals(signInStartParams.clientId) == true)
        assert(submitPasswordParams.getChallengeType()?.equals(signInStartParams.getChallengeType()) == true)
        assert(submitPasswordParams.redirectUri?.equals(signInStartParams.redirectUri) == true)
    }

    @Test
    fun testCreateSignInSubmitCodeCommandParameters_containsCorrectInfo() {
        val mfaSubmitChallengeParams = MFASubmitChallengeCommandParameters.builder()
            .claimsRequestJson("claimsRequestJson")
            .clientId("clientId")
            .challengeType(arrayListOf("OOB"))
            .redirectUri("redirectUri")
            .platformComponents(platformComponents)
            .challenge("123456")
            .continuationToken("continuationToken")
            .correlationId(UUID.randomUUID().toString())
            .build()
        val submitCodeParams = CommandUtil.createSignInSubmitCodeCommandParameters(mfaSubmitChallengeParams)

        assert(submitCodeParams.getContinuationToken().contentEquals(mfaSubmitChallengeParams.getContinuationToken()))
        assert(submitCodeParams.correlationId?.contentEquals(mfaSubmitChallengeParams.correlationId) == true)
        assert(submitCodeParams.getClaimsRequestJson().contentEquals(mfaSubmitChallengeParams.getClaimsRequestJson()))
        assert(submitCodeParams.clientId?.contentEquals(mfaSubmitChallengeParams.clientId) == true)
        assert(submitCodeParams.getChallengeType()?.equals(mfaSubmitChallengeParams.getChallengeType()) == true)
        assert(submitCodeParams.getCode() == mfaSubmitChallengeParams.getChallenge())
        assert(submitCodeParams.redirectUri?.equals(mfaSubmitChallengeParams.redirectUri) == true)
    }
}