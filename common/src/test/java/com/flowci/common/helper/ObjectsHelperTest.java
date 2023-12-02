package com.flowci.common.helper;

import com.flowci.common.helper.ObjectsHelper;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

        boolean r = ObjectsHelper.hasValues(obj, Set.of("id", "name", "isCreated", "array"));
        Assertions.assertFalse(r);

        obj.setId("123");
        obj.setName("name");
        obj.setIsCreated(true);
        obj.setArray(List.of("1"));

        r = ObjectsHelper.hasValues(obj, Set.of("id", "name", "isCreated", "array"));
        Assertions.assertTrue(r);
    }

    @Test
    public void should_get_random_between() {
        int i = ObjectsHelper.randomNumber(5, 10);
        Assertions.assertTrue(i >= 5 && i <= 10);
    }
}
