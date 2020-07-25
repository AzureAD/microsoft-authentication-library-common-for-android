package com.microsoft.identity.client.ui.automation.annotations;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation indicating how many times a test should be retried. This annotation must be placed
 * on a test/test class if those tests are meant to be retried. If such an annotation is not present,
 * then the test would not be retried. The default value of this annotation is 1, however, a different
 * value can be specified via retryCount parameter to retry the test that many times.
 * <p>
 * For retry logic, see {@link com.microsoft.identity.client.ui.automation.rules.RetryTestRule}
 */
@Retention(value = RUNTIME)
public @interface RetryOnFailure {

    int retryCount() default 1;
}
