package com.alexecollins.testsupport.rules;
import junit.framework.AssertionFailedError;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alex.e.c@gmail.com
 */
public class InvariantRule implements TestRule {

    private final Object target;

    /**
     * It should be noted that, while cheap and safe, an object might
     * change, but the code not change. That would be a tricky to diagnose.
     */
    private final Map<Field, Integer> fieldToHashCode = new HashMap<Field, Integer>();

    public InvariantRule(Object target) {
        this.target = target;
    }

    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                fieldToHashCode.clear();

                for (Field f : target.getClass().getFields()) {
                    if (f.isAnnotationPresent(Invariant.class)) {
                        fieldToHashCode.put(f, f.get(target).hashCode());
                    }
                }

                base.evaluate();

                for (Map.Entry<Field, Integer> e : fieldToHashCode.entrySet()) {
                    if (e.getKey().get(target).hashCode() != e.getValue()) {
                        throw new AssertionFailedError(e.getKey().getName() + " changed");
                    }
                }
            }
        };
    }
}
