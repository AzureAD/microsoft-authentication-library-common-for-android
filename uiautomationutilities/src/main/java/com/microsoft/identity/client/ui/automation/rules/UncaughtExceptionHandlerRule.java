package com.microsoft.identity.client.ui.automation.rules;

import android.util.Log;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A Test Rule to set a default handler for uncaught exceptions from threads that may crash during automation.
 */
public class UncaughtExceptionHandlerRule implements TestRule {

    private final static Queue<Throwable> caughtThrowables = new ConcurrentLinkedQueue<>();

    private final static String TAG = UncaughtExceptionHandlerRule.class.getSimpleName();

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Create the exception handler
                final Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        Log.i(TAG, "Exception caught from " + t.getName());
                        caughtThrowables.add(e);
                    }
                };

                // Set the handler as the default for uncaught exceptions
                Thread.setDefaultUncaughtExceptionHandler(handler);

                base.evaluate();

                // After Test is executed, check to see if there are uncaught throwables
                if (!caughtThrowables.isEmpty()) {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Caught the following exception from non-main threads:\n");
                    for (Throwable e: caughtThrowables){
                        stringBuilder.append(e.getMessage());
                        stringBuilder.append("\n");
                    }

                    throw new AssertionError(stringBuilder.toString());
                }
            }
        };
    }
}
