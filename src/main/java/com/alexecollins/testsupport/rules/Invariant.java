package com.alexecollins.testsupport.rules;

import java.lang.annotation.*;

/**
 * <p>Mark fields within a test that should not change during tests.</p>
 * <p>Fields must be cloneable.</p>
 *
 * @author: alexec (alex.e.c@gmail.com)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Invariant {
}