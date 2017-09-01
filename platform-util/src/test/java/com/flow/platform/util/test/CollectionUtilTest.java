/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.util.test;

import com.flow.platform.util.CollectionUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yang
 */
public class CollectionUtilTest {

    public static class TestBean {

        private String name;

        private Integer number;

        public TestBean(String name, Integer number) {
            this.name = name;
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }
    }

    private Collection<TestBean> source;

    @Before
    public void before() {
        source = new ArrayList<>();
        source.add(new TestBean("a", 1));
        source.add(new TestBean("b", 2));
        source.add(new TestBean("c", 3));
    }

    @Test
    public void should_get_property_list_from_obj_collection() {
        // then: get name property collection
        List<String> names = CollectionUtil.toPropertyList("name", source);
        Assert.assertNotNull(names);
        Assert.assertEquals(3, names.size());
        Assert.assertEquals("a", names.get(0));
        Assert.assertEquals("b", names.get(1));
        Assert.assertEquals("c", names.get(2));

        // then: get name property collection
        List<Integer> numbers = CollectionUtil.toPropertyList("number", source);
        Assert.assertNotNull(numbers);
        Assert.assertEquals(3, numbers.size());
        Assert.assertEquals(1, numbers.get(0).intValue());
        Assert.assertEquals(2, numbers.get(1).intValue());
        Assert.assertEquals(3, numbers.get(2).intValue());
    }

    @Test
    public void should_get_property_map_from_obj_collection() {
        Map<String, TestBean> namesMap = CollectionUtil.toPropertyMap("name", source);
        Assert.assertNotNull(namesMap);
        Assert.assertEquals(3, namesMap.size());
        Assert.assertEquals("a", namesMap.get("a").getName());
        Assert.assertEquals("b", namesMap.get("b").getName());
        Assert.assertEquals("c", namesMap.get("c").getName());

        Map<Integer, TestBean> numMap = CollectionUtil.toPropertyMap("number", source);
        Assert.assertNotNull(numMap);
        Assert.assertEquals(3, numMap.size());
        Assert.assertEquals("a", numMap.get(1).getName());
        Assert.assertEquals("b", numMap.get(2).getName());
        Assert.assertEquals("c", numMap.get(3).getName());
    }

}
