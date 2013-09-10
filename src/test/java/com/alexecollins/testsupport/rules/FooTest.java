package com.alexecollins.testsupport.rules;

import com.alexecollins.testsupport.rules.MockRule;
import com.alexecollins.testsupport.rules.SpringContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author alex.e.c@gmail.com
 */
public class FooTest {

    @Rule
    public TestRule contextRule = new SpringContextRule(new String[]{"testContext.xml"}, this);

    @Rule
    public TestRule mockRule = new MockRule(this);

    @Autowired
    public String bar;

    @Mock
    public List baz;

    @Test
    public void testBar() throws Exception {
        assertEquals("bar", bar);
    }

    @Test
    public void testBaz() throws Exception {
        when(baz.size()).thenReturn(2);
        assertEquals(2, baz.size());
    }
}
