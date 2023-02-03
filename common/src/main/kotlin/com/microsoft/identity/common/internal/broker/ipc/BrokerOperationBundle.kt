
package com.microsoft.identity.common.internal.broker.ipc

import android.os.Bundle
import com.microsoft.identity.common.BrokerApi
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerAccountManagerOperation
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.logging.Logger

class BrokerOperationBundle (val operation: Operation,
                             val targetBrokerAppPackageName: String,
                             val bundle: Bundle?){

    private val TAG = BrokerOperationBundle::class.java.name

    enum class Operation(
        val contentApi: BrokerApi?,
        val accountManagerOperation: String?
    ) {
         MSAL_HELLO(
             BrokerApi.MSAL_HELLO,
             BrokerAccountManagerOperation.HELLO
        ),
        MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST(
            BrokerApi.ACQUIRE_TOKEN_INTERACTIVE,
            BrokerAccountManagerOperation.GET_INTENT_FOR_INTERACTIVE_REQUEST
        ),
        MSAL_ACQUIRE_TOKEN_SILENT(
            BrokerApi.ACQUIRE_TOKEN_SILENT,
            BrokerAccountManagerOperation.ACQUIRE_TOKEN_SILENT
        ),
        MSAL_GET_ACCOUNTS(
            BrokerApi.GET_ACCOUNTS,
            BrokerAccountManagerOperation.GET_ACCOUNTS
        ),
        MSAL_REMOVE_ACCOUNT(
            BrokerApi.REMOVE_ACCOUNT,
            BrokerAccountManagerOperation.REMOVE_ACCOUNT
        ),
        MSAL_GET_DEVICE_MODE(
            BrokerApi.GET_DEVICE_MODE,
            BrokerAccountManagerOperation.GET_DEVICE_MODE
        ),
        MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE(
            BrokerApi.GET_CURRENT_ACCOUNT_SHARED_DEVICE,
            BrokerAccountManagerOperation.GET_CURRENT_ACCOUNT
        ),
        MSAL_SIGN_OUT_FROM_SHARED_DEVICE(
            BrokerApi.SIGN_OUT_FROM_SHARED_DEVICE,
            BrokerAccountManagerOperation.REMOVE_ACCOUNT_FROM_SHARED_DEVICE
        ),
        MSAL_GENERATE_SHR(
            BrokerApi.GENERATE_SHR,
            BrokerAccountManagerOperation.GENERATE_SHR
        ),
        //TODO: remove this.
        BROKER_GET_KEY_FROM_INACTIVE_BROKER(
            BrokerApi.UNKNOWN, null
        ),
        BROKER_API_HELLO(
            BrokerApi.BROKER_HELLO, null
        ),
        BROKER_API_GET_BROKER_ACCOUNTS(
            BrokerApi.BROKER_GET_ACCOUNTS, null
        ),
        BROKER_API_REMOVE_BROKER_ACCOUNT(
            BrokerApi.BROKER_REMOVE_ACCOUNT, null
        ),
        BROKER_API_UPDATE_BRT(
            BrokerApi.BROKER_UPDATE_BRT, null
        ),
        BROKER_GET_FLIGHTS(
            BrokerApi.BROKER_GET_FLIGHTS, null
        ),
        BROKER_SET_FLIGHTS(
            BrokerApi.BROKER_SET_FLIGHTS, null
        ),
        MSAL_SSO_TOKEN(
            BrokerApi.GET_SSO_TOKEN, null
        ),
        DEVICE_REGISTRATION_OPERATIONS(
            BrokerApi.DEVICE_REGISTRATION_PROTOCOLS, null
        ),
        BROKER_UPLOAD_LOGS(
            BrokerApi.BROKER_UPLOAD_LOGS, null
        ),
        BROKER_METADATA_RETRIEVAL(
            BrokerApi.BROKER_METADATA_RETRIEVAL, null
        ),
        MSAL_BROKER_DISCOVERY(
            BrokerApi.BROKER_DISCOVERY, null
        ),
        BROKER_SET_ACTIVE_BROKER(
            BrokerApi.BROKER_SET_ACTIVE_BROKER, null
        ),
        BROKER_EMPTY_REQUEST_TEST(
            BrokerApi.BROKER_EMPTY_REQUEST_TEST, null
        );
    }

    /**
     * Packs the response bundle with the account manager key.
     */
    @Throws(BrokerCommunicationException::class)
    fun getAccountManagerBundle(): Bundle? {
        var requestBundle = bundle
        if (requestBundle == null) {
            requestBundle = Bundle()
        }
        requestBundle.putString(
            AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
            getAccountManagerAddAccountOperationKey()
        )
        return requestBundle
    }

    @Throws(BrokerCommunicationException::class)
    private fun getAccountManagerAddAccountOperationKey(): String? {
        val methodTag = "$TAG:getAccountManagerAddAccountOperationKey"
        val accountManagerKey: String? = operation.accountManagerOperation
        if (accountManagerKey == null) {
            val errorMessage =
                "Operation " + operation.name + " is not supported by AccountManager addAccount()."
            Logger.warn(methodTag, errorMessage)
            throw BrokerCommunicationException(
                BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT,
                errorMessage,
                null
            )
        }
        return accountManagerKey
    }

    @Throws(BrokerCommunicationException::class)
    fun getContentProviderPath(): String? {
        val methodTag = "$TAG:getContentProviderUriPath"
        val contentApi: BrokerApi? =
            operation.contentApi
        if (contentApi == null) {
            val errorMessage =
                "Operation " + operation.name + " is not supported by ContentProvider."
            Logger.warn(methodTag, errorMessage)
            throw BrokerCommunicationException(
                BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                IIpcStrategy.Type.CONTENT_PROVIDER,
                errorMessage,
                null
            )
        }
        return contentApi.path
    }
}