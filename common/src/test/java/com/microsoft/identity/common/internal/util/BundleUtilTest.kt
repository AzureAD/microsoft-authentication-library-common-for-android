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
package com.microsoft.identity.common.internal.util

import android.os.Bundle
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BundleUtilTest {

    @Test
    fun deepCopyTest(){
        val internalBundle = Bundle()
        internalBundle.putString("InternalKey1", "InternalValue")
        internalBundle.putChar("InternalKey2", 'y')

        val internalParcelable = Bundle()
        internalParcelable.putString("InternalKey3", "InternalValue2")
        internalParcelable.putInt("InternalKey4", 124)
        internalParcelable.putFloat("InternalKey5", 31231.32F)

        val source = Bundle()
        source.putBundle("BundleKey", internalBundle)
        source.putString("StringKey", "stringVal")
        source.putInt("IntKey", 500)
        source.putFloat("FloatKey", 132.39127F)
        source.putByteArray("ByteArrayKey", byteArrayOf(0, 1, 2, 3, 4, 5))
        source.putIntegerArrayList("IntArrayKey", arrayListOf(0, 1, 2))
        source.putByte("ByteKey", 50)
        source.putChar("CharKey", 'x')
        source.putCharArray("CharArrayKey", charArrayOf('c','h','a','r'))
        source.putParcelable("ParcelableKey", internalParcelable)
        source.putParcelableArray("ParcelableArrayKey", arrayOf(internalParcelable, internalBundle))

        val duplicate = BundleUtil.deepCopy(source)

        Assert.assertFalse(source === duplicate) // not the same object!
        Assert.assertEquals(source.keySet(), duplicate.keySet())
        for (key in source.keySet()) {
            Assert.assertEquals(source.get(key), duplicate.get(key))
        }
    }
}
