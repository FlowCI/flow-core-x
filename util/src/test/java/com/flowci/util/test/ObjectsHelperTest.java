package com.flowci.util.test;

import com.flowci.util.ObjectsHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ObjectsHelperTest {

    @Getter
    @Setter
    public static class DummyParent {

        private String id;

        private String name;
    }

    @Getter
    @Setter
    public static class DummyObj extends DummyParent {

        private Boolean isCreated;

        private List<String> array = new ArrayList<>();
    }

    @Test
    public void should_check_fields_value() throws ReflectiveOperationException {
        DummyObj obj = new DummyObj();

        boolean r = ObjectsHelper.hasValues(obj, Sets.newHashSet("id", "name", "isCreated", "array"));
        Assert.assertFalse(r);

        obj.setId("123");
        obj.setName("name");
        obj.setIsCreated(true);
        obj.setArray(Lists.newArrayList("1"));

        r = ObjectsHelper.hasValues(obj, Sets.newHashSet("id", "name", "isCreated", "array"));
        Assert.assertTrue(r);
    }

    @Test
    public void should_get_random_between() {
        int i = ObjectsHelper.randomNumber(5, 10);
        Assert.assertTrue(i >= 5 && i <= 10);
    }
}
