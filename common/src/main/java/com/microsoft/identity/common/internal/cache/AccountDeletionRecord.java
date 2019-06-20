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

package com.microsoft.identity.common.internal.cache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.dto.AccountRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Functions as a 'receipt' when deleting AccountRecords: AccountRecords which have been removed
 * from the cache are returned in this container.
 */
public class AccountDeletionRecord implements List<AccountRecord> {

    private static final String RESULT_IS_READ_ONLY = "Result is read-only";
    private final List<AccountRecord> mAccountRecordList;

    AccountDeletionRecord(@Nullable final List<AccountRecord> accountRecordMetadataList) {
        if (null == accountRecordMetadataList) {
            mAccountRecordList = new ArrayList<>();
        } else {
            mAccountRecordList = accountRecordMetadataList;
        }
    }

    @Override
    public int size() {
        return mAccountRecordList.size();
    }

    @Override
    public boolean isEmpty() {
        return mAccountRecordList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return mAccountRecordList.contains(o);
    }

    @Override
    public Iterator<AccountRecord> iterator() {
        return mAccountRecordList.iterator();
    }

    @Override
    public Object[] toArray() {
        return mAccountRecordList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return mAccountRecordList.toArray(a);
    }

    @Override
    public boolean add(AccountRecord accountRecord) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);

    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);

    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return mAccountRecordList.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends AccountRecord> c) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);

    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends AccountRecord> c) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);
    }

    @Override
    public AccountRecord get(int index) {
        return mAccountRecordList.get(index);
    }

    @Override
    public AccountRecord set(int index, AccountRecord element) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);

    }

    @Override
    public void add(int index, AccountRecord element) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);
    }

    @Override
    public AccountRecord remove(int index) {
        throw new UnsupportedOperationException(RESULT_IS_READ_ONLY);
    }

    @Override
    public int indexOf(Object o) {
        return mAccountRecordList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return mAccountRecordList.lastIndexOf(o);
    }

    @NonNull
    @Override
    public ListIterator<AccountRecord> listIterator() {
        return mAccountRecordList.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<AccountRecord> listIterator(int index) {
        return mAccountRecordList.listIterator(index);
    }

    @NonNull
    @Override
    public List<AccountRecord> subList(int fromIndex, int toIndex) {
        return mAccountRecordList.subList(fromIndex, toIndex);
    }
}
