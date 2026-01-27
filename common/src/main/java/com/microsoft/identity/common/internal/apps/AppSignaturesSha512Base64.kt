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
package com.microsoft.identity.common.internal.apps

/**
 * Contains Base64-encoded SHA-512 signatures for known  apps.
 */
object AppSignaturesSha512Base64 {
    const val SHARED_EDGE_SIGNATURE =
        "Ivy+Rk6ztai/IudfbyUrSHugzRqAtHWslFvHT0PTvLMsEKLUIgv7ZZbVxygWy/M5mOPpfjZrd3vOx3t+cA6fVQ=="

    const val ONE_AUTH_TEST_APP_SIGNATURE =
        "3V1mY6V7xXG5h0jz6KX1K5e4Z1k3q5V7y8Z9a0b1c2d3e4f5g6h7i8j9k0l1m2n3o4p5q6r7s8t9u0v1w2x3y4z5a6b7c8d9e0f1"

    const val SHARED_INTUNE_APP_SIGNATURE =
        "jPpMoaNvcxSLMX4yG4C3Gf86rtTqh33SqpuRKg4WOP+MnnpA52zZgvKLW76U4Cqqf68iaBk9W7k/jhciiSAtgQ=="

    const val INTUNE_AOSP_AGENT_DEBUG_SIGNATURE =
        "P+9aBy/EDfZVqtyeHWaLWpyklznLb4FkhAbjPHe/pHLa084vhjZdGEb9z7Fef9OghQqYmMfg3T8QqW8gMHfGyA=="
}
