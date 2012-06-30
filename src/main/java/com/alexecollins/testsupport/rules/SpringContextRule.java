package com.alexecollins.testsupport.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.lang.reflect.Field;

/**
 * Creates a context for tests that use Spring.
 * <p/>
 * Fields in the test annotated  are auto-wired
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
                AutowireCapableBeanFactory beanFactory = context
                        .getAutowireCapableBeanFactory();

                /* As this is an example of @Rule, this is a rough hand-rolled injector,
                * not suitable for production.
                * More capable ones, that support @Inject, @Qualifier etc. probably exist. */
                for (Field f : target.getClass().getFields()) {
                    if (f.isAnnotationPresent(Autowired.class)) {
                        f.set(target, context.getBean(f.getName(), f.getType()));
                    }
                }
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