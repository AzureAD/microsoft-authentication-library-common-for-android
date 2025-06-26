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
package com.microsoft.identity.common.internal.numberMatch

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class NumberMatchHelperTest {

    private val mockSessionId = "1234"
    private val mockNumberMatchValue = "00"
    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var contentResolver: ContentResolver
    @Mock
    private lateinit var cursor: Cursor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(context.contentResolver).thenReturn(contentResolver)
    }

    @Test
    fun `test storeNumberMatch with valid inputs`() {
        Mockito.`when`(contentResolver.insert(Mockito.any(), Mockito.any(ContentValues::class.java))).thenReturn(null)
        Mockito.`when`(contentResolver.query(
            Mockito.any(),
            Mockito.any(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.isNull()
        )).thenReturn(cursor)
        Mockito.`when`(cursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(cursor.getColumnIndexOrThrow(Mockito.anyString())).thenReturn(0)
        Mockito.`when`(cursor.getString(0)).thenReturn(mockNumberMatchValue)

        NumberMatchHelper.storeNumberMatch(context, mockSessionId, mockNumberMatchValue)
        val result = NumberMatchHelper.getNumberMatch(context, mockSessionId)
        assertEquals(mockNumberMatchValue, result)
    }

    @Test
    fun `test storeNumberMatch with null sessionId`() {
        NumberMatchHelper.storeNumberMatch(context, null, mockNumberMatchValue)
        val result = NumberMatchHelper.getNumberMatch(context, null)
        assertNull(result)
    }

    @Test
    fun `test storeNumberMatch with null numberMatch`() {
        NumberMatchHelper.storeNumberMatch(context, mockSessionId, null)
        // Should not call insert, so query returns null
        Mockito.`when`(contentResolver.query(
            Mockito.any(),
            Mockito.any(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.isNull()
        )).thenReturn(null)
        val result = NumberMatchHelper.getNumberMatch(context, mockSessionId)
        assertNull(result)
    }

    @Test
    fun `test getNumberMatch with non-existent sessionId`() {
        Mockito.`when`(contentResolver.query(
            Mockito.any(),
            Mockito.any(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.isNull()
        )).thenReturn(cursor)
        Mockito.`when`(cursor.moveToFirst()).thenReturn(false)
        val result = NumberMatchHelper.getNumberMatch(context, "nonexistent")
        assertNull(result)
    }

    @Test
    fun `test getNumberMatch after storing and clearing`() {
        // Simulate storing
        Mockito.`when`(contentResolver.insert(Mockito.any(), Mockito.any(ContentValues::class.java))).thenReturn(null)
        // Simulate clearing by returning a cursor that returns false for moveToFirst
        Mockito.`when`(contentResolver.query(
            Mockito.any(),
            Mockito.any(),
            Mockito.anyString(),
            Mockito.any(),
            Mockito.isNull()
        )).thenReturn(cursor)
        Mockito.`when`(cursor.moveToFirst()).thenReturn(false)
        NumberMatchHelper.storeNumberMatch(context, mockSessionId, mockNumberMatchValue)
        val result = NumberMatchHelper.getNumberMatch(context, mockSessionId)
        assertNull(result)
    }
}
