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

package com.flow.platform.api.test.util;

import com.flow.platform.api.config.AppConfig;
import com.flow.platform.api.domain.node.Flow;
import com.flow.platform.api.exception.YmlException;
import com.flow.platform.api.test.TestBase;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.yml.parser.YmlParser;
import com.google.common.io.Files;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * @author yh@firim
 */
public class YmlUtilTest extends TestBase{

    @Test
    public void should_test_success(){
        ClassLoader classLoader = NodeUtilYmlTest.class.getClassLoader();
        URL resource = classLoader.getResource("flow.yaml");
        File ymlSampleFile = new File(resource.getFile());
        Yaml yaml = new Yaml();
        Map result = null;

        Flow flow = null;
        try {
            String ymlString = Files.toString(ymlSampleFile, AppConfig.DEFAULT_CHARSET);
            result = (Map) yaml.load(ymlString);
            Object o = result.get("flow");
//            Object f = ((ArrayList)o).get(0);
            Flow[] flows = YmlParser.fromObject(o, Flow[].class);
        } catch (Throwable e) {
            throw new YmlException("Illegal yml definition");
        }

//        Flow flow1 = new Flow("/flow1", "flow1");
//        Flow flow2 = new Flow("/flow2", "flow2");
//        Flow[] flows = new Flow[] {flow1, flow2};
////        String s = Jsonable.GSON_CONFIG.toJson(flows);
////        String s = "[{\"path\":\"/flow1\",\"name\":\"flow1\",\"steps\":[],\"envs\":{}},{\"path\":\"/flow2\",\"name\":\"flow2\",\"steps\":[],\"envs\":{}}]";
//        String s = "{\"path\":\"/flow1\",\"name\":\"flow1\",\"steps\":[],\"envs\":{}}";
//        flow1 = Jsonable.GSON_CONFIG.fromJson(s, Flow.class);
    }
}