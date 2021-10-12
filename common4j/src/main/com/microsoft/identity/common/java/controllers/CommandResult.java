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


@Getter
@Accessors(prefix = "m")
public class CommandResult<T> implements ICommandResult<T> {

    private final ResultStatus mStatus;

    private final T mResult;

    private final String mCorrelationId;

    private final Class<T> mResultClass;

    @Setter
    private List<Map<String, String>> mTelemetryMap = new ArrayList<>();

    @Deprecated
    public CommandResult(ResultStatus status, T result) {
        this(status, result, null);
    }

    /**
     * Construct a command result with the corresponding status, and correlation id.
     * @param status the ResultStatus of the command.
     * @param result the command result, may <strong>NOT</strong> be null.
     * @param correlationId an optional correlation Id for the command.
     */
    public CommandResult(final @NonNull ResultStatus status, final @NonNull T result, @Nullable String correlationId) {
        mStatus = status;
        mResult = result;
        mCorrelationId = correlationId == null ? "UNSET" : correlationId;
        @SuppressWarnings("unchecked")
        final Class<T> aClass = (Class<T>) result.getClass();
        mResultClass = aClass;
    }

    /**
     * Construct a command that does not contain a result object.
     * @param status the result status.
     * @param correlationId an optional correlation id.
     */
    private CommandResult(final @NonNull ResultStatus status, @Nullable String correlationId) {
        mStatus = status;
        mResult = null;
        mCorrelationId = correlationId == null ? "UNSET" : correlationId;
        final Class<T> aClass = (Class<T>) Void.class;
        this.mResultClass = aClass;
    }

    /**
     * This is a special method for constructing a result with a null value.  This is to insure
     * that we can have null-checking on the parameters for the other construction methods.
     * @param status the {@link com.microsoft.identity.common.java.commands.ICommandResult.ResultStatus}.
     * @param correlationId a correlation id.
     * @return A command result with a null result, given the status provided.
     */
    public static CommandResult<Void> ofNull(final @NonNull ResultStatus status, final @Nullable String correlationId) {
        return new CommandResult<>(status, correlationId);
    }

    /**
     * A factory method for constructing command results.
     * @param status the result status.
     * @param result the result of the command.
     * @param correlationId an optional correlation id.
     * @param <T> the type of the result.
     * @return a commandResult object containing the above values.
     */
    public static <T> CommandResult<T> of(final @NonNull ResultStatus status, final @NonNull T result, final @Nullable String correlationId) {
        return new CommandResult<T>(status, result, correlationId);
    }
}
