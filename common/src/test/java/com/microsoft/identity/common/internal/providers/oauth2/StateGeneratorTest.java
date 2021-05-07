package com.microsoft.identity.common.internal.providers.oauth2;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StateGeneratorTest {

    private static final int TASK_ID = 19;
    private static final String STATE_EXAMPLE_1 = String.format("%s:%s", TASK_ID, "SOMEGUID-SOMEGUID");

    @Test
    public void test_stateGeneratorGenerateMethod(){
        StateGenerator generator = new AndroidTaskStateGenerator(TASK_ID);
        String state = generator.generate();
        String expected = String.valueOf(TASK_ID);
        Assert.assertEquals(expected, state.split(":")[0]);
    }

    @Test
    public void test_stateGeneratorParseMethod(){
        int taskId = AndroidTaskStateGenerator.getTaskFromState(STATE_EXAMPLE_1);
        Assert.assertEquals(TASK_ID, taskId);
    }
}
