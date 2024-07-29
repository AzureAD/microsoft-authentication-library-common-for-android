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
package com.microsoft.identity.common.java.logging

import com.microsoft.identity.common.java.nativeauth.util.ILoggable
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ILoggableTest {
    @Before
    fun setUp() {
        Logger.resetLogger()
        DiagnosticContext.INSTANCE.clear()
    }

    @Test()
    fun testPIIObject() {
        data class PIIObject(
            val fieldOne: String,
            val PIIfieldTwo: String,
            val fieldThree: Int,
            val PIIfieldFour: Int,
            val fieldFive: Boolean,
            val PIIfieldSix: Boolean
        ) : ILoggable {
            override fun toUnsanitizedString(): String {
                return "PIIObject(fieldOne=" + fieldOne + ", PIIfieldTwo=" + PIIfieldTwo + ", fieldThree=" + fieldThree +
                        ", PIIfieldFour=" + PIIfieldFour + ", fieldFive=" + fieldFive + ", PIIfieldSix=" + PIIfieldSix + ")"
            }

            override fun toString(): String {
                return "PIIObject(fieldOne=" + fieldOne + ", fieldThree=" + fieldThree +
                        ", fieldFive=" + fieldFive + ")"
            }
        }

        val fieldOne = "lorum"
        val fieldTwo = "ipsum"
        val fieldThree = 1
        val fieldFour = 2
        val fieldFive = true
        val fieldSix = false

        val objectUnderTest = PIIObject(
            fieldOne,
            fieldTwo,
            fieldThree,
            fieldFour,
            fieldFive,
            fieldSix
        )

        val objectAsUnsanitisedString = objectUnderTest.toUnsanitizedString()
        val objectAsString = objectUnderTest.toString()

        val expectedUnsanitisedString =
            "PIIObject(fieldOne=" + fieldOne + ", PIIfieldTwo=" + fieldTwo + ", fieldThree=" + fieldThree +
                    ", PIIfieldFour=" + fieldFour + ", fieldFive=" + fieldFive + ", PIIfieldSix=" + fieldSix + ")"
        val expectedString = "PIIObject(fieldOne=" + fieldOne + ", fieldThree=" + fieldThree +
                ", fieldFive=" + fieldFive + ")"

        Assert.assertEquals(expectedUnsanitisedString, objectAsUnsanitisedString)
        Assert.assertEquals(expectedString, objectAsString)
        Assert.assertNotEquals(expectedUnsanitisedString, expectedString)
    }

    @Test
    fun testNonPIIObject() {
        class NonPIIObject(
            var fieldOne: String,
            var fieldTwo: String,
            var fieldThree: Int,
            var fieldFour: Int,
            var fieldFive: Boolean,
            var fieldSix: Boolean
        ) : ILoggable {
            override fun toUnsanitizedString(): String {
                return "NonPIIObject(fieldOne=" + fieldOne + ", fieldTwo=" + fieldTwo + ", fieldThree=" + fieldThree +
                        ", fieldFour=" + fieldFour + ", fieldFive=" + fieldFive + ", fieldSix=" + fieldSix + ")"
            }

            override fun toString(): String {
                return "NonPIIObject(fieldOne=" + fieldOne + ", fieldTwo=" + fieldTwo + ", fieldThree=" + fieldThree +
                        ", fieldFour=" + fieldFour + ", fieldFive=" + fieldFive + ", fieldSix=" + fieldSix + ")"
            }
        }

        val fieldOne = "lorum"
        val fieldTwo = "ipsum"
        val fieldThree = 1
        val fieldFour = 2
        val fieldFive = true
        val fieldSix = false

        val objectUnderTest = NonPIIObject(
            fieldOne,
            fieldTwo,
            fieldThree,
            fieldFour,
            fieldFive,
            fieldSix
        )

        val objectAsUnsanitisedString = objectUnderTest.toUnsanitizedString()
        val objectAsString = objectUnderTest.toString()

        val expectedUnsanitisedString =
            "NonPIIObject(fieldOne=" + fieldOne + ", fieldTwo=" + fieldTwo + ", fieldThree=" + fieldThree +
                    ", fieldFour=" + fieldFour + ", fieldFive=" + fieldFive + ", fieldSix=" + fieldSix + ")"
        val expectedString = "NonPIIObject(fieldOne=" + fieldOne + ", fieldTwo=" + fieldTwo + ", fieldThree=" + fieldThree +
                ", fieldFour=" + fieldFour + ", fieldFive=" + fieldFive + ", fieldSix=" + fieldSix + ")"

        Assert.assertEquals(expectedUnsanitisedString, objectAsUnsanitisedString)
        Assert.assertEquals(expectedString, objectAsString)
        Assert.assertEquals(expectedUnsanitisedString, expectedString)
    }
}
