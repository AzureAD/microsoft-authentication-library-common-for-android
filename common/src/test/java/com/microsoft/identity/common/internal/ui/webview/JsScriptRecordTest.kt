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
package com.microsoft.identity.common.internal.ui.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JsScriptRecordTest {

    @Test
    fun `isAllowedForUrl returns true when allowedUrls is null`() {
        val record = JsScriptRecord("id", "script", null)
        assertTrue(record.isAllowedForUrl("https://any.url.com"))
    }

    @Test
    fun `isAllowedForUrl returns true for exact allowed prefix`() {
        val allowed = setOf("https://example.com")
        val record = JsScriptRecord("id", "script", allowed)
        assertTrue(record.isAllowedForUrl("https://example.com/page"))
    }

    @Test
    fun `isAllowedForUrl returns false for non-matching prefix`() {
        val allowed = setOf("https://example.com")
        val record = JsScriptRecord("id", "script", allowed)
        assertFalse(record.isAllowedForUrl("https://other.com/page"))
    }

    @Test
    fun `isAllowedForUrl returns true for sovereign cloud with fido in path`() {
        val allowed = setOf("https://login.microsoftonline.us")
        val record = JsScriptRecord("id", "script", allowed)
        assertTrue(record.isAllowedForUrl("https://login.microsoftonline.us/fido/endpoint"))
        assertTrue(record.isAllowedForUrl("https://login.microsoftonline.us/some/path/fido"))
    }

    @Test
    fun `isAllowedForUrl returns false for sovereign cloud without fido in path`() {
        val allowed = setOf("https://login.microsoftonline.us")
        val record = JsScriptRecord("id", "script", allowed)
        assertFalse(record.isAllowedForUrl("https://login.microsoftonline.us/other/endpoint"))
    }

    @Test
    fun `isAllowedForUrl returns false for sovereign cloud subdomain`() {
        val allowed = setOf("https://login.microsoftonline.us")
        val record = JsScriptRecord("id", "script", allowed)
        // Should not match, as it's a subdomain, not a path
        assertFalse(record.isAllowedForUrl("https://login.microsoftonline.us.someDomain.com/fido"))
    }

    @Test
    fun `isAllowedForUrl returns true for non-sovereign allowed prefix`() {
        val allowed = setOf("https://mytenant.b2clogin.com")
        val record = JsScriptRecord("id", "script", allowed)
        assertTrue(record.isAllowedForUrl("https://mytenant.b2clogin.com/path"))
    }
}

