package com.microsoft.identity.common.java.nativeauth.commands.parameters;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuppressFBWarnings("EI_EXPOSE_REP2")   //Suppresses spotbugs warning on the builder class
@SuperBuilder(toBuilder = true)
public class JITChallengeAuthMethodCommandParameters extends BaseNativeAuthCommandParameters {
    private static final String TAG = JITChallengeAuthMethodCommandParameters.class.getSimpleName();

    /**
     * email/phone to contact to register a new strong authentication method
     */
    @NonNull
    public final String verificationContact;

    /**
     * Auth method challenge type (oob, email, etc.)
     */
    @NonNull
    public final String authMethodChallengeType;

    /**
     * The channel to send the challenge on. (email, voice, sms, etc.)
     */
    @NonNull
    public final String challengeChannel;

    /**
     * The continuation token obtained from the previous endpoint.
     */
    @NonNull
    public final String continuationToken;

    @Override
    public String toUnsanitizedString() {
        return "JITChallengeAuthMethodCommandParameters(authority=" + authority + ", challengeType=" + challengeType + ")";
    }

    @Override
    public boolean containsPii() {
        return !toString().equals(toUnsanitizedString());
    }

    @Override
    public String toString() {
        return toUnsanitizedString();
    }
}
