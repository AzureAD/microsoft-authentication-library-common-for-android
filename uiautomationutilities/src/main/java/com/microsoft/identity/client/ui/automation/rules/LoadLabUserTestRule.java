package com.microsoft.identity.client.ui.automation.rules;

import androidx.annotation.NonNull;

import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to load lab user for the provided query prior to executing the test case
 */
public class LoadLabUserTestRule implements TestRule {

    public static final int TEMP_USER_WAIT_TIME = 15000;

    private LabUserQuery query;
    private String tempUserType;

    private String upn;

    public LoadLabUserTestRule(@NonNull final LabUserQuery query) {
        this.query = query;
    }

    public LoadLabUserTestRule(@NonNull final String tempUserType) {
        this.tempUserType = tempUserType;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (query != null) {
                    upn = LabUserHelper.loadUserForTest(query);
                } else if (tempUserType != null) {
                    upn = LabUserHelper.loadTempUser(tempUserType);
                    try {
                        // temp user takes some time to actually being created even though it may be
                        // returned by the LAB API. Adding a wait here before we proceed with the test.
                        Thread.sleep(TEMP_USER_WAIT_TIME);
                    } catch (InterruptedException e) {
                        Assert.fail(e.getMessage());
                    }
                } else {
                    throw new IllegalArgumentException("Both Lab User query and temp user type were null.");
                }

                base.evaluate();
            }
        };
    }

    public String getLabUserUpn() {
        return upn;
    }

}
