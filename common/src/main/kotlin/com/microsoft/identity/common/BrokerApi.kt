package com.microsoft.identity.common

import com.microsoft.identity.common.adal.internal.AuthenticationConstants

/**
 * Tie the API paths and codes into a single object structure to stop us from having to keep
 * them in sync.  This is designed to pull all the parts of the API definition into a single
 * place, so that we only need to make updates in one location, and it's clearer what we need
 * to do when adding new APIs.
 *
 * @param path           The content provider path that the API exists behind.
 * @param brokerVersion  The broker-host-to-broker protocol version that the API requires.
 * @param msalVersion    The msal-to-broker version that the API requires.
 */
enum class BrokerApi (val path: String,
                      val brokerVersion: String? = null,
                      val msalVersion: String? = null) {
    MSAL_HELLO(
        AuthenticationConstants.BrokerContentProvider.MSAL_HELLO_PATH,
        msalVersion = AuthenticationConstants.VERSION_1
    ),
    ACQUIRE_TOKEN_INTERACTIVE(
        AuthenticationConstants.BrokerContentProvider.MSAL_ACQUIRE_TOKEN_INTERACTIVE_PATH,
        msalVersion = AuthenticationConstants.VERSION_3
    ),
    ACQUIRE_TOKEN_SILENT(
        AuthenticationConstants.BrokerContentProvider.MSAL_ACQUIRE_TOKEN_SILENT_PATH,
        msalVersion = AuthenticationConstants.VERSION_3
    ),
    GET_ACCOUNTS(
        AuthenticationConstants.BrokerContentProvider.MSAL_GET_ACCOUNTS_PATH,
        msalVersion = AuthenticationConstants.VERSION_3
    ),
    REMOVE_ACCOUNT(
        AuthenticationConstants.BrokerContentProvider.MSAL_REMOVE_ACCOUNT_PATH,
        msalVersion = AuthenticationConstants.VERSION_3
    ),
    GET_CURRENT_ACCOUNT_SHARED_DEVICE(
        AuthenticationConstants.BrokerContentProvider.MSAL_GET_CURRENT_ACCOUNT_SHARED_DEVICE_PATH,
        msalVersion = AuthenticationConstants.VERSION_3
    ),
    GET_DEVICE_MODE(
        AuthenticationConstants.BrokerContentProvider.MSAL_GET_DEVICE_MODE_PATH,
        msalVersion = AuthenticationConstants.VERSION_3
    ),
    SIGN_OUT_FROM_SHARED_DEVICE(
        AuthenticationConstants.BrokerContentProvider.MSAL_SIGN_OUT_FROM_SHARED_DEVICE_PATH,
        msalVersion = AuthenticationConstants.VERSION_3
    ),
    GENERATE_SHR(
        AuthenticationConstants.BrokerContentProvider.GENERATE_SHR_PATH,
        msalVersion = AuthenticationConstants.VERSION_6
    ),
    BROKER_HELLO(
        AuthenticationConstants.BrokerContentProvider.BROKER_API_HELLO_PATH,
        brokerVersion = AuthenticationConstants.VERSION_1
    ),
    BROKER_GET_ACCOUNTS(
        AuthenticationConstants.BrokerContentProvider.BROKER_API_GET_BROKER_ACCOUNTS_PATH,
        brokerVersion = AuthenticationConstants.VERSION_1
    ),
    BROKER_REMOVE_ACCOUNT(
        AuthenticationConstants.BrokerContentProvider.BROKER_API_REMOVE_BROKER_ACCOUNT_PATH,
        brokerVersion = AuthenticationConstants.VERSION_1
    ),
    BROKER_UPDATE_BRT(
        AuthenticationConstants.BrokerContentProvider.BROKER_API_UPDATE_BRT_PATH,
        brokerVersion = AuthenticationConstants.VERSION_1
    ),
    BROKER_SET_FLIGHTS(
        AuthenticationConstants.BrokerContentProvider.BROKER_API_SET_FLIGHTS_PATH,
        brokerVersion = AuthenticationConstants.VERSION_3
    ),
    BROKER_GET_FLIGHTS(
        AuthenticationConstants.BrokerContentProvider.BROKER_API_GET_FLIGHTS_PATH,
        brokerVersion = AuthenticationConstants.VERSION_3
    ),
    GET_SSO_TOKEN(
        AuthenticationConstants.BrokerContentProvider.GET_SSO_TOKEN_PATH,
        msalVersion = AuthenticationConstants.VERSION_7
    ),
    UNKNOWN(""),
    DEVICE_REGISTRATION_PROTOCOLS(
        AuthenticationConstants.BrokerContentProvider.DEVICE_REGISTRATION_PROTOCOLS_PATH
    ),
    BROKER_UPLOAD_LOGS(
        AuthenticationConstants.BrokerContentProvider.DEVICE_REGISTRATION_PROTOCOLS_PATH,
        brokerVersion = AuthenticationConstants.VERSION_4
    ),
    BROKER_METADATA_RETRIEVAL(
        AuthenticationConstants.BrokerContentProvider.BROKER_METADATA_RETRIEVAL_PATH
    ),
    BROKER_DISCOVERY(
        AuthenticationConstants.BrokerContentProvider.BROKER_DISCOVERY_PATH
    ),
    BROKER_SET_ACTIVE_BROKER(
        AuthenticationConstants.BrokerContentProvider.BROKER_SET_ACTIVE_BROKER_PATH
    ),
    BROKER_EMPTY_REQUEST_TEST(
        AuthenticationConstants.BrokerContentProvider.BROKER_EMPTY_REQUEST_TEST_PATH
    );
}
