package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

/**
 * The UI options that developer can pass during interactive token acquisition requests.
 */
public enum MicrosoftStsPromptBehavior {

    /**
     * AcquireToken will send prompt=select_account to the authorize endpoint. Shows a list of users from which can be
     * selected for authentication.
     */
    SELECT_ACCOUNT,

    /**
     * AcquireToken will send prompt=login to the authorize endpoint.  The user will always be prompted for credentials by the service.
     */
    FORCE_LOGIN,

    /**
     * AcquireToken will send prompt=consent to the authorize endpoint.  The user will be prompted to consent even if consent was granted before.
     */
    CONSENT
}