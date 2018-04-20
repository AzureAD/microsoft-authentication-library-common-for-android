package com.microsoft.identity.common.internal.dto;

/**
 * Interface for schema-necessary fields for RefreshTokens.
 */
public interface IRefreshToken {

    /**
     * Gets the unique_user_id.
     *
     * @return The unique_user_id to get.
     */
    String getUniqueUserId();

    /**
     * Gets the environment.
     *
     * @return The environment to get.
     */
    String getEnvironment();

    /**
     * Gets the clientId.
     *
     * @return The clientId to get.
     */
    String getClientId();

    /**
     * Gets the secret.
     *
     * @return The secret to get.
     */
    String getSecret();

    /**
     * Gets the target.
     *
     * @return The target to get.
     */
    String getTarget();

    /**
     * Gets the expires_on.
     *
     * @return The expires_on to get.
     */
    String getExpiresOn();

    /**
     * Gets the family_id.
     *
     * @return The family_id to get.
     */
    String getFamilyId();

}
