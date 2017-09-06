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

package com.flow.platform.yml.parser.test;

import static org.junit.Assert.fail;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.flow.platform.yml.parser.YmlParser;
import com.flow.platform.yml.parser.exception.YmlParseException;
import com.flow.platform.yml.parser.exception.YmlFormatException;
import com.flow.platform.yml.parser.test.domain.FlowTest;
import com.flow.platform.yml.parser.test.domain.FlowTestAdaptor;
import com.flow.platform.yml.parser.test.domain.FlowTestIgnore;
import com.flow.platform.yml.parser.test.domain.FlowTestInteger;
import com.flow.platform.yml.parser.test.domain.FlowTestPrimitativeBoolean;
import com.flow.platform.yml.parser.test.domain.FlowTestRequired;
import com.flow.platform.yml.parser.test.domain.FlowTestValidator;
import com.sun.tools.javac.comp.Flow;
import java.io.FileReader;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yh@firim
 */
public class YmlParserTest extends TestBase {

    @Test
    public void should_get_flows_success() {
        String demo = loadDemoFlowYaml("demo-yml.yaml");
        FlowTest[] flows = YmlParser.fromYml(demo, FlowTest[].class);
        Assert.assertEquals(1, flows.length);
    }

    @Test
    public void should_required() {
        String demo = loadDemoFlowYaml("demo-yml.yaml");
        try {
            FlowTestRequired[] flows = YmlParser.fromYml(demo, FlowTestRequired[].class);
        } catch (YmlParseException e) {
            Assert.assertEquals(YmlParseException.class, e.getClass());
        }
    }

    @Test
    public void should_ignore() {
        String demo = loadDemoFlowYaml("demo-yml.yaml");
        FlowTestIgnore[] flows = YmlParser.fromYml(demo, FlowTestIgnore[].class);
        Assert.assertEquals(1, flows.length);
        Assert.assertEquals(null, flows[0].getName());
    }

    @Test
    public void should_test_integer() {
        String demo = loadDemoFlowYaml("demo-yml-integer.yaml");
        FlowTestInteger[] flows = YmlParser.fromYml(demo, FlowTestInteger[].class);
        Assert.assertEquals(1, flows.length);
        Assert.assertEquals((Integer) 1, flows[0].getName());
        Assert.assertEquals("2", flows[0].getScript());
    }

    @Test
    public void should_adaptor_success() {
        String demo = loadDemoFlowYaml("demo-yml-adaptor.yaml");
        FlowTestAdaptor[] flows = YmlParser.fromYml(demo, FlowTestAdaptor[].class);
        Assert.assertEquals(1, flows.length);
        Assert.assertEquals("abc", flows[0].getName());
    }

    @Test(expected = YmlFormatException.class)
    public void should_validator_error() {
        String demo = loadDemoFlowYaml("demo-yml-validator.yaml");
        YmlParser.fromYml(demo, FlowTestValidator[].class);
    }

    @Test(expected = YmlParseException.class)
    public void should_parser_yml_error() {
        String demo = loadDemoFlowYaml("demo-yml-error.yaml");
        YmlParser.fromYml(demo, FlowTestValidator[].class);
    }

    @Test(expected = YmlParseException.class)
    public void should_parser_yml_error_format_error() {
        String demo = loadDemoFlowYaml("demo-yml-error-format.yaml");
        YmlParser.fromYml(demo, FlowTestValidator[].class);
    }

    @Test
    public void should_to_yml_success() {
        String demo = loadDemoFlowYaml("demo-yml.yaml");
        FlowTestIgnore[] flows = YmlParser.fromYml(demo, FlowTestIgnore[].class);
        Assert.assertEquals(1, flows.length);
        Assert.assertEquals(null, flows[0].getName());
        String yml = YmlParser.toYml(flows);
        Assert.assertNotNull(yml);
    }

    @Test
    public void should_to_yml_complex_success() {
        String demo = loadDemoFlowYaml("demo-yml-complex.yaml");
        FlowTest[] flows = YmlParser.fromYml(demo, FlowTest[].class);
        Assert.assertEquals(1, flows.length);
        String yml = YmlParser.toYml(flows);
        Assert.assertNotNull(yml);
        flows = YmlParser.fromYml(yml, FlowTest[].class);
        Assert.assertEquals(1, flows.length);
        Assert.assertNotNull(flows[0].getScript());
    }

    @Test
    public void should_primitative_boolean(){
        String demo = loadDemoFlowYaml("demo-yml-primitive-boolen.yaml");
        FlowTestPrimitativeBoolean[] flows = YmlParser.fromYml(demo, FlowTestPrimitativeBoolean[].class);
        Assert.assertEquals(1, flows.length);
        Assert.assertEquals(true, flows[0].getaBoolean());
        Assert.assertEquals("26012202222", flows[0].getaLong().toString());
        Assert.assertEquals("1.0", flows[0].getaFloat().toString());
        Assert.assertEquals((Double)1d, flows[0].getaDouble());
        Assert.assertEquals((Integer)1, flows[0].getInteger());
    }
}
