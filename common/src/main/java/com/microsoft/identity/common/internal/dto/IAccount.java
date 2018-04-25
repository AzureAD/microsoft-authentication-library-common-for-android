package com.microsoft.identity.common.internal.dto;

public interface IAccount {

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
     * Gets the realm.
     *
     * @return The realm to get.
     */
    String getRealm();

    /**
     * Gets the authority_account_id.
     *
     * @return The authority_account_id to get.
     */
    String getAuthorityAccountId();

    /**
     * Gets the username.
     *
     * @return The username to get.
     */
    String getUsername();

    /**
     * Gets the authority_type.
     *
     * @return The authority_type to get.
     */
    String getAuthorityType();

    /**
     * Gets the guest_id.
     *
     * @return The guest_id to get.
     */
    String getGuestId();

    /**
     * Gets the first_name;
     *
     * @return The first_name to get.
     */
    String getFirstName();

    /**
     * Gets the last_name.
     *
     * @return The last_name to get.
     */
    String getLastName();

    /**
     * Gets the avatar_url.
     *
     * @return The avatar_url to get.
     */
    String getAvatarUrl();
}
