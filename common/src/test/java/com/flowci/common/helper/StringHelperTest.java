package com.flowci.common.helper;

import com.flowci.common.helper.StringHelper;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class StringHelperTest {

    @Test
    public void should_generate_unique_random_string() {
        int size = 1000;
        Set<String> set = new HashSet<>(size);

        for (int i = 0; i < size; i++) {
            String val = StringHelper.randomString(5);
            assertTrue(set.add(val));
        }
    }

    @Test
    public void should_escape_number_in_string() {
        assertEquals("abcdbcc", StringHelper.escapeNumber("a123bc2"));
        assertEquals("azcdbcc", StringHelper.escapeNumber("a_23bc2"));
    }
}