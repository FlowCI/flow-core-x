/*
 * Copyright 2018 flow.ci
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

package com.flowci.domain.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.domain.StringVars;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class JsonableTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void should_parse_variable_map_to_json() throws IOException {
        StringVars vm = new StringVars();
        vm.put("hello", "world");
        String json = mapper.writeValueAsString(vm);
        Assert.assertNotNull(json);

        StringVars value = mapper.readValue(json, StringVars.class);
        Assert.assertNotNull(value);
        Assert.assertEquals("world", value.get("hello"));
    }

}
