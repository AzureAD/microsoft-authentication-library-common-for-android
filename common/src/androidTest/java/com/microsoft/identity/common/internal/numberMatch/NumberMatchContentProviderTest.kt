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

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NumberMatchContentProviderTest {

    private lateinit var contentUri: Uri

    @Before
    fun setUp() {
        contentUri = NumberMatchContentProvider.CONTENT_URI
        clearDatabase()
    }

    @After
    fun tearDown() {
        clearDatabase()
    }

    private fun clearDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.contentResolver.delete(contentUri, null, null)
    }

    @Test
    fun testInsertAndQuery() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Insert a new entry
        val values = ContentValues().apply {
            put(NumberMatchContentProvider.SESSION_ID, "session1")
            put(NumberMatchContentProvider.NUMBER_MATCH_DATA, "data1")
        }
        val uri = context.contentResolver.insert(contentUri, values)
        assertNotNull(uri)

        // Query the inserted entry
        val cursor: Cursor? = context.contentResolver.query(
            contentUri,
            null,
            "${NumberMatchContentProvider.SESSION_ID} = ?",
            arrayOf("session1"),
            null
        )
        assertNotNull(cursor)
        cursor?.use {
            assertTrue(it.moveToFirst())
            assertEquals("session1", it.getString(it.getColumnIndexOrThrow(NumberMatchContentProvider.SESSION_ID)))
            assertEquals("data1", it.getString(it.getColumnIndexOrThrow(NumberMatchContentProvider.NUMBER_MATCH_DATA)))
        }
    }

    @Test
    fun testExpiryLogic() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Insert an entry with a timestamp older than the expiry time
        val expiredTime = System.currentTimeMillis() - NumberMatchContentProvider.ENTRY_EXPIRY_TIME_IN_MS - 1000
        val expiredValues = ContentValues().apply {
            put(NumberMatchContentProvider.SESSION_ID, "expiredSession")
            put(NumberMatchContentProvider.NUMBER_MATCH_DATA, "expiredData")
            put(NumberMatchContentProvider.EXPIRY_TIME, expiredTime)
        }
        context.contentResolver.insert(contentUri, expiredValues)

        // Insert a valid entry
        val validValues = ContentValues().apply {
            put(NumberMatchContentProvider.SESSION_ID, "validSession")
            put(NumberMatchContentProvider.NUMBER_MATCH_DATA, "validData")
        }
        context.contentResolver.insert(contentUri, validValues)

        // Query all entries
        val cursor: Cursor? = context.contentResolver.query(
            contentUri,
            null,
            null,
            null,
            null
        )
        assertNotNull(cursor)
        cursor?.use {
            assertEquals(1, it.count) // Only the valid entry should remain
            assertTrue(it.moveToFirst())
            assertEquals("validSession", it.getString(it.getColumnIndexOrThrow(NumberMatchContentProvider.SESSION_ID)))
        }
    }

    @Test
    fun testDelete() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Insert an entry
        val values = ContentValues().apply {
            put(NumberMatchContentProvider.SESSION_ID, "session1")
            put(NumberMatchContentProvider.NUMBER_MATCH_DATA, "data1")
        }
        val uri = context.contentResolver.insert(contentUri, values)
        assertNotNull(uri)

        // Delete the entry
        val rowsDeleted = context.contentResolver.delete(
            contentUri,
            "${NumberMatchContentProvider.SESSION_ID} = ?",
            arrayOf("session1")
        )
        assertEquals(1, rowsDeleted)

        // Verify the entry is deleted
        val cursor: Cursor? = context.contentResolver.query(
            contentUri,
            null,
            "${NumberMatchContentProvider.SESSION_ID} = ?",
            arrayOf("session1"),
            null
        )
        assertNotNull(cursor)
        cursor?.use {
            assertFalse(it.moveToFirst())
        }
    }
}