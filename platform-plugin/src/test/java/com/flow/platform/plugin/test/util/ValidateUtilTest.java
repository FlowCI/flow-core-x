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

package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.domain.envs.PluginProperty;
import com.flow.platform.plugin.domain.envs.PluginPropertyType;
import com.flow.platform.plugin.test.TestBase;
import com.flow.platform.plugin.util.ValidateUtil;
import com.flow.platform.plugin.util.ValidateUtil.Result;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class ValidateUtilTest extends TestBase {

    @Test
    public void should_validate_list_properties() throws Throwable {
        // given: define list property
        PluginProperty listProperty = new PluginProperty("CITY_LIST", PluginPropertyType.LIST);
        listProperty.setRequired(true);
        listProperty.setValues(ImmutableList.of("beijing", "shanghai", "seattle"));

        // when:
        Map<String, String> keyValues = new HashMap<>();
        keyValues.put("ENV_NOT_THERE", "beijing");
        Result result = ValidateUtil.validate(ImmutableList.of(listProperty), keyValues);

        // then:
        Assert.assertFalse(result.isValid());
        Assert.assertEquals("The property 'ENV_NOT_THERE' is not defined", result.getError());

        // when:
        keyValues = new HashMap<>();
        keyValues.put("CITY_LIST", "beijing");
        result = ValidateUtil.validate(ImmutableList.of(listProperty), keyValues);

        // then:
        Assert.assertTrue(result.isValid());
        Assert.assertNull(result.getError());
    }

    @Test
    public void should_validate_boolean_properties() throws Throwable {
        // given: define boolean property
        PluginProperty booleanProperty = new PluginProperty("FIR_IS_CONNECT", PluginPropertyType.BOOLEAN);
        booleanProperty.setDefaultValue("false");
        booleanProperty.setRequired(true);

        // when: empty value
        Map<String, String> keyValues = new HashMap<>();
        keyValues.put("FIR_IS_CONNECT", "");
        Result validResult = ValidateUtil.validate(ImmutableList.of(booleanProperty), keyValues);

        // then: should return false since property is required
        Assert.assertFalse(validResult.isValid());
        Assert.assertNotNull(validResult.getError());

        // when: validate illegal boolean value
        keyValues = new HashMap<>();
        keyValues.put("FIR_IS_CONNECT", "xxx");
        validResult = ValidateUtil.validate(ImmutableList.of(booleanProperty), keyValues);

        // then: should return false with error message
        Assert.assertFalse(validResult.isValid());
        Assert.assertNotNull(validResult.getError());

        // when: validate correct boolean value
        keyValues = new HashMap<>();
        keyValues.put("FIR_IS_CONNECT", "true");
        validResult = ValidateUtil.validate(ImmutableList.of(booleanProperty), keyValues);

        // then: should return true without error message
        Assert.assertTrue(validResult.isValid());
        Assert.assertNull(validResult.getError());
    }
}
