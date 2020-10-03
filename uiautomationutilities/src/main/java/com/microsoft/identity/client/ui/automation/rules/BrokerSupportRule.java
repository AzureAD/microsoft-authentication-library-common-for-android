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
package com.microsoft.identity.client.ui.automation.rules;

import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Arrays;
import java.util.List;

public class BrokerSupportRule implements TestRule {

    private final static String TAG = BrokerSupportRule.class.getSimpleName();

    private final ITestBroker mBroker;

    public BrokerSupportRule(@NonNull final ITestBroker broker) {
        this.mBroker = broker;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SupportedBrokers supportedBrokersAnnotation = description.getAnnotation(SupportedBrokers.class);

                if (supportedBrokersAnnotation == null) {
                    // if the test didn't have the SupportedBrokers annotation, then we see if the
                    // class had that annotation and we try to honor that
                    supportedBrokersAnnotation = description.getTestClass().getAnnotation(SupportedBrokers.class);
                }

                if (supportedBrokersAnnotation != null) {
                    final List<Class<? extends ITestBroker>> supportedBrokerClasses =
                            Arrays.asList(supportedBrokersAnnotation.brokers());
                    Log.i(TAG, "Received supported broker annotation with value: " + supportedBrokerClasses.toString());
                    Assume.assumeTrue(
                            "Ignoring test as not applicable with supplied broker",
                            supportedBrokerClasses.contains(mBroker.getClass())
                    );
                }

                base.evaluate();
            }
        };
    }
}
