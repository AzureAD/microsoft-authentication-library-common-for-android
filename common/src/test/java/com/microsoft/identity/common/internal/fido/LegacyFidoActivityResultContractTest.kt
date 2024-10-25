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
package com.microsoft.identity.common.internal.fido

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.fido.Fido
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LegacyFidoActivityResultContractTest {

    val resultContract = LegacyFidoActivityResultContract()

    @Test
    fun testParseResult_nullIntent() {
        resultContract.createIntent(ExtendedTestWebView().context,
            LegacyFido2ApiObject(
                { result ->
                    fail("Should not succeed")
                },
                { exception ->
                    assertEquals(LegacyFido2ApiException.NULL_OBJECT, exception.errorCode)
                },
                PendingIntent.getBroadcast(ExtendedTestWebView().context, 0, Intent("foo"), 0)
            )
        )
        resultContract.parseResult(Activity.RESULT_OK, null)
    }

    @Test
    fun testParseResult_notOkResultCode() {
        resultContract.createIntent(ExtendedTestWebView().context,
            LegacyFido2ApiObject(
                { result ->
                    fail("Should not succeed")
                },
                { exception ->
                    assertEquals(LegacyFido2ApiException.BAD_ACTIVITY_RESULT_CODE, exception.errorCode)
                },
                PendingIntent.getBroadcast(ExtendedTestWebView().context, 0, Intent("foo"), 0)
            )
        )
        resultContract.parseResult(Activity.RESULT_CANCELED, Intent())
    }

    @Test
    fun testParseResult_credentialExtraNull() {
        resultContract.createIntent(ExtendedTestWebView().context,
            LegacyFido2ApiObject(
                { result ->
                    fail("Should not succeed")
                },
                { exception ->
                    assertEquals(LegacyFido2ApiException.NULL_OBJECT, exception.errorCode)
                },
                PendingIntent.getBroadcast(ExtendedTestWebView().context, 0, Intent("foo"), 0)
            )
        )
        val intent = Intent()
        val nullBytes : ByteArray? = null
        intent.putExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA, nullBytes)
        resultContract.parseResult(Activity.RESULT_OK, intent)
    }
}
