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
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
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
        Mockito.reset(context, contentResolver, cursor)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.insert(ArgumentMatchers.any(), ArgumentMatchers.any(ContentValues::class.java))).thenReturn(null)
        `when`(contentResolver.delete(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(1)
        `when`(contentResolver.query(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(),
            ArgumentMatchers.isNull()
        )).thenReturn(cursor)
        `when`(cursor.moveToFirst()).thenReturn(true)
        `when`(cursor.getColumnIndexOrThrow(ArgumentMatchers.anyString())).thenReturn(0)
        `when`(cursor.getString(0)).thenReturn(mockNumberMatchValue)
    }

    @After
    fun tearDown() {
        Mockito.reset(context, contentResolver, cursor)
    }

    @Test
    fun `storeNumberMatch should insert when sessionId and numberMatch are not null`() {
        NumberMatchHelper.storeNumberMatch(context, mockSessionId, mockNumberMatchValue)
        verify(contentResolver, times(1)).insert(ArgumentMatchers.any(), ArgumentMatchers.any(ContentValues::class.java))
    }

    @Test
    fun `storeNumberMatch should not insert when sessionId is null`() {
        NumberMatchHelper.storeNumberMatch(context, null, mockNumberMatchValue)
        verify(contentResolver, times(0)).insert(ArgumentMatchers.any(), ArgumentMatchers.any(ContentValues::class.java))
    }

    @Test
    fun `storeNumberMatch should not insert when numberMatch is null`() {
        NumberMatchHelper.storeNumberMatch(context, mockSessionId, null)
        verify(contentResolver, times(0)).insert(ArgumentMatchers.any(), ArgumentMatchers.any(ContentValues::class.java))
    }

    @Test
    fun `getNumberMatch should return value when sessionId exists`() {
        `when`(cursor.moveToFirst()).thenReturn(true)
        `when`(cursor.getString(0)).thenReturn(mockNumberMatchValue)
        val result = NumberMatchHelper.getNumberMatch(context, mockSessionId)
        assertEquals(mockNumberMatchValue, result)
    }

    @Test
    fun `getNumberMatch should return null when sessionId is null`() {
        val result = NumberMatchHelper.getNumberMatch(context, null)
        assertNull(result)
    }

    @Test
    fun `getNumberMatch should return null when cursor is null`() {
        `when`(contentResolver.query(
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(),
            ArgumentMatchers.isNull()
        )).thenReturn(null)
        val result = NumberMatchHelper.getNumberMatch(context, mockSessionId)
        assertNull(result)
    }

    @Test
    fun `getNumberMatch should return null when cursor is empty`() {
        `when`(cursor.moveToFirst()).thenReturn(false)
        val result = NumberMatchHelper.getNumberMatch(context, mockSessionId)
        assertNull(result)
    }

    @Test
    fun `clearNumberMatchData should call delete on contentResolver`() {
        NumberMatchHelper.clearNumberMatch(context)
        verify(contentResolver, times(1)).delete(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }
}
