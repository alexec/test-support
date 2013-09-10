package com.alexecollins.testsupport.rules;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
public class BazTest {

    @Rule
    public TestRule invariantRule = RuleChain.outerRule(
            new TestRule() {
                public Statement apply(final Statement base, Description description) {
                    return new Statement() {
                        @Override
                        public void evaluate() throws Throwable {
                            qux = 2;
                            base.evaluate();
                        }
                    };
                }
            }).around(new InvariantRule(this));

    @Invariant
    public int qux;

    @Test
    public void testListUnchanged() throws Exception {
        // nop
    }

    @Test
    @Ignore("this will cause on exception")
    public void testListChangedImpliesError() throws Exception {
        qux = 3;
    }
}
