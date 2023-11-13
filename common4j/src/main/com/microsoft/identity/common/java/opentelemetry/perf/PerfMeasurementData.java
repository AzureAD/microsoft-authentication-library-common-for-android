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
// LIABILITY, WHETHER IN A
package com.microsoft.identity.common.java.opentelemetry.perf;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PerfMeasurementData {

    @SerializedName("operation_perf_totals")
    private final List<OperationPerfTotal> operationPerfTotals;

    @SerializedName("network_total")
    private final long networkTotal;

    @SerializedName("account_manager_total")
    private final long accountManagerTotal;

    @SerializedName("shared_preferences_cache_total")
    private final long sharedPreferencesCacheTotal;

    @SerializedName("wpj_data_total")
    private final long wpjDataTotal;

    @SerializedName("key_operation_total")
    private final long keyOperationTotal;

    public PerfMeasurementData(List<OperationPerfTotal> operationPerfTotals,
                               long networkTotal,
                               long accountManagerTotal,
                               long sharedPreferencesCacheTotal,
                               long wpjDataTotal,
                               long keyOperationTotal) {
        this.operationPerfTotals = operationPerfTotals;
        this.networkTotal = networkTotal;
        this.accountManagerTotal = accountManagerTotal;
        this.sharedPreferencesCacheTotal = sharedPreferencesCacheTotal;
        this.wpjDataTotal = wpjDataTotal;
        this.keyOperationTotal = keyOperationTotal;
    }

    public List<OperationPerfTotal> getOperationPerfTotals() {
        return operationPerfTotals;
    }

    public long getNetworkTotal() {
        return networkTotal;
    }

    public long getAccountManagerTotal() {
        return accountManagerTotal;
    }

    public long getSharedPreferencesCacheTotal() {
        return sharedPreferencesCacheTotal;
    }

    public long getWpjDataTotal() {
        return wpjDataTotal;
    }

    public long getKeyOperationTotal() {
        return keyOperationTotal;
    }
}
