package com.alexecollins.testsupport.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Creates a context for tests that use Spring.
 * <p/>
 * Public fields in the test annotated {@link Autowired} are auto-wired from the context.
 *
 * @author alexec (alex.e.c@gmail.com)
 */
public class SpringContextRule implements TestRule {

    /** A list of class-path contexts. */
    private final String[] locations;

    /** The target test. */
    private final Object target;

    public SpringContextRule(String[] locations, Object target) {
        this.locations = locations;
        this.target = target;
    }

    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
                        locations);
	            context
	                    .getAutowireCapableBeanFactory().autowireBean(target);
                context.start();
                try {
                    base.evaluate();
                } finally {
                    context.close();
                }
            }
        };
    }
}