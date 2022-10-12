package com.microsoft.identity.client.ui.automation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating that an automated End-to-End test case is not fit to be ran on the UI automation
 * pipeline. A pipeline has to include the argument to ignore tests with this annotation in the test targets option
 * for this to take effect.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DoNotRunOnPipeline {
    String value() default "";
}
