package com.alexecollins.testsupport.rules;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author alex.e.c@gmail.com
 */
@RunWith(MockitoJUnitRunner.class)
public class BarTest {

    @Mock
    private List<String> bar;

    @Test
    public void testBar() throws Exception {
        assertNotNull(bar);
    }
}
