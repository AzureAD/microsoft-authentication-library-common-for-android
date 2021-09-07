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
package com.microsoft.identity.common.java.controllers;


import com.microsoft.identity.common.java.commands.ICommandResult;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

public class CommandResult<T> implements ICommandResult<T> {

    @Getter
    @Accessors(prefix = "m")
    private final ResultStatus mStatus;

    @Getter
    @Accessors(prefix = "m")
    private final T mResult;

    @Getter
    @Accessors(prefix = "m")
    private final String mCorrelationId;

    @Getter
    @Accessors(prefix = "m")
    private final Class<T> mResultClass;

    @Setter
    @Getter
    @Accessors(prefix = "m")
    private List<Map<String, String>> mTelemetryMap = new ArrayList<>();

    public CommandResult(ResultStatus status, T result) {
        this(status, result, null);
    }

    public CommandResult(final @NonNull ResultStatus status, final @NonNull T result, @Nullable String correlationId) {
        mStatus = status;
        mResult = result;
        mCorrelationId = correlationId == null ? "UNSET" : correlationId;
        mResultClass = ((Class<T>) result.getClass());
    }

    private CommandResult(final @NonNull ResultStatus status, @Nullable String correlationId) {
        mStatus = status;
        mResult = null;
        mCorrelationId = correlationId == null ? "UNSET" : correlationId;
        mResultClass = (Class<T>) Void.class;
    }

    public static CommandResult<Void> ofNull(ResultStatus status, @Nullable String correlationId) {
        return new CommandResult<Void>(status, correlationId);
    }
}
