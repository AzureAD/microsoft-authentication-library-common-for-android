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
// OUT OF OR IN CO
package com.microsoft.identity.common.java.opentelemetry.perf;


import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PerfMeasurement {
    @SerializedName("operation_name")
    private final String operationName;

    @SerializedName("operation_type")
    private final String operationType;

    @SerializedName("elapsed_time")
    private final Long elapsedTime;

    public PerfMeasurement(String operationName, String operationType, Long elapsedTime) {
        this.operationName = operationName;
        this.operationType = operationType;
        this.elapsedTime = elapsedTime;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getOperationType() {
        return operationType;
    }

    public Long getElapsedTime() {
        return elapsedTime;
    }
}
