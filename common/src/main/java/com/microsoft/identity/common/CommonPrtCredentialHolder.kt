package com.microsoft.identity.common

import android.app.Activity
import com.microsoft.identity.common.logging.Logger

/**
 * Consumer of commons needs to implement [IPrtCredentialHolder] interface
 * and set it using CommonPrtCredentialHolder.initializeCommonPrtCredentialHolder(@NonNull IPrtCredentialHolder prtCredentialHolder)
 * to provide prtCredentialHolder to common module.
 */
object CommonPrtCredentialHolder : IPrtCredentialHolder {
    private val TAG = CommonPrtCredentialHolder::class.java.simpleName
    private var mPrtCredentialHolder: IPrtCredentialHolder? = null

    // Note : This method should only be invoked by broker module.
    fun initializeCommonPrtCredentialHolder(prtCredentialHolder: IPrtCredentialHolder) {
        val methodTag = "$TAG:initializeCommonPrtCredentialHolder"
        Logger.info(methodTag, "Initializing common prt credential holder with " + prtCredentialHolder.javaClass.simpleName)
        mPrtCredentialHolder = prtCredentialHolder
    }

    override fun getRefreshTokenCredentialUsingNewNonce(authorityStr : String, username : String, nonce : String, prtHeader : String, activity : Activity) : String? {
        val methodTag = "$TAG:getRefreshTokenCredentialUsingNewNonce";
        if (mPrtCredentialHolder != null) {
            return mPrtCredentialHolder?.getRefreshTokenCredentialUsingNewNonce(authorityStr, username,
                nonce,
                prtHeader,
                activity
            )
        }
        Logger.warn(methodTag, "mPrtCredentialHolder is not initialized!")
        return null
    }
}