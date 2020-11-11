package com.microsoft.identity.client.ui.automation;

import org.junit.rules.RuleChain;

public interface IRuleBasedTest {

    RuleChain getPrimaryRules();

}
