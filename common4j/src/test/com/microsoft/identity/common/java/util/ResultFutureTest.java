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
package com.microsoft.identity.common.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ResultFutureTest {

    @Test
    public void testSetResultCompletesFutureAndGetReturnsValue() throws Exception {
        final ResultFuture<String> future = new ResultFuture<>();
        assertFalse(future.isDone());
        assertFalse(future.isCancelled());

        future.setResult("value");

        assertTrue(future.isDone());
        assertEquals("value", future.get());
        assertEquals("value", future.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testCancelIsNotSupported() {
        final ResultFuture<String> future = new ResultFuture<>();
        assertFalse(future.cancel(true));
        assertFalse(future.isCancelled());
    }

    @Test
    public void testSetExceptionCausesGetToThrowExecutionException() throws Exception {
        final ResultFuture<String> future = new ResultFuture<>();
        final IllegalStateException cause = new IllegalStateException("boom");
        future.setException(cause);

        assertTrue(future.isDone());
        try {
            future.get();
            fail("Expected ExecutionException");
        } catch (final ExecutionException e) {
            assertSame(cause, e.getCause());
        }
    }

    @Test(expected = TimeoutException.class)
    public void testGetWithTimeoutThrowsWhenNotCompleted() throws Exception {
        final ResultFuture<String> future = new ResultFuture<>();
        future.get(10, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testWhenCompleteRegisteredBeforeCompletionReceivesResult() {
        final ResultFuture<String> future = new ResultFuture<>();
        final AtomicReference<String> captured = new AtomicReference<>();
        future.whenComplete((result, throwable) -> captured.set(result));

        future.setResult("done");

        assertEquals("done", captured.get());
    }

    @Test
    public void testWhenCompleteRegisteredAfterCompletionIsInvokedImmediately() {
        final ResultFuture<String> future = new ResultFuture<>();
        future.setResult("done");

        final AtomicReference<String> captured = new AtomicReference<>();
        future.whenComplete((result, throwable) -> captured.set(result));

        assertEquals("done", captured.get());
    }
}
