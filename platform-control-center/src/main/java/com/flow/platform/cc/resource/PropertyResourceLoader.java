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

package com.flow.platform.cc.resource;

import java.io.IOException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * @author gy@fir.im
 */
public class PropertyResourceLoader extends AppResourceLoader {

    private final static String ENV_FLOW_CONFIG_PATH = "FLOW_CONFIG_PATH";

    private final static String PROP_FLOW_CONFIG_PATH = "config.path";

    private final static String DEFAULT_CONFIG_PATH = "/etc/flow.ci/config/app.properties";

    private final static String CLASSPATH = "app-test.properties";

    private final static String DEFAULT_CLASSPATH = "app-default.properties";

    @Override
    public String getEnvName() {
        return ENV_FLOW_CONFIG_PATH;
    }

    @Override
    public String getPropertyName() {
        return PROP_FLOW_CONFIG_PATH;
    }

    @Override
    public String getDefaultDir() {
        return DEFAULT_CONFIG_PATH;
    }

    @Override
    public String getClassPath() {
        return CLASSPATH;
    }

    @Override
    public String getDefault() {
        return DEFAULT_CLASSPATH;
    }

    @Override
    public void register(ConfigurableApplicationContext context) {
        try {
            context.getEnvironment()
                .getPropertySources()
                .addFirst(new ResourcePropertySource(find()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
