package com.alexecollins.testsupport.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.MockitoAnnotations;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
public class MockRule implements TestRule {

    private final Object target;

    public MockRule(Object target) {
        this.target = target;
    }

    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                MockitoAnnotations.initMocks(target);
                base.evaluate();
            }
        };
    }
}