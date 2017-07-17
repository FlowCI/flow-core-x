package com.flow.platform.api.util;

import com.flow.platform.util.Logger;
import com.google.common.base.Strings;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by gyfirim on 14/07/2017.
 *
 * @Copyright fir.im
 */
public class AppPropertyUtil {

    private final static Logger LOGGER = new Logger(AppPropertyUtil.class);

    private final static String ENV_FLOW_CONFIG_PATH = "FLOW_CONFIG_PATH";

    private final static String PROP_FLOW_CONFIG_PATH = "config.path";

    private final static String DEFAULT_CONFIG_PATH = "/etc/flow.ci/config/app.properties";

    public final static Resource RESOURCE = init();

    public static void register(ConfigurableApplicationContext context) {
        try {
            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new ResourcePropertySource(RESOURCE));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load property resource
     *
     * @return Spring resource
     */
    private static Resource init() {
        // find property file path from system env
        String path = System.getenv(ENV_FLOW_CONFIG_PATH);
        if (!Strings.isNullOrEmpty(path) && Files.exists(Paths.get(path))) {
            LOGGER.trace("Load config file from system env: %s", path);
            return new FileSystemResource(path);
        }

        // find property file path from property
        path = System.getProperty(PROP_FLOW_CONFIG_PATH);
        if (!Strings.isNullOrEmpty(path) && Files.exists(Paths.get(path))) {
            LOGGER.trace("Load config file from system property: %s", path);
            return new FileSystemResource(path);
        }

        // find property file path from default directory
        if (Files.exists(Paths.get(DEFAULT_CONFIG_PATH))) {
            LOGGER.trace("Load config file from default path: %s", DEFAULT_CONFIG_PATH);
            return new FileSystemResource(DEFAULT_CONFIG_PATH);
        }

        // find default app-test.properties from class path
        ClassPathResource classPathResource = new ClassPathResource("app-test.properties");
        if (classPathResource.isReadable()) {
            LOGGER.trace("Load config file from default class path: %s", "app-test.properties");
            return classPathResource;
        }

        // apply default config
        LOGGER.trace("Load config file from default class path: %s", "app-default.properties");
        return new ClassPathResource("app-default.properties");
    }
}