//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.internal.fido

import com.microsoft.identity.common.java.constants.FidoConstants
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class FidoUtilTest {

    val webauthnParam = java.util.AbstractMap.SimpleEntry<String, String>(
        FidoConstants.WEBAUTHN_QUERY_PARAMETER_FIELD,
        FidoConstants.WEBAUTHN_QUERY_PARAMETER_VALUE
    )
    val emptyList = ArrayList<Map.Entry<String, String>>()
    val singleList = ArrayList<Map.Entry<String, String>>(listOf(java.util.AbstractMap.SimpleEntry<String,String>("foo", "1")))
    val alreadyInList = ArrayList<Map.Entry<String, String>>(listOf(webauthnParam))

    @Test
    @Config(sdk = [28])
    fun testUpdateWithOrDeleteWebAuthnParam_emptyListWebAuthnCapable() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(emptyList, true)
        assertTrue(list.contains(webauthnParam))
    }

    @Test
    @Config(sdk = [28])
    fun testUpdateWithOrDeleteWebAuthnParam_emptyListNotWebAuthnCapable() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(emptyList, false)
        assertFalse(list.contains(webauthnParam))
    }

    @Test
    @Config(sdk = [28])
    fun testUpdateWithOrDeleteWebAuthnParam_singleListWebAuthnCapable() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(singleList, true)
        assertTrue(list.contains(webauthnParam))
    }

    @Test
    @Config(sdk = [28])
    fun testUpdateWithOrDeleteWebAuthnParam_singleListNotWebAuthnCapable() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(singleList, false)
        assertFalse(list.contains(webauthnParam))
    }

    @Test
    @Config(sdk = [28])
    fun testUpdateWithOrDeleteWebAuthnParam_alreadyInListWebAuthnCapable() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(alreadyInList, true)
        assertTrue(list.contains(webauthnParam))
    }

    @Test
    @Config(sdk = [28])
    fun testUpdateWithOrDeleteWebAuthnParam_alreadyListNotWebAuthnCapable() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(alreadyInList, false)
        // We don't remove, since the app could be doing the per-request option and manually adding the param.
        assertTrue(list.contains(webauthnParam))
    }

    @Test
    @Config(sdk = [26])
    fun testUpdateWithOrDeleteWebAuthnParam_emptyListWebAuthnCapableLowOs() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(emptyList, true)
        assertFalse(list.contains(webauthnParam))
    }

    @Test
    @Config(sdk = [26])
    fun testUpdateWithOrDeleteWebAuthnParam_singleListWebAuthnCapableLowOs() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(singleList, true)
        assertFalse(list.contains(webauthnParam))
    }

    @Test
    @Config(sdk = [26])
    fun testUpdateWithOrDeleteWebAuthnParam_alreadyInListWebAuthnCapableLowOs() {
        val list = FidoUtil.updateWithOrDeleteWebAuthnParam(alreadyInList, true)
        assertFalse(list.contains(webauthnParam))
    }


}
