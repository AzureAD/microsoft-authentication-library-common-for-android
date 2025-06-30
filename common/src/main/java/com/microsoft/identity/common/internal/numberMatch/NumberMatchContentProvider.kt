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

package com.microsoft.identity.common.internal.numberMatch

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.microsoft.identity.common.logging.Logger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class NumberMatchContentProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.microsoft.identity.common.internal.numberMatch.provider"
        const val TABLE_NAME = "number_match"
        const val SESSION_ID = "sessionId"
        const val NUMBER_MATCH_DATA = "numberMatchData"
        const val CODE_NUMBER_MATCH = 1
        const val EXPIRY_TIME = "expiryTime"
        const val ENTRY_EXPIRY_TIME_IN_MS = 5 * 60 * 1000
        private val deletionLock = Any()
        val TAG = NumberMatchContentProvider::class.java.simpleName
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLE_NAME")
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, TABLE_NAME, CODE_NUMBER_MATCH)
        }
        private val threadExecutor : ExecutorService by lazy {
            Executors.newSingleThreadExecutor()
        }
    }

    private lateinit var dbHelper: NumberMatchDbHelper

    override fun onCreate(): Boolean {
        dbHelper = NumberMatchDbHelper(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        Logger.info(TAG, "Query called: uri=$uri, selection=$selection, selectionArgs=${selectionArgs?.joinToString()}")

        // Validate URI
        validateURI(uri)

        val db = dbHelper.readableDatabase
        return db.query(
            TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        )
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_NUMBER_MATCH -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$TABLE_NAME"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Logger.info(TAG, "Insert called: uri=$uri, values=$values")

        // Validate URI
        validateURI(uri)

        val db = dbHelper.writableDatabase

        // Add timestamp to the new entry
        val currentTime = System.currentTimeMillis()
        val updatedValues = ContentValues(values).apply {
            put(EXPIRY_TIME, currentTime)
        }

        // Delete expired entries
        deleteExpiredDataInBackground(currentTime, db)

        // Insert the new entry
        val id = db.insert(TABLE_NAME, null, updatedValues)
        if (id > 0) {
            Logger.info(TAG, "Insert successful: id=$id")
            return Uri.withAppendedPath(CONTENT_URI, id.toString())
        } else {
            Logger.warn(TAG, "Insert failed")
            return null
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        Logger.info(TAG, "Delete called: uri=$uri, selection=$selection, selectionArgs=${selectionArgs?.joinToString()}")
        validateURI(uri)
        val db = dbHelper.writableDatabase
        val rows = db.delete(TABLE_NAME, selection, selectionArgs)
        Logger.info(TAG, "Rows deleted: $rows")
        return rows
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        Logger.info(TAG, "Update called: uri=$uri, values=$values, selection=$selection, selectionArgs=${selectionArgs?.joinToString()}")
        val db = dbHelper.writableDatabase
        val rows = db.update(TABLE_NAME, values, selection, selectionArgs)
        Logger.info(TAG, "Rows updated: $rows")
        return rows
    }

    private fun validateURI(uri: Uri) {
        if (uriMatcher.match(uri) != CODE_NUMBER_MATCH) {
            Logger.warn(TAG, "Unknown URI: $uri")
            throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun deleteExpiredDataInBackground(currentTime: Long, db: SQLiteDatabase, ) {
        threadExecutor.execute {
            synchronized(deletionLock) {
                val expiryTime = currentTime - ENTRY_EXPIRY_TIME_IN_MS
                db.delete(TABLE_NAME, "timestamp < ?", arrayOf(expiryTime.toString()))
            }
        }
    }
}
