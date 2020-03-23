package com.flowci.util.test;

import java.util.HashSet;
import java.util.Set;

import com.flowci.util.StringHelper;

import org.junit.Assert;
import org.junit.Test;

public class StringHelperTest {
    
    @Test
    public void should_generate_unique_random_string() {
        int size = 1000;
        Set<String> set = new HashSet<>(size);

        for (int i = 0; i < size; i++) {
            String val = StringHelper.randomString(5);
            Assert.assertTrue(set.add(val));
        }
    }
}