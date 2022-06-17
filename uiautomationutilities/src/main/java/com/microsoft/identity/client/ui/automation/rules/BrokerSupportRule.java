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
import com.microsoft.identity.client.ui.automation.logging.Logger;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Arrays;
import java.util.List;

/**
 * A rule to determine if a test case should be skipped or run depending on whether the test
 * supports the supplied broker. This rule uses the {@link SupportedBrokers} annotation to see if
 * the test case should be ignored or run. The test case is executed if the current broker is
 * declared as supported for the test via the annotation and the test is ignored if the current
 * broker is not declared in supported brokers specified via the annotation. It is important to
 * note that if the {@link SupportedBrokers} annotation is not declared on the test, then the rule
 * will assume that all brokers are supported and will run the test against the current broker.
 */
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
                Logger.i(TAG, "Applying rule....");
                SupportedBrokers supportedBrokersAnnotation = description.getAnnotation(SupportedBrokers.class);

                if (supportedBrokersAnnotation == null) {
                    Logger.i(TAG, "Does not Received any supported broker annotation..");
                    // if the test didn't have the SupportedBrokers annotation, then we see if the
                    // class had that annotation and we try to honor that
                    supportedBrokersAnnotation = description.getTestClass().getAnnotation(SupportedBrokers.class);
                }

                if (supportedBrokersAnnotation != null) {
                    final List<Class<? extends ITestBroker>> supportedBrokerClasses =
                            Arrays.asList(supportedBrokersAnnotation.brokers());
                    Logger.i(TAG, "Received supported broker annotation with value: " + supportedBrokerClasses.toString());
                    Assume.assumeTrue(
                            "Ignoring test as not applicable with supplied broker",
                            (supportedBrokerClasses.contains(mBroker.getClass()) || supportedBrokerClasses.contains(mBroker.getClass().getSuperclass()))
                    );
                }

                base.evaluate();
            }
        };
    }
}
