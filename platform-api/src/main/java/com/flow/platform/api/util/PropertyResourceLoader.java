package com.flow.platform.api.util;

import java.io.IOException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Created by gy@fir.im on 14/07/2017.
 * Copyright fir.im
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
