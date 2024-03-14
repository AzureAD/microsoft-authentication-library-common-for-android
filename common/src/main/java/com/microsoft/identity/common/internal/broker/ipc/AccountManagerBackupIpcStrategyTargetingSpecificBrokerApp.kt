// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.broker.ipc

import android.accounts.AccountManager
import android.accounts.AuthenticatorDescription
import android.content.Context
import android.os.Bundle
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.internal.broker.IBrokerValidator
import com.microsoft.identity.common.internal.util.AccountManagerUtil
import com.microsoft.identity.common.internal.util.ProcessUtil
import com.microsoft.identity.common.logging.Logger
import java.util.concurrent.TimeUnit

/**
 * An IPC strategy that utilizes AccountManager.addAccount().
 *
 * The receiving end of this class is Broker's AccountAuthenticatorForBrokerDiscovery.
 * Each broker apps would need to wire that class up to AccountManager manually,
 * with a unique account type.
 * (see getAccountTypeForEachPackage() and
 * https://developer.android.com/training/sync-adapters/creating-authenticator)
 *
 * As of Jan 23, this class (and AccountAuthenticatorForBrokerDiscovery) only supports
 * Broker Discovery and Passthrough mode.
 *
 * NOTE: This is not an official IPC Mechanism, therefore it is meant to be used as a backup only.
 * (Apparently, it's more resilient than Content Provider and Bound Service under certain scenarios).
 **/
class AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp
    internal constructor (private val accountTypeForEachPackage: Map<String, String>,
                          private val sendRequestViaAccountManager: (accountType: String, bundle: Bundle) -> Bundle,
                          private val getAccountManagerApps: () -> Array<AuthenticatorDescription>,
                          private val brokerValidator: IBrokerValidator) : IIpcStrategy {
    companion object {
        val TAG = AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp::class.simpleName

        // Content of the bundle to be sent.
        /** Key associated to Content Provider path of [BrokerOperationBundle.Operation] **/
        const val CONTENT_PROVIDER_PATH_KEY = "CONTENT_PROVIDER_PATH"

        /** Key associated to request bundle **/
        const val REQUEST_BUNDLE_KEY = "REQUEST_BUNDLE"

        // Account types associated to each broker apps.
        /** Account type for Link To Windows (defined in Broker's res/xml/ltw_account_manager_passthrough_backup.xml) **/
        internal const val LTW_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE = "com.microsoft.ltwpassthroughbackup"

        /** Account type for Company Portal (defined in Broker's res/xml/cp_account_manager_passthrough_backup.xml) **/
        internal const val CP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE = "com.microsoft.cppassthroughbackup"

        /** Account type for Authenticator (defined in Broker's res/xml/authapp_account_manager_passthrough_backup.xml) **/
        internal const val AUTHAPP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE = "com.microsoft.authapppassthroughbackup"

        /**
         * Returns AccountManager account types associated to each Broker app packages.
         **/
        internal fun getAccountTypeForEachPackage(): Map<String, String>{
            val result = mapOf(
                BrokerData.debugMockLtw.packageName to LTW_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,
                BrokerData.prodLTW.packageName to LTW_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,

                BrokerData.debugMockCp.packageName to CP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,
                BrokerData.prodCompanyPortal.packageName to CP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,

                BrokerData.debugMockAuthApp.packageName to AUTHAPP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,
                BrokerData.prodMicrosoftAuthenticator.packageName to AUTHAPP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,
            )

            // This will throw an error if any PROD apps doesn't implement this mechanism.
            // (To make sure we don't miss this if we have to add another broker app in the future).
            BrokerData.prodBrokers.forEach {
                assert(result.containsKey(it.packageName))
            }

            return result
        }

        /**
         * Gets an instance of this class.
         * This will return null if AccountManager cannot be used as an IPC mechanism.
         * */
        fun tryCreateInstance(context: Context):
                AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp?{
            if (!AccountManagerUtil.canUseAccountManagerOperation(
                    context,
                    setOf(LTW_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,
                        CP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE,
                        AUTHAPP_BACKUP_IPC_ACCOUNT_MANAGER_ACCOUNT_TYPE)
                )){
                return null
            }

            val accountManager = AccountManager.get(context)

            return AccountManagerBackupIpcStrategyTargetingSpecificBrokerApp(
                accountTypeForEachPackage = getAccountTypeForEachPackage(),
                sendRequestViaAccountManager = { accountType, requestBundle ->
                    accountManager.addAccount(
                        accountType,
                        null,
                        null,
                        requestBundle,
                        null,
                        null,
                        ProcessUtil.getPreferredHandler()
                    ).getResult(5, TimeUnit.SECONDS)
                },
                getAccountManagerApps = {
                    accountManager.authenticatorTypes
                },
                brokerValidator = BrokerValidator(context))
        }
    }

    @Throws(BrokerCommunicationException::class)
    override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? {
        val methodTag = "$TAG:communicateToBroker"
        val targetPackageName = bundle.targetBrokerAppPackageName

        val accountType = accountTypeForEachPackage.getOrElse(targetPackageName) {
            throw BrokerCommunicationException(
                BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                getType(),
                "AccountManagerBackupIpcStrategy doesn't recognize $targetPackageName as a broker",
                null
            )
        }

        // check if the account type owner actually belongs to the corresponding broker app.
        validateTargetApp(targetPackageName, accountType)

        // Pack the request bundle.
        // Changing this format is a breaking change
        // as the receiving end might be on an older broker version (therefore doesn't recognize the change)
        val requestBundle = Bundle().apply {
            putParcelable(REQUEST_BUNDLE_KEY, bundle.bundle)
            putString(CONTENT_PROVIDER_PATH_KEY, bundle.operation.contentApi.path)
        }

        return try {
            val result = sendRequestViaAccountManager(accountType, requestBundle)
            result
        } catch (e: Throwable) {
            val errorMessage =
                if (e.message.isNullOrEmpty())
                    "${e.javaClass.simpleName} is thrown"
                else
                    e.message

            Logger.error(methodTag, errorMessage, e)
            // Technically... this might NOT be a connection error.
            // AccountManager returns both connection error and legit failure
            // (i.e. not supported, bad request) as AuthenticatorException...
            // It also has no error code. The only difference would be in the error message.
            throw BrokerCommunicationException(
                BrokerCommunicationException.Category.CONNECTION_ERROR,
                getType(),
                "AccountManager failed to respond - $errorMessage",
                e
            )
        }
    }

    override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String): Boolean {
        val methodTag = "$TAG:isSupportedByTargetedBroker"

        return try {
            val accountType = accountTypeForEachPackage.getOrElse(targetedBrokerPackageName) {
                Logger.info(methodTag,
                    "AccountManagerBackupIpcStrategy doesn't recognize $targetedBrokerPackageName as a broker",)
                return false
            }

            validateTargetApp(targetedBrokerPackageName, accountType)
            true
        } catch (t: Throwable){
            Logger.error(methodTag, t.message, t)
            false
        }
    }

    /**
     * Check if the App currently associated to the given account type is
     * actually a valid broker app.
     **/
    @Throws(BrokerCommunicationException::class)
    private fun validateTargetApp(
        targetPackageName: String,
        accountType: String
    ) {
        val targetAppInfo = try {
            getAccountManagerApps().first {
                it.packageName == targetPackageName && it.type == accountType
            }
        } catch (t: Throwable) {
            throw BrokerCommunicationException(
                BrokerCommunicationException.Category.VALIDATION_ERROR,
                getType(),
                "$targetPackageName doesn't support account manager backup ipc.",
                null)
        }

        if (!brokerValidator.isValidBrokerPackage(targetAppInfo.packageName)) {
            throw BrokerCommunicationException(
                BrokerCommunicationException.Category.VALIDATION_ERROR,
                getType(),
                "${targetAppInfo.packageName} is not a valid broker app.",
                null
            )
        }
    }

    override fun getType(): IIpcStrategy.Type {
        return IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT
    }
}